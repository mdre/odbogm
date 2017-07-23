/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TestBB;

import net.odbogm.proxy.BBGeneralInterceptor;
import Test.SimpleVertexEx;
import net.odbogm.utils.ReflectionUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class TestBB {

    private final static Logger LOGGER = Logger.getLogger(TestBB.class.getName());

//    enum ToStringAssigner implements Assigner {
//
//        INSTANCE; // singleton
//
//        @Override
//        public StackManipulation assign(TypeDescription.Generic source,
//                TypeDescription.Generic target,
//                Assigner.Typing typing) {
//            if (!source.isPrimitive() && target.represents(String.class)) {
//                MethodDescription toStringMethod = new TypeDescription.ForLoadedType(Object.class)
//                        .getDeclaredMethods()
//                        .filter(named("toString"))
//                        .getOnly();
//                return MethodInvocation.invoke(toStringMethod).virtual(sourceType);
//            } else {
//                return StackManipulation.Illegal.INSTANCE;
//            }
//        }
//    }
    public static void main(String[] args) {
        new TestBB();
    }

    public TestBB() {
        try {
//            System.out.println("install agent...");
//            ByteBuddyAgent.install();
//            System.out.println("Retransform: " + ByteBuddyAgent.getInstrumentation().isRedefineClassesSupported());
//            System.out.println("Create dynamic type....");
//            DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
//                    .redefine(SimpleVertexEx.class)
//                    .defineField("__BB__Dirty", boolean.class, Visibility.PUBLIC)
//                    .make()
//                    .load(getClass().getClassLoader(), ClassReloadingStrategy.Default.CHILD_FIRST);
//                    .getLoaded();

            System.out.println("create instance....");

//            SimpleVertexEx sve = new ByteBuddy()
//                    .sub(SimpleVertexEx.class)
//                    .defineField("__BB__Dirty", boolean.class, Visibility.PUBLIC)
//                    .make()
//                    .load(getClass().getClassLoader(), ClassReloadingStrategy.Default.CHILD_FIRST)
//                    .getLoaded().newInstance();
            SimpleVertexEx sve = new SimpleVertexEx();
            sve.initInner();
            
            BBGeneralInterceptor bbi = new BBGeneralInterceptor();
            SimpleVertexEx proxied1 = new ByteBuddy()
                            .subclass(SimpleVertexEx.class)
                    .implement(ITest.class)
                        .method(isDeclaredBy(ITest.class))
                        .intercept(MethodDelegation.to(bbi))
                    .make()
                    .load(getClass().getClassLoader(), ClassReloadingStrategy.Default.INJECTION)
                    .getLoaded().newInstance();
            
            // copiar todo 
            ReflectionUtils.copyObject(sve, proxied1);

            System.out.println("" + proxied1.getClass().getName());
            System.out.println("" + proxied1.getClass().getSuperclass().getName());
            System.out.println("" + proxied1.getS());
            System.out.println("" + proxied1.getSvex().toString());
            System.out.println("" + proxied1.getSvinner().toString());
            bbi = new BBGeneralInterceptor();
            SimpleVertexEx proxied2 = new ByteBuddy()
                            .subclass(SimpleVertexEx.class)
                    .implement(ITest.class)
                        .method(isDeclaredBy(ITest.class))
                        .intercept(MethodDelegation.to(bbi))
                    .make()
                    .load(getClass().getClassLoader(), ClassReloadingStrategy.Default.INJECTION)
                    .getLoaded().newInstance();
            
            // copiar todo 
            ReflectionUtils.copyObject(sve, proxied2);
            ((ITest)proxied1).setData("test1");
            ((ITest)proxied2).setData("test2");
            System.out.println("Class: "+proxied1.getClass().getSimpleName());
            System.out.println("Class: "+proxied1.getClass().getSuperclass().getSimpleName());
            System.out.println("1: "+((ITest)proxied1).getData());
            System.out.println("2: "+((ITest)proxied2).getData());
            System.out.println(" FIN");
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TestBB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(TestBB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    

}

