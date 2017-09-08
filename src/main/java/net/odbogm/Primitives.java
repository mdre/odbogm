/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import com.orientechnologies.orient.core.metadata.schema.OType;
import net.odbogm.proxy.ArrayListLazyProxy;
import net.odbogm.proxy.HashMapLazyProxy;
import net.odbogm.proxy.LinkedListLazyProxy;
import net.odbogm.proxy.VectorLazyProxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
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
        BIGDECIMAL,
        DATE
    }
    public static final IdentityHashMap<Class<?>, OType> PRIMITIVE_MAP = new IdentityHashMap<>();
    public static final IdentityHashMap<Class<?>, Class<?>> LAZY_COLLECTION = new IdentityHashMap<>();

    static {
        PRIMITIVE_MAP.put(Boolean.class, OType.BOOLEAN);
        PRIMITIVE_MAP.put(Byte.class, OType.BYTE);
        PRIMITIVE_MAP.put(Double.class, OType.DOUBLE);
        PRIMITIVE_MAP.put(Float.class, OType.FLOAT);
        PRIMITIVE_MAP.put(Integer.class, OType.INTEGER);
        PRIMITIVE_MAP.put(Long.class, OType.LONG);
        PRIMITIVE_MAP.put(Short.class, OType.SHORT);
        
        PRIMITIVE_MAP.put(BigDecimal.class, OType.DECIMAL);

        PRIMITIVE_MAP.put(String.class, OType.STRING);
        
        PRIMITIVE_MAP.put(Date.class, OType.DATETIME);

        PRIMITIVE_MAP.put(Boolean.TYPE, OType.BOOLEAN);
        PRIMITIVE_MAP.put(Byte.TYPE, OType.BYTE);
        PRIMITIVE_MAP.put(Double.TYPE, OType.DOUBLE);
        PRIMITIVE_MAP.put(Float.TYPE, OType.FLOAT);
        PRIMITIVE_MAP.put(Integer.TYPE, OType.INTEGER);
        PRIMITIVE_MAP.put(Long.TYPE, OType.LONG);
        PRIMITIVE_MAP.put(Short.TYPE, OType.SHORT);
        
        
        LAZY_COLLECTION.put(List.class,ArrayListLazyProxy.class);
        LAZY_COLLECTION.put(ArrayList.class,ArrayListLazyProxy.class);
        LAZY_COLLECTION.put(LinkedList.class,LinkedListLazyProxy.class);
        LAZY_COLLECTION.put(Vector.class,VectorLazyProxy.class);
        
        LAZY_COLLECTION.put(HashMap.class,HashMapLazyProxy.class);
        LAZY_COLLECTION.put(Map.class,HashMapLazyProxy.class);
        
        
    }

    
}
