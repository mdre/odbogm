package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.OElement;
import static java.lang.ClassLoader.getSystemClassLoader;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.odbogm.LogginProperties;
import net.odbogm.Transaction;

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

    private static TypeCache<Class<?>> cache = new TypeCache<>(TypeCache.Sort.SOFT);
    public ObjectProxyFactory() {
    }
  

    public static <T> T create(T o, OElement oe, Transaction transaction) {
//        return cglibcreate((Class<T>)o.getClass(), oe, transaction);
        return bbcreate((Class<T>)o.getClass(), oe, transaction);
    }


    public static <T> T create(Class<T> c, OElement ov, Transaction transaction) {
//        return cglibcreate(c, ov, transaction);
        return bbcreate(c, ov, transaction);
    }


    /**
     * Devuelve un proxy a partir de una definición de clase.
     * @param <T>
     * @param c
     * @param ov
     * @param sm
     * @return 
     */
    public static <T> T bbcreate(Class<T> c, OElement ov, Transaction transaction ) {
        LOGGER.log(Level.FINEST, "create proxy for class: "+c+(c!=null?c.getName().toString():"NULL CLASS!!!!"));
        T po = null;
        try {
            Class<?> type = cache.findOrInsert(getSystemClassLoader(), c, () -> {
                                return
                                    new ByteBuddy(ClassFileVersion.ofThisVm())
                                        .subclass(c)
                                        .defineField("___ogm___interceptor", ObjectProxy.class, Visibility.PUBLIC)
                                        .implement(IObjectProxy.class)
                                        .defineConstructor(Visibility.PUBLIC)
                                            .withParameter(ObjectProxy.class)
                                            .intercept(FieldAccessor.ofField("___ogm___interceptor").setsArgumentAt(0)
                                                        .andThen(MethodCall.invoke(c.getDeclaredConstructor()))
                                            )
                                        .method(ElementMatchers.any()) // isDeclaredBy(ITest.class)
                                            .intercept(MethodDelegation   // This.class,Origin.class,AllArguments.class,SuperMethod.class)
                                                        .withDefaultConfiguration() //.withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS)
                                                        .filter(ElementMatchers.named("intercept"))
                                                        .toField("___ogm___interceptor"))   // MethodDelegation.to(bbi)
                                        .make()
                                        .load(getSystemClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                                        .getLoaded();
            });
            
            
            // crear el proxy al que delegar las llamadas
            ObjectProxy bbi = new ObjectProxy(c,ov,transaction);
            
            // crear una instancia
            po = (T)type.getConstructor(ObjectProxy.class).newInstance(bbi);
            
            bbi.___setProxiedObject(po);
            
            // clean possible dirtiness (because of actions in default constructor)
            bbi.___removeDirtyMark();
            
        } catch (InstantiationException | IllegalAccessException | SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return po;
    }
    
    // Implementación con CGLib
//    private static <T> T cglibcreate(Class<T> c, OElement oe, Transaction transaction) {
//        // this is the main cglib api entry-point
//        // this object will 'enhance' (in terms of CGLIB) with new capabilities
//        // one can treat this class as a 'Builder' for the dynamic proxy
//        Enhancer e = new Enhancer();
//
//        // the class will extend from the real class
//        e.setSuperclass(c);
//        // we have to declare the interceptor  - the class whose 'intercept'
//        // will be called when any method of the proxified object is called.
//        ObjectProxy po = new ObjectProxy(c, oe, transaction);
//        e.setCallback(po);
//        e.setInterfaces(new Class[]{IObjectProxy.class});
//
//        // now the enhancer is configured and we'll create the proxified object
//        T proxifiedObj = (T) e.create();
//
//        po.___setProxiedObject(proxifiedObj);
//        
//        // clean possible dirtiness (because of actions in default constructor)
//        po.___removeDirtyMark();
//
//        // the object is ready to be used - return it
//        return proxifiedObj;
//    }

}
