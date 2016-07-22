/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.cache;

import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.Link;
import net.odbogm.annotations.LinkList;
import net.odbogm.ObjectMapper;
import net.odbogm.Primitives;
import static net.odbogm.Primitives.PRIMITIVE_MAP;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;

/**
 *
 * @author SShadow
 */
public class ClassCache {

    private final static Logger LOGGER = Logger.getLogger(ClassCache.class.getName());
    static {
        LOGGER.setLevel(LogginProperties.ClassCache);
    }
    
    private final HashMap<Class<?>, ClassDef> classCache = new HashMap<>();

    public ClassCache() {
    }

    /**
     * Devuelve el mapa de la clase obteniéndolo desde el cache. Si no exite, analiza la clase y lo agrega.
     *
     * @param c
     * @return
     */
    public ClassDef get(Class<?> c) {
        LOGGER.log(Level.FINER, "Procesando clase: {0}", c.getName());
        ClassDef cached = classCache.get(c);
        if (cached == null) {
            LOGGER.log(Level.FINER, "Nueva clase detectada. Analizando...");
            cached = this.cacheClass(c);
            this.classCache.put(c, cached);
        }
        return cached;
    }

    /**
     * Analiza la clase y devuelve un mapa con las definiciones de campo. Además agrega la clase al cache
     *
     * @param c
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
                            )
//                            || f.getName().startsWith("___")
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
                            // FIXME: ojo que si se tratara de una extensín de AL o HM no lo vería como tal y lo vincularía con un link
                            cached.linkLists.put(f.getName(), f.getType());
                        } else {
                            cached.links.put(f.getName(), f.getType());
//                        } else {
//                            LOGGER.log(Level.WARNING, "NO PERSISTIDO: {0}.{1}", new Object[]{c.getName(), f.getName()});
                        }

                        // resstablecer el campo
                        f.setAccessible(acc);
                        
                    } else {
                        if (!(f.isAnnotationPresent(Ignore.class)))
                            LOGGER.log(Level.WARNING, "Ignorado: {0}", f.getName());
                    }

                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            this.cacheClass(c.getSuperclass(), cached);
        }
    }

}
