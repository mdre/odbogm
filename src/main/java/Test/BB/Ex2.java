/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test.BB;

import Test.BB.Ex1;
import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class Ex2 extends Ex1 {
    private final static Logger LOGGER = Logger.getLogger(Ex2.class .getName());
    
    private Ex1 inner;
    private String ex2String;
    
    
    public Ex2() {
        inner = new Ex1();
        ex2String = "hola mundo";
    }

    public Ex1 getInner() {
        return inner;
    }

    public void setInner(Ex1 inner) {
        this.inner = inner;
    }

    public String getEx2String() {
        return ex2String;
    }

    public void setEx2String(String ex2String) {
        this.ex2String = ex2String;
    }
    
}
