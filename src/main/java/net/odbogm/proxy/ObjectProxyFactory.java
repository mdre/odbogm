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
import net.bytebuddy.implementation.MethodDelegation;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 *
 * @author SShadow
 */
public class ObjectProxyFactory {
    private final static Logger LOGGER = Logger.getLogger(ObjectProxyFactory.class .getName());

    /**
     * Devuelve un proxy a partir de un objeto existente y copia todos los valores del objeto original al 
     * nuevo objecto provisto por el proxy
     * @param <T>
     * @param o
     * @param oe
     * @param sm
     * @return 
     */
    public static <T> T create(T o, OrientElement oe, SessionManager sm ) {
        T po = null;
        try {
            ObjectProxy bbi = new ObjectProxy(o,oe,sm);
            po = (T) new ByteBuddy()
                    .subclass(o.getClass())
                    .implement(IObjectProxy.class)
//                        .method(isDeclaredBy(IObjectProxy.class))
                        .method(any())
                        .intercept(MethodDelegation.to(bbi))
                    .make()
                    .load(o.getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
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
    public static <T> T create(Class<T> c, OrientElement ov, SessionManager sm ) {
        T po = null;
        try {
            ObjectProxy bbi = new ObjectProxy(c,ov,sm);
            po = (T) new ByteBuddy()
                    .subclass(c)
                    .implement(IObjectProxy.class)
                    .method(isDeclaredBy(IObjectProxy.class))
                    .intercept(MethodDelegation.to(bbi))
                    .make()
                    .load(getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded().newInstance();

        } catch (InstantiationException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return po;
    }
}
