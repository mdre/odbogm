/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.proxy;

import net.odbogm.SessionManager;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import static java.lang.ClassLoader.getSystemClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.implementation.MethodDelegation;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import net.sf.cglib.proxy.Enhancer;

/**
 *
 * @author SShadow
 */
public class ObjectProxyFactory {
    private final static Logger LOGGER = Logger.getLogger(ObjectProxyFactory.class .getName());
    
    private enum lib {CGLIB,BB};
    private static lib library = lib.CGLIB;
    
    public static <T> T create(T o, OrientElement oe, SessionManager sm ) {
        if (library == lib.BB)
            return bbcreate(o, oe, sm);
        else
            return cglibcreate(o, oe, sm);
    }
    
    public static <T> T create(Class<T> c, OrientElement ov, SessionManager sm ) {
        if (library == lib.BB)
            return bbcreate(c, ov, sm);
        else
          return cglibcreate(c, ov, sm);
    }
    
    /**
     * Devuelve un proxy a partir de un objeto existente y copia todos los valores del objeto original al 
     * nuevo objecto provisto por el proxy
     * @param <T>
     * @param o
     * @param oe
     * @param sm
     * @return 
     */
    public static <T> T bbcreate(T o, OrientElement oe, SessionManager sm ) {
        T po = null;
        try {
            ObjectProxy bbi = new ObjectProxy(o,oe,sm);
            ClassLoader mpcl = new MultipleParentClassLoader.Builder()
                                                .append(IObjectProxy.class, o.getClass())
                                                .build();
            
            po = (T) new ByteBuddy()
                    .subclass(o.getClass())
                    .implement(IObjectProxy.class)
//                        .method(isDeclaredBy(IObjectProxy.class))
                        .method(any())
                        .intercept(MethodDelegation.to(bbi))
                    .make()
                    .load(o.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded().newInstance();
            bbi.___setProxyObject(po);
            
        } catch (InstantiationException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return po;
    }

    /**
     * Devuelve un proxy a partir de una definición de clase.
     * @param <T>
     * @param c
     * @param ov
     * @param sm
     * @return 
     */
    public static <T> T bbcreate(Class<T> c, OrientElement ov, SessionManager sm ) {
        T po = null;
        try {
            ObjectProxy bbi = new ObjectProxy(c,ov,sm);
            ClassLoader mpcl = new MultipleParentClassLoader.Builder()
                                                .append(IObjectProxy.class, c)
                                                .build();
            
            po = (T) new ByteBuddy()
                    .subclass(c)
                    .implement(IObjectProxy.class)
                    .method(isDeclaredBy(IObjectProxy.class))
                    .intercept(MethodDelegation.to(bbi))
                    .make()
                    .load(mpcl, ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded().newInstance();
            bbi.___setProxyObject(po);
        } catch (InstantiationException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return po;
    }
    
    
    // Implementación con CGLib
    public static <T> T cglibcreate(T o, OrientElement oe, SessionManager sm ) {
        // this is the main cglib api entry-point
        // this object will 'enhance' (in terms of CGLIB) with new capabilities
        // one can treat this class as a 'Builder' for the dynamic proxy
        Enhancer e = new Enhancer();

        // the class will extend from the real class
        e.setSuperclass(o.getClass());
        // we have to declare the interceptor  - the class whose 'intercept'
        // will be called when any method of the proxified object is called.
        ObjectProxy po = new ObjectProxy(o.getClass(),oe, sm);
        e.setCallback(po);
        e.setInterfaces(new Class[]{IObjectProxy.class});

        // now the enhancer is configured and we'll create the proxified object
        T proxifiedObj = (T) e.create();
        
        po.___setProxyObject(proxifiedObj);
        
        // the object is ready to be used - return it
        return proxifiedObj;
    }
    
    public static <T> T cglibcreate(Class<T> c, OrientElement oe, SessionManager sm ) {
        // this is the main cglib api entry-point
        // this object will 'enhance' (in terms of CGLIB) with new capabilities
        // one can treat this class as a 'Builder' for the dynamic proxy
        Enhancer e = new Enhancer();

        // the class will extend from the real class
        e.setSuperclass(c);
        // we have to declare the interceptor  - the class whose 'intercept'
        // will be called when any method of the proxified object is called.
        ObjectProxy po = new ObjectProxy(c,oe, sm);
        e.setCallback(po);
        e.setInterfaces(new Class[]{IObjectProxy.class});

        // now the enhancer is configured and we'll create the proxified object
        T proxifiedObj = (T) e.create();
        
        po.___setProxyObject(proxifiedObj);
        
        // the object is ready to be used - return it
        return proxifiedObj;
    }
    
    
    
}
