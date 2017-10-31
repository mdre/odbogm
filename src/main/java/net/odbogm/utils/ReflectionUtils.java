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
     * Copia todos los atributos del objeto "from" al objeto "to".
     * 
     * @param from objeto origen
     * @param to objeto destina
     *
     */
    public static void copyObject(Object from, Object to) {
        // Walk up the superclass hierarchy
        for (Class obj = from.getClass();
                !obj.equals(Object.class);
                obj = obj.getSuperclass()) {
            Field[] fields = obj.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                try {
                    
                    fields[i].set(to, fields[i].get(from));

                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                }
            }
        }
    }
    
    
}
