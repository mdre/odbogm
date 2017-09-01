/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
//import net.bytebuddy.implementation.bind.annotation.Origin;
//import net.bytebuddy.implementation.bind.annotation.RuntimeType;
//import net.bytebuddy.implementation.bind.annotation.SuperCall;
//import net.bytebuddy.implementation.bind.annotation.This;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class BBGeneralInterceptor {

    private final static Logger LOGGER = Logger.getLogger(BBGeneralInterceptor.class.getName());
    private String testData;
            
//    @RuntimeType
//    public Object intercept(@SuperCall Callable<?> zuper, @This Object thiz, @Origin Method method) throws Exception {
//        // intercept any method of any signature
//        if (!method.getName().equals("toString")) 
//            System.out.println("intercepted call:"+thiz);
//        Object res = null;
//        switch (method.getName()) {
//            case "setData":
//                this.setData(testData);
//                break;
//            case "getData":
//                res = this.getData();
//                break;
//            default:
//                res = zuper.call();
//        }
//        return res;
//    }
//    
//    @RuntimeType
//    public void setData(String s) {
//        this.testData = s;
//    }
//
//    @RuntimeType
//    public String getData() {
//        return this.testData;
//    }
}
