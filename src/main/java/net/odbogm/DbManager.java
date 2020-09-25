package net.odbogm;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.OrientDBConfigBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static net.odbogm.Primitives.PRIMITIVE_MAP;
import net.odbogm.annotations.ClassIndex;
import net.odbogm.annotations.Embedded;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.FieldAttributes;
import net.odbogm.annotations.FieldAttributes.Bool;
import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.IgnoreClass;
import net.odbogm.annotations.Indexed;
import net.odbogm.annotations.RID;
import net.odbogm.annotations.Version;
import net.odbogm.cache.ClassCache;
import net.odbogm.security.GroupSID;
import net.odbogm.security.SObject;
import net.odbogm.security.UserSID;

/**
 * DbManager se encarga de analizar todas las clases que se encuentren en los
 * paquetes que se le indiquen o las clases específicas que se le indiquen
 * y crea la correspondiente estructura en la base de datos. Para ello se basa 
 * en las anotaciones:
 *
 * {@code @Indexed}: crea un índice sobre el campo.
 * 
 * {@code @FieldAttributes}: define varios atributos de los campos de acuerdo a
 * la cláusula {@code ALTER PROPERTY}.
 * 
 * {@code @Ignore}: ignora el campo en la definición de la clase. Sin embargo se
 * persistirá si el campo es distinto de null cuando se realice un store del objeto.
 * 
 * {@code @IgnoreClass}: ignora la clase completa.
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class DbManager {

    private final static Logger LOGGER = Logger.getLogger(DbManager.class.getName());
    static {
        LOGGER.setLevel(LogginProperties.DbManager);
    }

    private OrientDB orientDB;
    private ODatabasePool dbPool;
    private ODatabaseSession graphdb;

    private static final String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";
    private static final char PKG_SEPARATOR = '.';
    private static final char DIR_SEPARATOR = '/';
    private static final String CLASS_FILE_SUFFIX = ".class";

    /** clases ya registradas, la clave es el nombre de la clase Java, no de la entidad */
    private final ConcurrentHashMap<String, ClassStruct> registeredClasses = new ConcurrentHashMap<>();
    private final ArrayList<ClassStruct> orderedRegisteredClass = new ArrayList<>();

    /** determina si se genera la cadena de drops como comentarios o no */
    private boolean withDrops = false;
    
    /** determina si los comandos tienen en cuenta la existencia previa de los elementos */
    private boolean incremental = true;
    
    
    public DbManager() {
    }
    
    /**
     * 
     * @param withDrops Dejar drops.
     * @param incremental Verificar existencia previa.
     */
    public DbManager(boolean withDrops, boolean incremental) {
        this.withDrops = withDrops;
        this.incremental = incremental;
    }
    
    /**
     * 
     * @param url URL.
     * @param user Usuario.
     * @param passwd Contraseña.
     * @deprecated No se usa para nada la conexión a la base.
     */
    @Deprecated
    public DbManager(String url, String user, String passwd) {
        this.init(url, user, passwd);
    }

    /**
     * 
     * @param url URL.
     * @param user Usuario.
     * @param passwd Contraseña.
     * @param withDrops Dejar drops.
     * @deprecated No se usa para nada la conexión a la base.
     */
    @Deprecated
    public DbManager(String url, String user, String passwd, boolean withDrops) {
        this.init(url, user, passwd);
        this.withDrops = withDrops;
    }


    private void init(String url, String user, String passwd){
        LOGGER.log(Level.INFO, "ODBOGM Session Manager initialization...");
        this.orientDB = new OrientDB(url,OrientDBConfig.defaultConfig());
        
        OrientDBConfigBuilder poolCfg = OrientDBConfig.builder();
        poolCfg.addConfig(OGlobalConfiguration.DB_POOL_MIN, 5);
        poolCfg.addConfig(OGlobalConfiguration.DB_POOL_MAX, 10);
        
        this.dbPool = new ODatabasePool(orientDB,url, user, passwd, poolCfg.build());
    }
    
    public void begin() {
        graphdb = this.dbPool.acquire();
    }

    public DbManager setClassLevelLog(Class<?> clazz, Level level) {
        Logger L = Logger.getLogger(clazz.getName());
        L.setLevel(level);
        return this;
    }
    
    public void generateToConsole(String... analize) {
        this.process(analize);
        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
            System.out.println(orderedRegisteredClas.drop);
        }
        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
            System.out.println(orderedRegisteredClas.create);
            for (String property : orderedRegisteredClas.properties) {
                System.out.println(property);
            }
            for (String index : orderedRegisteredClas.classIndexes) {
                System.out.println(index);
            }
            System.out.println("");
        }
    }
    
    /**
     * Devuelve un arraylist con todas las instrucciones necesarias para la creación de la base de datos.
     * @param analize lista de clases a analizar
     * @return arraylist con las instrucciones.
     */
    public ArrayList<String> generateDBSQL(String... analize){
        ArrayList<String> statements = new ArrayList<>();
        this.process(analize);
        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
            statements.add(orderedRegisteredClas.drop);
        }
        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
            statements.add(orderedRegisteredClas.create);
            for (String property : orderedRegisteredClas.properties) {
                statements.add(property);
            }
            for (String index : orderedRegisteredClas.classIndexes) {
                statements.add(index);
            }
        }
        return statements;
    }

    /**
     * Genera un archivo con las intrucciones SQL necesarias para la creación de la base
     * @param fileName: path y nombre del archivo a crear. 
     * @param analize: paquetes o clases a incluir.
     */
    public void generateDBSQL(String fileName, String[] analize){
        this.process(analize);
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            LOGGER.log(Level.FINER, "abriendo el archivo...");
            fw = new FileWriter(fileName);
            LOGGER.log(Level.FINER, "preparando el printwriter...");
            pw = new PrintWriter(fw);
            LOGGER.log(Level.FINER, "procesando {0} lineas...", orderedRegisteredClass.size());
            for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
                pw.println(orderedRegisteredClas.drop);
            }
            for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
                pw.println(orderedRegisteredClas.create);
                for (String property : orderedRegisteredClas.properties) {
                    pw.println(property);
                }
                for (String classIndexes : orderedRegisteredClas.classIndexes) {
                    pw.println(classIndexes);
                }
                pw.println("");
            }
            LOGGER.log(Level.FINER, "finalizado!");
        } catch (IOException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * recorrer el vector analizando todas las clases que se encuentran referenciadas y crea
     * las sentencias correspondientes para su creación            
     */
    private void process(String[] analize) {
        List<Class<?>> classes = new ArrayList<>();
        for (String clazz : analize) {
            classes.addAll(find(clazz));
        }
        classes.stream().filter(clazz -> clazz.isAnnotationPresent(Entity.class)).
                forEach(clazz -> buildDBScript(clazz));
    }
    
    /**
     * Verifica que el árbol de herencias de la clase esté registrado.
     * Si no es así, lo registra desde la clase superior hacia abajo.
     *
     * @param clazz clase a analizar
     */
    private ClassStruct buildDBScript(Class clazz) {
        if ((clazz == null)
                ||(clazz.isAnonymousClass())
                ||(clazz.isEnum())
                ||(clazz.isAnnotationPresent(IgnoreClass.class))
                ||(clazz.isInterface())
                )
            return null;
        
        LOGGER.log(Level.FINER, "procesando: {0}...", clazz.getSimpleName());
        
        // verificar si ya se ha agregado
        ClassStruct pre = this.registeredClasses.get(clazz.getSimpleName());
        if (pre != null) return pre;
        
        //si se está usando SObject se debe forzar a definir las implementaciones,
        //puede ser que nunca haya una referencia directa y nunca serían detectadas
        if (clazz.equals(SObject.class)) {
            buildDBScript(UserSID.class);
            buildDBScript(GroupSID.class);
        }
        
        String superName = "";
        ClassStruct superStruct = null;
        // primero procesar la superclass
        if (clazz.getSuperclass() != Object.class) {
            superStruct = buildDBScript(clazz.getSuperclass());
            superName = superStruct.className;
        }
        
        // procesar todos los campos de la clase actual.
        String className = ClassCache.getEntityName(clazz);

        // la clase no existe aún. Registrarla
        ClassStruct clazzStruct = new ClassStruct(className);
        this.registeredClasses.put(clazz.getSimpleName(), clazzStruct);
        
        // es vértice o arista
        boolean esArista = isEdgeClass(clazz);
        
        String exist = "let exist = select from (select expand(classes) from "
                + "metadata:schema) where name = '"+className+"';\n"
                + "if ($exist.size() %s 0) {\n"
                + "    %s"
                + "}\n";
        
        // orden de drop
        String drop = String.format("delete %s %s;\n    drop class %s;\n",
                esArista ? "edge" : "vertex", className, className);
        
        clazzStruct.drop = (!this.withDrops?"/*\n":"")
                + String.format(exist, ">", drop) + (!this.withDrops?"*/":"");
        
        // orden de create
        String extendsFrom = superName.isEmpty() ? (esArista ? "E" : "V") : superName;
        String create = String.format("create class %s extends %s;\n", className, extendsFrom);
        
        clazzStruct.create = (this.incremental ? String.format(exist, "=", create) : create)
                + "alter class " + className + " custom javaClass='" 
                + clazz.getCanonicalName() + "';\n";

        // se utilizará para registrar todas las clases de atributos que no sean primitivos:
        ArrayList<Class<?>> postProcess = new ArrayList<>();
        
        // procesar todos los campos de la clase:
        Field[] fields = clazz.getDeclaredFields();
        String fieldName;
        for (Field field : fields) {
            fieldName = field.getName();
            field.setAccessible(true);
            
            if (!(field.isAnnotationPresent(Ignore.class)
                            || Modifier.isTransient(field.getModifiers())
                            || (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                            || fieldName.startsWith("___ogm___"))
                            || field.isAnnotationPresent(RID.class)
                            || field.isAnnotationPresent(Version.class)
                            )) {
                FieldAttributes fa = field.getAnnotation(FieldAttributes.class);
                
                String currentProp = className + "." + fieldName;
                
                boolean embeddedList = false;
                if (List.class.isAssignableFrom(field.getType())) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                    embeddedList = listClass.isEnum();
                }
                
                if ((PRIMITIVE_MAP.get(field.getType())!=null)
                        ||(field.getType().isEnum())
                        ||(field.isAnnotationPresent(Embedded.class))
                        ||(embeddedList)) {
                    // crear el statement para el campo si corresponde
                    String type;
                    if (field.getType().isEnum()) {
                        type = "string";
                    } else if (field.isAnnotationPresent(Embedded.class)) {
                        type = "embedded";
                    } else if (List.class.isAssignableFrom(field.getType())) {
                        type = "embeddedlist";
                    } else if (Map.class.isAssignableFrom(field.getType())) {
                        type = "embeddedmap";
                    } else {
                        type = PRIMITIVE_MAP.get(field.getType()).toString();
                    }
                    String statement = "create property " + currentProp + " " + type + ";";
                    String let = "\n"
                                    +"let exist = select from "
                                                        + "(select expand(properties) "
                                                        + " from (select expand(classes) "
                                                        + " from metadata:schema) "
                                                        + " where name = '"+className+"') where name = '"+fieldName+"';\n"
                                    + "if ($exist.size()=0) {\n"
                                    + "    %s"
                                    + "\n}\n ";
                    
                    statement = this.incremental ? String.format(let, statement) : statement;
                    clazzStruct.properties.add(statement);

                    if (fa != null) {
                        if (!fa.min().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" min "+fa.min()+";");
                        }

                        if (!fa.max().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" max "+fa.max()+";");
                        }

    //                    Bool mandatory() default Bool.UNDEF;
                        if (fa.mandatory() != Bool.UNDEF) {
                            clazzStruct.properties.add("alter property "+currentProp+" mandatory "+fa.mandatory()+";");
                        }

    ////                    String name() default "";
    //                if (fa.name().isEmpty())
    //                    dbClazz.getProperty(field.getName()).setN(null);
    //                else
    //                    dbClazz.getProperty(field.getName()).setMin(fa.sMin());
    //                    Bool notNull() default Bool.UNDEF;
                        if (fa.notNull() != Bool.UNDEF) {
                            clazzStruct.properties.add("alter property "+currentProp+" NotNull "+fa.notNull()+";");
                        }

    //                    String regexp() default "";
                        if (!fa.regexp().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" regexp "+fa.regexp()+";");
                        }

    ////                    String type() default "";
    //                if (!fa.type().isEmpty())
    //                    dbClazz.getProperty(field.getName()).setType( );
    //                    String collate() default "";
                        if (!fa.collate().isEmpty()) {
                           clazzStruct.properties.add("alter property "+currentProp+" collate "+fa.collate()+";");
                        }

    //                    Bool readOnly() default Bool.UNDEF;
                        if (fa.readOnly() != Bool.UNDEF) {
                            clazzStruct.properties.add("alter property "+currentProp+" readonly "+fa.readOnly()+";");
                        }

    //                    String defaultVal() default "";
                        if (!fa.defaultVal().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" default "+fa.defaultVal()+";");
                        }

    //                    String linkedClass() default "";
    //                    String linkedType() default "";
                    }
                } else {
                    // si no es un tipo básico, se debe conectar con un Edge.
                    extendsFrom = "E";
                    
                    //si se usan atributos de aristas (con un Map), mantener la herencia
                    //con la clase anotada como EdgeClass
                    if (Map.class.isAssignableFrom(field.getType())) {
                        ParameterizedType paratype = (ParameterizedType)field.getGenericType();
                        Class keyClass = (Class)paratype.getActualTypeArguments()[0];
                        if (isEdgeClass(keyClass)) {
                            buildDBScript(keyClass);
                            extendsFrom = ClassCache.getEntityName(keyClass);
                            clazzStruct.edgeCollections.put(fieldName, extendsFrom);
                        }
                    }
                    
                    if (extendsFrom.equals("E")) clazzStruct.relations.add(fieldName);
                    
                    processEdge(fieldName, extendsFrom, clazzStruct);
                    
                    // y se debe analizar si no se trata de una clase marcada como Entidad
                    // que no se encuentra entre los paquetes indicados como parámetros
                    // Ej. UserSID / GroupSID
                    LOGGER.log(Level.FINER, "field type: {0}", field.getType());
                    if (field.getType().isAnnotationPresent(Entity.class) 
                            && registeredClasses.get(field.getType().getSimpleName())==null) {
                        postProcess.add(field.getType());
                    }
                }
                //indexed?
                if (field.isAnnotationPresent(Indexed.class)) {
                    Indexed idx = field.getAnnotation(Indexed.class);
                    
                    String engine = idx.type()==Indexed.IndexType.LUCENE?" FULLTEXT ENGINE LUCENE ":""+idx.type();
                    engine += (!idx.metadata().isEmpty()?"METADATA "+idx.metadata():"");
                    
                    String createLink = "create index "+currentProp+" on "+className+"("+fieldName+") "+engine+";";
                    String statement = this.incremental ? String.format("\n"
                        +"let exist = select from(select expand(indexes) from metadata:indexmanager) where name = '"+currentProp+"';\n"
                        + "if ($exist.size()=0) {\n"
                        + "    %s"
                        + "\n}\n ", createLink) : createLink;
                    clazzStruct.properties.add(statement);
                }
            }
        }
        
        //procesar las relaciones heredadas:
        final String finalSuperName = superName;
        if (superStruct != null) {
            superStruct.relations.forEach(field -> {
                processEdge(field, finalSuperName + "_" + field, clazzStruct);
                clazzStruct.relations.add(field);
            });
            superStruct.edgeCollections.forEach((field, edgeClass) -> {
                processEdge(field, edgeClass, clazzStruct);
                clazzStruct.edgeCollections.put(field, edgeClass);
            });
        }
        
        // procesar las anotaciones de clase buscando índices compuestos.
        for (Annotation annotation : clazz.getAnnotationsByType(ClassIndex.class)) {
            clazzStruct.classIndexes.add(((ClassIndex)annotation).indexExpr());
        }
        
        this.orderedRegisteredClass.add(clazzStruct);
        
        // procesar los tipos decubiertos durante la exploración de los campos.
        postProcess.forEach(discoveredType -> buildDBScript(discoveredType));
        
        return clazzStruct;
    }

    private boolean isEdgeClass(Class<?> c) {
        Entity anno = c.getAnnotation(Entity.class);
        return (anno != null && anno.isEdgeClass());
    }
    
    private void processEdge(String field, String edgeClass, ClassStruct classStruct) {
        String linkName = classStruct.className + "_" + field;
        String createLink = String.format("create class %s extends %s;",
                linkName, edgeClass);
        String statement = this.incremental ? String.format("\n"
            + "let exist = select from (select expand(classes) from metadata:schema) "
            + "where name = '" + linkName + "';\n"
            + "if ($exist.size()=0) {\n"
            + "    %s"
            + "\n}\n ", createLink) : createLink;
        classStruct.properties.add(statement);
    }
    
    private List<Class<?>> find(String scannedPackage) {
        String scannedPath = scannedPackage.replace(PKG_SEPARATOR, DIR_SEPARATOR);
        URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
        LOGGER.log(Level.INFO, "URL: {0}", scannedUrl);
        if (scannedUrl == null) {
            throw new IllegalArgumentException(String.format(
                    BAD_PACKAGE_ERROR, scannedPath, scannedPackage));
        }
        File scannedDir = new File(scannedUrl.getFile());
        try {
            LOGGER.log(Level.INFO, "scannedDir: {0}", scannedDir.getCanonicalPath());
        } catch (IOException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File file : scannedDir.listFiles()) {
            classes.addAll(find(file, scannedPackage));
        }
        return classes;
    }

    private List<Class<?>> find(File file, String scannedPackage) {
        List<Class<?>> classes = new ArrayList<>();
        String resource = scannedPackage + PKG_SEPARATOR + file.getName();
        LOGGER.log(Level.INFO, "resource: {0}", resource);
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                classes.addAll(find(child, resource));
            }
        } else if (resource.endsWith(CLASS_FILE_SUFFIX)) {
            int endIndex = resource.length() - CLASS_FILE_SUFFIX.length();
            String className = resource.substring(0, endIndex);
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException ignore) {
            }
        }
        return classes;
    }

    /**
     * Clase de ayuda para crear los comandos que generan la base de datos.
     */
    class ClassStruct {

        public String className;
        public String drop;
        public String create;
        public ArrayList<String> properties = new ArrayList<>();
        public ArrayList<String> classIndexes = new ArrayList<>();
        public Map<String, String> edgeCollections = new HashMap<>();
        public ArrayList<String> relations = new ArrayList<>();

        public ClassStruct(String className) {
            this.className = className;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ClassStruct other = (ClassStruct) obj;
            return Objects.equals(this.className, other.className);
        }
    }
    
}
