package net.odbogm.proxy;

import asm.proxy.EasyProxy;
import asm.proxy.TypesCache;
import com.orientechnologies.orient.core.record.OElement;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static TypesCache typeCache = new TypesCache();

    public ObjectProxyFactory() {
    }

    public static <T> T create(T o, OElement oe, Transaction transaction) {
        // return cglibcreate((Class<T>)o.getClass(), oe, transaction);
        return epcreate((Class<T>) o.getClass(), oe, transaction);
    }

    public static <T> T create(Class<T> c, OElement ov, Transaction transaction) {
        // return cglibcreate(c, ov, transaction);
        return epcreate(c, ov, transaction);
    }

    /**
     * Devuelve un proxy a partir de una definición de clase.
     * @param <T>
     * @param c
     * @param ov
     * @return T instance
     */
    public static <T> T epcreate(Class<T> c, OElement ov, Transaction transaction ) {
        LOGGER.log(Level.FINEST, "create proxy for class: "+c+(c!=null?c.getName().toString():"NULL CLASS!!!!"));
        T po = null;
        try {
            
            // crear el proxy al que delegar las llamadas
            ObjectProxy bbi = new ObjectProxy(c,ov,transaction);
            
            // crear una instancia
            po = typeCache.findOrInsert(c, IObjectProxy.class, ()->{
                    Class clazz = new EasyProxy().getProxyClass(c, IObjectProxy.class);
                    return clazz;
                }).newInstance(bbi);

            bbi.___setProxiedObject(po);
            
            // clean possible dirtiness (because of actions in default constructor)
            bbi.___removeDirtyMark();
            
        
//            System.out.println("//Object Proxy =====================================================");
//            
//            for (Method declaredMethod : c.getDeclaredMethods()) {
//                System.out.println(": "+declaredMethod.getName()+" : "+declaredMethod.isSynthetic()+" : "+Arrays.toString(declaredMethod.getParameters()));
//            }
//            System.out.println("//-----------------------------------------------------");
//            for (Method declaredMethod : po.getClass().getDeclaredMethods()) {
//                System.out.println(": "+declaredMethod.getName()+" : "+declaredMethod.isSynthetic()+" : "+Arrays.toString(declaredMethod.getParameters()));
//            }
//            System.out.println("//=====================================================");
        
            
        } catch (InstantiationException | IllegalAccessException | SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ObjectProxyFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return po;
    }

}
