package net.odbogm.cache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.ObjectMapper;
import net.odbogm.Primitives;
import static net.odbogm.Primitives.PRIMITIVE_MAP;
import net.odbogm.annotations.Eager;
import net.odbogm.annotations.Embedded;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.Indirect;
import net.odbogm.annotations.RID;
import net.odbogm.annotations.Sequence;
import net.odbogm.annotations.Version;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.IncorrectSequenceField;
import net.odbogm.exceptions.IncorrectVersionField;

/**
 * Caché con la definición de clases (ClassDef's).
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ClassCache {

    private final static Logger LOGGER = Logger.getLogger(ClassCache.class.getName());

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ClassCache);
        }
    }

    private final HashMap<Class<?>, ClassDef> classCache = new HashMap<>();

    public ClassCache() {
    }

    /**
     * Devuelve el mapa de la clase obteniéndolo desde el cache. Si no exite, analiza la clase y lo agrega.
     *
     * @param c: reference class.
     * @return a ClassDefiniton object
     */
    public ClassDef get(Class<?> c) {
        LOGGER.log(Level.FINER, "Procesando clase: {0}", c.getName());
        
        Class<?> toProcess = c;
        // buscar la primera clase que no sea un proxy de CGLIB
        // para esto, el nombre de la clase no debe tener la cadena $$EnhancerByCGLIB$$
        if (c.getName().contains("$$EnhancerByCGLIB$$")) {
            toProcess = c.getSuperclass();
        }
        
        ClassDef cached = classCache.get(toProcess);
        if (cached == null) {
            LOGGER.log(Level.FINER, "Nueva clase detectada. Analizando...");
            cached = this.cacheClass(toProcess);
            this.classCache.put(toProcess, cached);
        }
        LOGGER.log(Level.FINER, "Class struc:");
        LOGGER.log(Level.FINER, "Class: " + toProcess.getName());
        LOGGER.log(Level.FINER, "Fields: " + cached.fields.size()+ " - " + cached.fields);
        LOGGER.log(Level.FINER, "enums: " + cached.enumFields.size()+ " - " + cached.enumFields);
        LOGGER.log(Level.FINER, "Links: " + cached.links.size()+ " - " + cached.links);
        LOGGER.log(Level.FINER, "LinkList: " + cached.linkLists.size()+ " - " + cached.linkLists);
        LOGGER.log(Level.FINER, "Indirect Link: " + cached.indirectLinks.size()+ " - " + cached.indirectLinks);
        LOGGER.log(Level.FINER, "Indirect LinkList: " + cached.indirectLinkLists.size()+ " - " + cached.indirectLinkLists);
        LOGGER.log(Level.FINER, "-------------------------------------");
        return cached;
    }

    /**
     * Analiza la clase y devuelve un mapa con las definiciones de campo.
     * Además agrega la clase al caché.
     *
     * @param c reference class.
     */
    private ClassDef cacheClass(Class<?> c) {
        ClassDef classdef = new ClassDef();
        this.cacheClass(c, classdef);
        return classdef;
    }

    private void cacheClass(Class<?> c, ClassDef cached) {
        if (c != Object.class) {
            LOGGER.log(Level.FINER, "Clase: {0}", c.getName());
            
            // iniciamos analizando la superclass y luego seguimos con los campos de la clase 
            // actual. Esto es así para que los shallow fields se agreguen correctamente 
            // en los HM del caché de clases.
            LOGGER.log(Level.FINER, "Analizando superclass...");
            this.cacheClass(c.getSuperclass(), cached);
            
            cached.entityName = getEntityName(c);
            cached.isEager = c.isAnnotationPresent(Eager.class);
            
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields) {
                try {
                    //determinar si se debe o no procesar el campo.
                    //No se aceptan los transient y static final.
                    if (!(f.isAnnotationPresent(Ignore.class)
                            || Modifier.isTransient(f.getModifiers())
                            || (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
                            || f.getName().startsWith("___ogm___"))
                            )) {
                        
                        f.setAccessible(true);
                        
                        //rid field:
                        if (f.isAnnotationPresent(RID.class)) {
                            if (!f.getType().equals(String.class)) {
                                throw new IncorrectRIDField("A field annotated with @RID must be of type String.");
                            }
                            if (cached.ridField != null) {
                                throw new IncorrectRIDField("Only one field can be annotated with @RID.");
                            }
                            cached.ridField = f;
                            continue;
                        }
                        
                        //version field:
                        if (f.isAnnotationPresent(Version.class)) {
                            if (!f.getType().equals(Integer.class) && !f.getType().equals(int.class)) {
                                throw new IncorrectVersionField("A field annotated with @Version must be of type int or Integer.");
                            }
                            if (cached.versionField != null) {
                                throw new IncorrectVersionField("Only one field can be annotated with @Version.");
                            }
                            cached.versionField = f;
                            continue;
                        }
                        
                        //eager field?
                        if (f.isAnnotationPresent(Eager.class)) {
                            cached.isEager = true;
                        }
                        
                        cached.fieldsObject.put(f.getName(), f);

                        // determinar si es un campo permitido
                        // FIXME: falta considerar la posibilidad de los Embedded Object
                        LOGGER.log(Level.FINER, "Field: {0}  Type: {1}{2}",
                                new Object[]{f.getName(), f.getType(), f.getType().isEnum() ? "<<<<<<<<<<< ENUM" : ""});
                        
                        if (PRIMITIVE_MAP.get(f.getType()) != null) {
                            cached.fields.put(f.getName(), f.getType());
                            //check if it's a sequence field:
                            Sequence annotation = f.getAnnotation(Sequence.class);
                            if (annotation != null) {
                                if (!f.getType().equals(Long.class)) {
                                    throw new IncorrectSequenceField();
                                }
                                cached.sequenceFields.put(f.getName(), annotation.sequenceName());
                            }
                        } else if (f.getType().isEnum()) {
                            cached.enumFields.put(f.getName(), f.getType());
                        } else if (Primitives.LAZY_COLLECTION.get(f.getType()) != null) {
                            // FIXME: ojo que si se tratara de una extensión de AL o HM 
                            // no lo vería como tal y lo vincularía con un link

                            // primero verificar si se trata de una colección de objeto primitivos
                            // ej: ArrayList<String> ... 
                            // En este caso, no correspondería crear Edges. La colección completa
                            // se va a guardar embebida en el Vertex.
                            LOGGER.log(Level.FINER, "Colección detectada: {0}", f.getName());
                            boolean setAsEmbedded = false;
                            boolean isEnumCollection = false;
                            if (List.class.isAssignableFrom(f.getType())) {
                                LOGGER.log(Level.FINER, "Se trata de una Lista...");
                                // se trata de una lista. Verificar el subtipo o el @Embedded
                                
                                ParameterizedType listType = (ParameterizedType) f.getGenericType();
                                Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                                String typeName = listClass.getSimpleName();
                                if (listClass.isEnum()) {
                                    LOGGER.log(Level.FINER, "Es una colección de enums: {0}", typeName);
                                    isEnumCollection = true;
                                } else if ((Primitives.PRIMITIVE_MAP.get(listClass) != null)
                                        || (f.isAnnotationPresent(Embedded.class))) {
                                    LOGGER.log(Level.FINER, "\n**********************************************************");
                                    if (f.isAnnotationPresent(Embedded.class)) {
                                        LOGGER.log(Level.FINER, "Clase anotada como Embedded: {0}", typeName);
                                    } else {
                                        LOGGER.log(Level.FINER, "Es una colección de primitivas: {0}", typeName);
                                        LOGGER.log(Level.FINER, "Se procede a embeberla.");
                                    }
                                    LOGGER.log(Level.FINER, "\n**********************************************************");
                                    setAsEmbedded = true;
                                }
                            } else if (Map.class.isAssignableFrom(f.getType())) {
                                // si se trata de un Map, verificar que el tipo del valor almacenado
                                // sea una primitiva
                                LOGGER.log(Level.FINER, "se trata de un Map...");
                                ParameterizedType listType = (ParameterizedType) f.getGenericType();
                                Class<?> keyClass = (Class<?>) listType.getActualTypeArguments()[0];
                                Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[1];

                                // para que un map pueda ser embebido tiene que tener el key: string y si el value es una primitiva
                                // directamente lo embebemos. En caso contrario, si existe el annotation @Embedded tambien
                                // lo marcamos como campo.
                                if ((keyClass == String.class)
                                        && ((Primitives.PRIMITIVE_MAP.get(valClass) != null)
                                        || (f.isAnnotationPresent(Embedded.class)))) {
                                    LOGGER.log(Level.FINER, "Es una colección de embebida: {0}", valClass.getSimpleName());
                                    setAsEmbedded = true;
                                }
                            }
                            
                            if (setAsEmbedded) {
                                // es una colección de primitivas. Tratarla como un field común
                                cached.fields.put(f.getName(), f.getType());
                                // FIXME: verificar si se puede unificar y no registrar en dos lados.
                                cached.embeddedFields.put(f.getName(), f.getType());
                            } else if (isEnumCollection) {
                                cached.enumCollectionFields.put(f.getName(), f.getType());
                            } else if (f.isAnnotationPresent(Indirect.class)) {
                                // es una colección de objetos indirectos.
                                LOGGER.log(Level.FINER, "Es una colección de objetos indirectos.");
                                cached.indirectLinkLists.put(f.getName(), f.getType());
                            } else {
                                // es una colección de objetos.
                                LOGGER.log(Level.FINER, "Es una colección de objetos que genera Vértices y Ejes.");
                                cached.linkLists.put(f.getName(), f.getType());
                            }
                        } else {
                            if (f.isAnnotationPresent(Indirect.class)) {
                                // es un objetos indirectos.
                                LOGGER.log(Level.FINER, "Es un objeto indirecto.");
                                cached.indirectLinks.put(f.getName(), f.getType());
                            } else {
                                // FIXME: los @Embedded sobre las propiedades pueden generar problemas para detectar los cambios en los 
                                // objetos embebidos. Ojo con como se procesan.
                                LOGGER.log(Level.FINER, "Link detectado!");
                                cached.links.put(f.getName(), f.getType());
                            }
                        }

                    } else {
                        if (!(f.isAnnotationPresent(Ignore.class))) {
                            if (f.getName().startsWith("___ogm___")) {
                                LOGGER.log(Level.FINER, "Ignorado: {0}", f.getName());
                            } else {
                                LOGGER.log(Level.WARNING, "Ignorado: {0}", f.getName());
                            }
                        }
                    }

                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            LOGGER.log(Level.FINER, "Fin clase {0} <<<<<<<<<<<<<<<<<", c.getName());
        }
    }

        
    public static String getEntityName(Class<?> cls) {
        Entity entity = cls.getAnnotation(Entity.class);
        return (entity == null || entity.name().isEmpty()) ? cls.getSimpleName() : entity.name();
    }
    
}
