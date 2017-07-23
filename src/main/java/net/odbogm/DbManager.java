/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static net.odbogm.Primitives.PRIMITIVE_MAP;
import net.odbogm.annotations.FieldAttributes;
import net.odbogm.annotations.FieldAttributes.Bool;
import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.IgnoreClass;
import net.odbogm.annotations.Indexed;

/**
 * DbManager se encarga de analizar todas las clases que se encuentren en los paquetes que se le indiquen o las clases específicas que se le indiquen
 * y crea la correspondiete estructura en la base de datos. Para ello se basa en las anotaciones:
 *
 * {@code @Index}: crea un índice sobre el campo.
 * {@code @FieldAttributes}: define varios attributos de los campos de acuerdo a la cláusula {@code ALTER PROPERTY LINKEDCLASS}, the linked class name. Accepts a
 * string as value. NULL to remove it LINKEDTYPE, the linked type name between those supported:Types. Accepts a string as value. NULL to remove it
 * MIN, the minimum value as constraint. Accepts strings, numbers or dates as value. NULL to remove it MANDATORY, true if the property is mandatory.
 * Accepts "true" or "false" MAX, the maximum value as constraint. Accepts strings, numbers or dates as value. NULL to remove it NAME, the property
 * name. Accepts a string as value NOTNULL, the property can't be null. Accepts "true" or "false" REGEXP the regular expression as constraint. Accepts
 * a string as value. NULL to remove it TYPE, the type between those supported:Types Accepts a string as value COLLATE, sets the collate to define the
 * strategy of comparison. By default is case sensitive. By setting it yo "ci", any comparison will be case-insensitive READONLY the property value is
 * immutable: it can't be changed after the first assignment. Use this with DEFAULT to have immutable values on creation. Accepts "true" or "false"
 * CUSTOM Set custom properties. Syntax is {@code <name> = <value>}. Example: {@code stereotype = icon DEFAULT} (Since 2.1) set the default value. Default value can
 * be a value or a function. See below for examples
 * {@code @IgnoreProperty}: ignora el campo en la definición de la clase. Sin embargo se persistirá si el campo es distinto de null cuando se realice un store
 * del objeto.
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class DbManager {

    private final static Logger LOGGER = Logger.getLogger(DbManager.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.DbManager);
    }

    private OrientGraph graphdb;
    private OrientGraphFactory factory;

    private static final String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";
    private static final char PKG_SEPARATOR = '.';
    private static final char DIR_SEPARATOR = '/';
    private static final String CLASS_FILE_SUFFIX = ".class";

    private ArrayList<ClassStruct> orderedRegisteredClass = new ArrayList<>();
    private ConcurrentHashMap<String, ClassStruct> registeredClass = new ConcurrentHashMap<>();

    /** determina si se genera la cadena de drops como comentarios o no */
    private boolean withDrops = false;
    
    public DbManager(String url, String user, String passwd) {
        this.init(url, user, passwd);
    }

    public DbManager(String url, String user, String passwd, boolean withDrops) {
        this.init(url, user, passwd);
        this.withDrops = withDrops;
    }

    private void init(String url, String user, String passwd){
        this.factory = new OrientGraphFactory(url, user, passwd).setupPool(1, 10);
    }
    public void begin() {
        graphdb = factory.getTx();
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
        // levantar cada clase y verificar su existencia en la base.
        for (Class<?> clazz : classes) {
            // verificar que la clase exista en la base.
            String className = clazz.getSimpleName();
            if (!clazz.isAnnotationPresent(IgnoreClass.class))
                buildDBScript(clazz);
        }
        
//        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
//            System.out.println(orderedRegisteredClas.drop);
//            System.out.println(orderedRegisteredClas.create);
//            for (String property : orderedRegisteredClas.properties) {
//                System.out.println(property);
//            }
//            System.out.println("");
//        }
    }

    public void generateToConsole(String[] analize){
        this.process(analize);
        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
            System.out.println(orderedRegisteredClas.drop);
            System.out.println(orderedRegisteredClas.create);
            for (String property : orderedRegisteredClas.properties) {
                System.out.println(property);
            }
            System.out.println("");
        }
    }
    
    /**
     * Devuelve un arraylist con todas las instrucciones necesarias para la creación de la base de datos.
     * @param analize
     * @return 
     */
    public ArrayList<String> generateDBSQL(String[] analize){
        ArrayList<String> statements = new ArrayList<>();
        this.process(analize);
        for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
            
            statements.add(orderedRegisteredClas.drop);
            statements.add(orderedRegisteredClas.create);
            for (String property : orderedRegisteredClas.properties) {
                statements.add(property);
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
            LOGGER.log(Level.FINER, "procesando "+orderedRegisteredClass.size()+" lineas...");
            for (ClassStruct orderedRegisteredClas : orderedRegisteredClass) {
                pw.println(orderedRegisteredClas.drop);
                pw.println(orderedRegisteredClas.create);
                for (String property : orderedRegisteredClas.properties) {
                    pw.println(property);
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
     * Verifica que el árbol de herencias de la clase esté registrado. Si no es así, lo registra desde la clase superior hacia abajo.
     *
     * @param clazz
     */
    private void buildDBScript(Class clazz) {
        if ((clazz == null)||(clazz.isAnonymousClass())||(clazz.isEnum())||(clazz.isAnnotationPresent(IgnoreClass.class)))
            return;
        
        LOGGER.log(Level.FINER, "procesando: "+clazz.getSimpleName()+"...");
        String superName = "";
        // primero procesar la superclass
        if (clazz.getSuperclass() != Object.class) {
            buildDBScript(clazz.getSuperclass());
            superName = (clazz.getSuperclass()==null?"":clazz.getSuperclass().getSimpleName());
        } 
            

        // procesar todos los campos de la clase actual.
        String className = clazz.getSimpleName();

        // verificar si ya se ha agregado
        if (this.registeredClass.get(className) != null) {
            return;
        }
        // la clase no existe aún. Registrarla
        ClassStruct clazzStruct = new ClassStruct(className);
        this.registeredClass.put(className, clazzStruct);
        this.orderedRegisteredClass.add(clazzStruct);
        // preparar orden de dropeo. Solo se ejecuta si la clase existe
        clazzStruct.drop = (!this.withDrops?"/*\n":"\n")
                +"let exist = select from (select expand(classes) from metadata:schema) where name = '"+className+"'\n"
                + "if ($exist.size()>0) {\n"
                + "     delete vertex "+className+"\n"
                + "     drop class "+className+"\n"
                + "}"
                + (!this.withDrops?"\n */":"\n");
        
        // order de create. Solo se ejecuta si no existe la clase
        clazzStruct.create = "\n"
                +"let exist = select from (select expand(classes) from metadata:schema) where name = '"+className+"'\n"
                + "if ($exist.size()=0) {\n"
                + "     create class "+className+(superName.isEmpty()?" extends V":" extends "+superName)
                + "\n}\n "
                + "alter class "+className+" custom javaClass='"+clazz.getCanonicalName()+"'\n"
                + (superName.isEmpty()?" ":"alter class "+className+" superclass "+superName+"\n");

        // procesar todos los campos del la clase.
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            
            if (!(  field.isAnnotationPresent(Ignore.class) 
                            || Modifier.isTransient(field.getModifiers())
                            || (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
                    )) {
                FieldAttributes fa = field.getAnnotation(FieldAttributes.class);
                
                String currentProp = className + "." + field.getName();
                if ((PRIMITIVE_MAP.get(field.getType())!=null)||(field.getType().isEnum())) {
                    // crear el statement para el campo si corresponde
                    String statement = "\n"
                                    +"let exist = select from "
                                                        + "(select expand(properties) "
                                                        + " from (select expand(classes) "
                                                        + " from metadata:schema) "
                                                        + " where name = '"+className+"') where name = '"+field.getName()+"'\n"
                                    + "if ($exist.size()=0) {\n"
                                    + "     create property " + currentProp + " " + (field.getType().isEnum()?" string ": PRIMITIVE_MAP.get(field.getType()))
                                    + "\n}\n ";
                    clazzStruct.properties.add(statement);

                    if (fa != null) {
                        if (!fa.min().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" min "+fa.min());
                        }

                        if (!fa.max().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" max "+fa.max());
                        }

    //                    Bool mandatory() default Bool.UNDEF;
                        if (fa.mandatory() != Bool.UNDEF) {
                            clazzStruct.properties.add("alter property "+currentProp+" mandatory "+fa.mandatory());
                        }

    ////                    String name() default "";
    //                if (fa.name().isEmpty())
    //                    dbClazz.getProperty(field.getName()).setN(null);
    //                else
    //                    dbClazz.getProperty(field.getName()).setMin(fa.sMin());
    //                    Bool notNull() default Bool.UNDEF;
                        if (fa.notNull() != Bool.UNDEF) {
                            clazzStruct.properties.add("alter property "+currentProp+" NotNull "+fa.notNull());
                        }

    //                    String regexp() default "";
                        if (!fa.regexp().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" regexp "+fa.regexp());
                        }

    ////                    String type() default "";
    //                if (!fa.type().isEmpty())
    //                    dbClazz.getProperty(field.getName()).setType( );
    //                    String collate() default "";
                        if (!fa.collate().isEmpty()) {
                           clazzStruct.properties.add("alter property "+currentProp+" collate "+fa.collate());
                        }

    //                    Bool readOnly() default Bool.UNDEF;
                        if (fa.readOnly() != Bool.UNDEF) {
                            clazzStruct.properties.add("alter property "+currentProp+" readonly "+fa.readOnly());
                        }

    //                    String defaultVal() default "";
                        if (!fa.defaultVal().isEmpty()) {
                            clazzStruct.properties.add("alter property "+currentProp+" default "+fa.defaultVal());
                        }

    //                    String linkedClass() default "";
    //                    String linkedType() default "";
                    }
                } else {
                    // si no es un tipo básico, se debe conectar con un Edge.
//                    if (Primitives.LAZY_COLLECTION.get(field.getType())!=null) {
                    // FIXME: ojo que si se tratara de una extensín de AL o HM no lo vería como tal y lo vincularía con un link
                    clazzStruct.properties.add("\n"
                        +"let exist = select from (select expand(classes) from metadata:schema) where name = '"+className+"_"+field.getName()+"'\n"
                        + "if ($exist.size()=0) {\n"
                        + "     create class "+className+"_"+field.getName()+" extends E"
                        + "\n}\n "
                        );
                }
                if (field.isAnnotationPresent(Indexed.class)) {
                    Indexed idx = field.getAnnotation(Indexed.class);
                    clazzStruct.properties.add("\n"
                        +"let exist = select from(select expand(indexes) from metadata:indexmanager) where name = '"+currentProp+"'\n"
                        + "if ($exist.size()=0) {\n"
                        + "     create index "+currentProp+" "+idx.type()
                        + "\n}\n "
                        );
                }
            }
        }
    }

    private List<Class<?>> find(String scannedPackage) {
        String scannedPath = scannedPackage.replace(PKG_SEPARATOR, DIR_SEPARATOR);
        URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
        LOGGER.log(Level.INFO, "URL: "+scannedUrl);
        if (scannedUrl == null) {
            throw new IllegalArgumentException(String.format(BAD_PACKAGE_ERROR, scannedPath, scannedPackage));
        }
        File scannedDir = new File(scannedUrl.getFile());
        try {
            LOGGER.log(Level.INFO, "scannedDir: "+scannedDir.getCanonicalPath());
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
        LOGGER.log(Level.INFO, "resource: "+resource);
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
     * Clase de ayuda para crear los comandos que generan la base de datos. Genera los delete vertex y drops como comentarios
     * para que sean desmarcados si se desea.
     */
    class ClassStruct {

        public String className;
//        public String deleteVertex;
        public String drop;
        public String create;
        public ArrayList<String> properties = new ArrayList<>();
//        public ArrayList<String> propertiesModifiers = new ArrayList<>();

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
            if (!Objects.equals(this.className, other.className)) {
                return false;
            }
            return true;
        }

    }
}
