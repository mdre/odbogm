/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import Test.BB.Ex2;
import net.odbogm.utils.ReflexionUtils;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class TestEx {
    private final static Logger LOGGER = Logger.getLogger(TestEx.class .getName());

    public TestEx() {
        Ex2 e2 = new Ex2();
        System.out.println("x: "+e2.x);
        System.out.println("Test: "+e2.test());
        System.out.println("inc: "+e2.inc());
        System.out.println("Test: "+e2.test());
    }
    
    public static void main(String[] args) {
        new TestEx();
    }
}
