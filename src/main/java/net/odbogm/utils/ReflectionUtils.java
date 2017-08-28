/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import static net.odbogm.LogginProperties.ArrayListEmbeddedProxy;
import static net.odbogm.LogginProperties.HashMapEmbeddedProxy;
import net.odbogm.proxy.ArrayListEmbeddedProxy;
import net.odbogm.proxy.HashMapEmbeddedProxy;
import net.odbogm.proxy.IObjectProxy;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ReflectionUtils {

    private final static Logger LOGGER = Logger.getLogger(ReflectionUtils.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.ReflectionUtils);
    }

    public static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        do {
            try {
                return current.getDeclaredField(fieldName);
            } catch (Exception e) {
            }
        } while ((current = current.getSuperclass()) != null);
        throw new NoSuchFieldException(fieldName);
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramType) throws NoSuchMethodException {
        Class<?> current = clazz;
        do {
            try {
                return current.getDeclaredMethod(methodName, paramType);
            } catch (Exception e) {
            }
        } while ((current = current.getSuperclass()) != Object.class);
        throw new NoSuchMethodException(methodName);
    }

    /**
     * Copia todos los atributos del objeto "from" al objeto "to". Si convertToProxy = true las colencciones se convierten a colecciones Embebidas
     *
     * @param from objeto origen
     * @param to objeto destina
     * @param convertToProxy determina si se convierten las colecciones a listas/mapas embebidos.
     *
     */
    public static void copyObject(Object from, Object to, boolean convertToProxy) {
        // Walk up the superclass hierarchy
        for (Class obj = from.getClass();
                !obj.equals(Object.class);
                obj = obj.getSuperclass()) {
            Field[] fields = obj.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                try {
                    // for each class/suerclass, copy all fields
                    // from this object to the clone

                    if (convertToProxy) {
                        if (fields[i].getType().isAssignableFrom(List.class)) {
                            // se debe hacer una copia del la lista para no quede referenciando al objeto original
                            // dado que en la asignación solo se pasa la referencia del objeto.
                            LOGGER.log(Level.FINER, "Lista detectada: realizando una copia del contenido...");
                            fields[i].set(to, new ArrayListEmbeddedProxy((IObjectProxy) to, (List) fields[i].get(from)));
                        } else if (fields[i].getType().isAssignableFrom(Map.class)) {
                            // se debe hacer una copia del la lista para no quede referenciando al objeto original
                            // dado que en la asignación solo se pasa la referencia del objeto.
                            LOGGER.log(Level.FINER, "Map detectado: realizando una copia del contenido...");
                            // FIXME: Ojo que se hace solo un shalow copy!! no se está conando la clave y el value
                            fields[i].set(to, new HashMapEmbeddedProxy((IObjectProxy) to, (Map) fields[i].get(from)));
                        } else {
                            fields[i].set(to, fields[i].get(from));
                        }
                    } else {
                        // si no hay conversión para embebidos, se copia directamente los valores
                        fields[i].set(to, fields[i].get(from));
                    }

                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                }
            }
        }
    }

    /**
     * Copia todos los atributos del objeto "from" al objeto "to". 
     * no se realiza la conversión de las listas/mapas a embedded
     *
     * @param from objeto origen
     * @param to objeto destina
     *
     */
    public static void copyObject(Object from, Object to) {
        ReflectionUtils.copyObject(from, to, false);
    }
    
}
