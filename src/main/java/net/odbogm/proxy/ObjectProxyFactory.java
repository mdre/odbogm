package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.OElement;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.Transaction;
import net.sf.cglib.proxy.Enhancer;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectProxyFactory {

    private final static Logger LOGGER = Logger.getLogger(ObjectProxyFactory.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ObjectProxyFactory);
        }
    }


    public static <T> T create(T o, OElement oe, Transaction transaction) {
        return cglibcreate((Class<T>)o.getClass(), oe, transaction);
    }


    public static <T> T create(Class<T> c, OElement ov, Transaction transaction) {
        return cglibcreate(c, ov, transaction);
    }


    // Implementación con CGLib
    private static <T> T cglibcreate(Class<T> c, OElement oe, Transaction transaction) {
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

        po.___setProxiedObject(proxifiedObj);

        // the object is ready to be used - return it
        return proxifiedObj;
    }

}
