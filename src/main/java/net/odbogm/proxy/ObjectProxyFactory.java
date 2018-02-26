/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import net.odbogm.SessionManager;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.cglib.proxy.Enhancer;
import net.odbogm.LogginProperties;
import net.odbogm.Transaction;
import net.odbogm.agent.ITransparentDirtyDetector;
import net.odbogm.agent.TransparentDirtyDetectorInstrumentator;
import net.sf.cglib.core.DefaultGeneratorStrategy;
import net.sf.cglib.proxy.CallbackFilter;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectProxyFactory {

    private final static Logger LOGGER = Logger.getLogger(ObjectProxyFactory.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.ObjectProxyFactory);
    }

    public enum proxyLibrary {
        CGLIB, BB
    };
    private static proxyLibrary library = proxyLibrary.CGLIB;

    public static <T> T create(T o, OrientElement oe, Transaction transaction) {
//        if (library == proxyLibrary.BB)
//            return bbcreate(o, oe, sm);
//        else
        return cglibcreate(o, oe, transaction);
    }

    public static <T> T create(T o, OrientElement oe, Transaction transaction, proxyLibrary proxyLibrary) {
        library = proxyLibrary;
        return create(o, oe, transaction);
    }

    public static <T> T create(Class<T> c, OrientElement ov, Transaction transaction) {
//        if (library == proxyLibrary.BB)
//            return bbcreate(c, ov, sm);
//        else
        return cglibcreate(c, ov, transaction);
    }

    public static <T> T create(Class<T> c, OrientElement ov, Transaction transaction, proxyLibrary proxyLibrary) {
        library = proxyLibrary;
        return create(c, ov, transaction);
    }

//    /**
//     * Devuelve un proxy a partir de un objeto existente y copia todos los valores del objeto original al 
//     * nuevo objecto provisto por el proxy
//     * @param <T>
//     * @param o
//     * @param oe
//     * @param sm
//     * @return 
//     */
//    public static <T> T bbcreate(T o, OrientElement oe, SessionManager sm ) {
//        T po = null;
//        try {
//            ObjectProxy bbi = new ObjectProxy(o,oe,sm);
//            ClassLoader mpcl = new MultipleParentClassLoader.Builder()
//                                                .append(IObjectProxy.class, o.getClass())
//                                                .build();
//            //mpcl = o.getClass().getClassLoader();
//            
//            po = (T) new ByteBuddy()
//                    .subclass(o.getClass())
//                    .implement(IObjectProxy.class)
////                        .method(isDeclaredBy(IObjectProxy.class))
//                        .method(any())
//                        .intercept(MethodDelegation.to(bbi))
//                    .make()
//                    .load(mpcl, ClassLoadingStrategy.Default.WRAPPER)
//                    .getLoaded().newInstance();
//            bbi.___setProxyObject(po);
//            
//        } catch (InstantiationException ex) {
//            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return po;
//    }
//
//    /**
//     * Devuelve un proxy a partir de una definición de clase.
//     * @param <T>
//     * @param c
//     * @param ov
//     * @param sm
//     * @return 
//     */
//    public static <T> T bbcreate(Class<T> c, OrientElement ov, SessionManager sm ) {
//        T po = null;
//        try {
//            ObjectProxy bbi = new ObjectProxy(c,ov,sm);
//            ClassLoader mpcl = new MultipleParentClassLoader.Builder()
//                                                .append(IObjectProxy.class, c)
//                                                .build();
//            // mpcl = c.getClassLoader();
//            po = (T) new ByteBuddy()
//                    .subclass(c)
//                    .implement(IObjectProxy.class)
//                    .method(isDeclaredBy(IObjectProxy.class))
//                    .intercept(MethodDelegation.to(bbi))
//                    .make()
//                    .load(mpcl, ClassLoadingStrategy.Default.WRAPPER)
//                    .getLoaded().newInstance();
//            bbi.___setProxyObject(po);
//        } catch (InstantiationException ex) {
//            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return po;
//    }
    // Implementación con CGLib
    public static <T> T cglibcreate(T o, OrientElement oe, Transaction transaction) {
        // this is the main cglib api entry-point
        // this object will 'enhance' (in terms of CGLIB) with new capabilities
        // one can treat this class as a 'Builder' for the dynamic proxy
        Enhancer e = new Enhancer();

        // the class will extend from the real class
        e.setSuperclass(o.getClass());
        // we have to declare the interceptor  - the class whose 'intercept'
        // will be called when any method of the proxified object is called.
        ObjectProxy po = new ObjectProxy(o.getClass(), oe, transaction);
        e.setCallback(po);
        e.setInterfaces(new Class[]{IObjectProxy.class});
//        e.setStrategy(new DefaultGeneratorStrategy() {
//
//            public byte[] transform(byte[] b) {
//                try (FileOutputStream fos = new FileOutputStream("/tmp/1/" + o.getClass().getName() + "_cglib.class")) {
//                    fos.write(b);
//                    fos.close();
//                } catch (IOException ex) {
//                    Logger.getLogger(TransparentDirtyDetectorInstrumentator.class.getName()).log(Level.SEVERE, null, ex);
//                }
//
//                return b;
//            }
//        });
        
        // now the enhancer is configured and we'll create the proxified object
        T proxifiedObj = (T) e.create();

        po.___setProxyObject(proxifiedObj);

        // the object is ready to be used - return it
        return proxifiedObj;
    }

    public static <T> T cglibcreate(Class<T> c, OrientElement oe, Transaction transaction) {
        // this is the main cglib api entry-point
        // this object will 'enhance' (in terms of CGLIB) with new capabilities
        // one can treat this class as a 'Builder' for the dynamic proxy
        Enhancer e = new Enhancer();

        // the class will extend from the real class
        e.setSuperclass(c);
        // we have to declare the interceptor  - the class whose 'intercept'
        // will be called when any method of the proxified object is called.
        ObjectProxy po = new ObjectProxy(c, oe, transaction);
        e.setCallback(po);
        e.setInterfaces(new Class[]{IObjectProxy.class});

        // now the enhancer is configured and we'll create the proxified object
        T proxifiedObj = (T) e.create();

        po.___setProxyObject(proxifiedObj);

        // the object is ready to be used - return it
        return proxifiedObj;
    }

}
