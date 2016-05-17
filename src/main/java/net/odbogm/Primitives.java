/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import net.odbogm.proxy.ArrayListLazyProxy;
import net.odbogm.proxy.HashMapLazyProxy;
import net.odbogm.proxy.LinkedListLazyProxy;
import net.odbogm.proxy.VectorLazyProxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author SShadow
 */
public class Primitives {
    public enum PRIMITIVE {

        BOOLEAN,
        BYTE,
        CHAR,
        STRING,
        DOUBLE,
        FLOAT,
        INT,
        LONG,
        SHORT,
        BIGDECIMAL
    }
    public static final IdentityHashMap<Class<?>, PRIMITIVE> PRIMITIVE_MAP = new IdentityHashMap<>();
    public static final IdentityHashMap<Class<?>, Class<?>> LAZY_COLLECTION = new IdentityHashMap<>();

    static {
        PRIMITIVE_MAP.put(Boolean.class, PRIMITIVE.BOOLEAN);
        PRIMITIVE_MAP.put(Byte.class, PRIMITIVE.BYTE);
        PRIMITIVE_MAP.put(Character.class, PRIMITIVE.CHAR);
        PRIMITIVE_MAP.put(Double.class, PRIMITIVE.DOUBLE);
        PRIMITIVE_MAP.put(Float.class, PRIMITIVE.FLOAT);
        PRIMITIVE_MAP.put(Integer.class, PRIMITIVE.INT);
        PRIMITIVE_MAP.put(Long.class, PRIMITIVE.LONG);
        PRIMITIVE_MAP.put(Short.class, PRIMITIVE.SHORT);
        
        PRIMITIVE_MAP.put(BigDecimal.class, PRIMITIVE.BIGDECIMAL);

        PRIMITIVE_MAP.put(String.class, PRIMITIVE.STRING);

        PRIMITIVE_MAP.put(Boolean.TYPE, PRIMITIVE.BOOLEAN);
        PRIMITIVE_MAP.put(Byte.TYPE, PRIMITIVE.BYTE);
        PRIMITIVE_MAP.put(Character.TYPE, PRIMITIVE.CHAR);
        PRIMITIVE_MAP.put(Double.TYPE, PRIMITIVE.DOUBLE);
        PRIMITIVE_MAP.put(Float.TYPE, PRIMITIVE.FLOAT);
        PRIMITIVE_MAP.put(Integer.TYPE, PRIMITIVE.INT);
        PRIMITIVE_MAP.put(Long.TYPE, PRIMITIVE.LONG);
        PRIMITIVE_MAP.put(Short.TYPE, PRIMITIVE.SHORT);
        
        
        LAZY_COLLECTION.put(List.class,ArrayListLazyProxy.class);
        LAZY_COLLECTION.put(ArrayList.class,ArrayListLazyProxy.class);
        LAZY_COLLECTION.put(LinkedList.class,LinkedListLazyProxy.class);
        LAZY_COLLECTION.put(Vector.class,VectorLazyProxy.class);
        
        LAZY_COLLECTION.put(HashMap.class,HashMapLazyProxy.class);
        
    }

    
}
