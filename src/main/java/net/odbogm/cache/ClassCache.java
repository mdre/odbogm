/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.cache;

import net.odbogm.annotations.Ignore;
import net.odbogm.ObjectMapper;
import net.odbogm.Primitives;
import static net.odbogm.Primitives.PRIMITIVE_MAP;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Embedded;

/**
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
        ClassDef cached = classCache.get(c);
        if (cached == null) {
            LOGGER.log(Level.FINER, "Nueva clase detectada. Analizando...");
            cached = this.cacheClass(c);
            this.classCache.put(c, cached);
        }
        LOGGER.log(Level.FINER, "Class struc:");
        LOGGER.log(Level.FINER, "Class: "+c.getName());
        LOGGER.log(Level.FINER, "Fields: "+cached.fields.size());
        LOGGER.log(Level.FINER, "enums: "+cached.enumFields.size());
        LOGGER.log(Level.FINER, "Links: "+cached.links.size());
        LOGGER.log(Level.FINER, "LinkList: "+cached.linkLists.size());
        LOGGER.log(Level.FINER, "-------------------------------------");
        return cached;
    }

    /**
     * Analiza la clase y devuelve un mapa con las definiciones de campo. Además agrega la clase al cache
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
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields) {
                try {
                    // determinar si se debe o no procesar el campo. No se aceptan los trascient y static final
                    if ( !(    f.isAnnotationPresent(Ignore.class) 
                            || Modifier.isTransient(f.getModifiers())
                            || (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
                            || f.getName().startsWith("___ogm___")
                            )
//                            || f.getName().startsWith("GCLIB")
                            )
                            ) {
                        boolean acc = f.isAccessible();
                        f.setAccessible(true);
                        
                        // determinar si es un campo permitido
                        // FIXME: falta considerar la posibilidad de los Embedded Object
                        LOGGER.log(Level.FINER, "Field: "+f.getName()+"  Type: "+f.getType()+(f.getType().isEnum()?"<<<<<<<<<<< ENUM":""));
                        if (PRIMITIVE_MAP.get(f.getType()) != null) {
                            cached.fields.put(f.getName(), f.getType());
                        } else if (f.getType().isEnum()) {
                            cached.enumFields.put(f.getName(), f.getType());
                        } else if (Primitives.LAZY_COLLECTION.get(f.getType())!=null) {
                            // FIXME: ojo que si se tratara de una extensión de AL o HM 
                            // no lo vería como tal y lo vincularía con un link
                            
                            // primero verificar si se trata de una colección de objeto primitivos
                            // ej: ArrayList<String> ... 
                            // En este caso, no correspondería crear Edges. La colección completa
                            // se va a guardar embebida en el Vertex.
                            LOGGER.log(Level.FINER, "Colección detectada: "+f.getName());
                            boolean setAsEmbedded = false;
                            if (List.class.isAssignableFrom(f.getType())) {
                                LOGGER.log(Level.FINER, "se trata de una Lista...");
                                // se trata de una lista. Verificar el subtipo o el @Embedded
                                ParameterizedType listType = (ParameterizedType) f.getGenericType();
                                Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                                if ((Primitives.PRIMITIVE_MAP.get(listClass)!=null)
                                        ||(f.isAnnotationPresent(Embedded.class))) {
                                    LOGGER.log(Level.FINER, "\n**********************************************************");
                                    LOGGER.log(Level.FINER, "Es una colección de primitivas: "+listClass.getSimpleName());
                                    LOGGER.log(Level.FINER, "Se procede a embeberla.");
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
                                if ( (keyClass == String.class)
                                        && ((Primitives.PRIMITIVE_MAP.get(valClass)!=null)
                                            || (f.isAnnotationPresent(Embedded.class))
                                            )
                                        ) {
                                    LOGGER.log(Level.FINER, "Es una colección de embebida: "+valClass.getSimpleName());
                                    setAsEmbedded = true;
                                }
                            }
                            if (setAsEmbedded) {
                                // es una colección de primitivas. Tratarla como un field común
                                cached.fields.put(f.getName(), f.getType());
                                // FIXME: verificar si se puede unificar y no registrar en dos lados.
                                cached.embeddedFields.put(f.getName(), f.getType());
                            } else {
                                // es una colección de objetos.
                                LOGGER.log(Level.FINER, "Es una colección de objetos que genera Vértices y Ejes.");
                                cached.linkLists.put(f.getName(), f.getType());
                            }
                        } else {
                            // FIXME: los @Embedded sobre las propiedades pueden generar problemas para detectar los cambios en los 
                            // objetos embebidos. Ojo con como se procesan.
                            cached.links.put(f.getName(), f.getType());
//                        } else {
//                            LOGGER.log(Level.WARNING, "NO PERSISTIDO: {0}.{1}", new Object[]{c.getName(), f.getName()});
                        }

                        // resstablecer el campo
                        f.setAccessible(acc);
                        
                    } else {
                        if (!(f.isAnnotationPresent(Ignore.class)))
                            if (f.getName().startsWith("___ogm___")) {
                                LOGGER.log(Level.FINER, "Ignorado: {0}", f.getName());
                            } else {
                                LOGGER.log(Level.WARNING, "Ignorado: {0}", f.getName());
                            }
                    }

                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            this.cacheClass(c.getSuperclass(), cached);
        }
    }

}
