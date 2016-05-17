/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import net.odbogm.annotations.Ignore;
import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class SimpleVertex {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SimpleVertex.class .getName());

    private String rid;
    private String s;
    public int i;
    private float f;
    private boolean b;

    public int getI() {
        return i;
    }

    public float getF() {
        return f;
    }

    public boolean isB() {
        return b;
    }

    public Integer getoI() {
        return oI;
    }

    public Float getoF() {
        return oF;
    }

    public Boolean getoB() {
        return oB;
    }
    private Integer oI;
    private Float oF;
    private Boolean oB;
    
    public SimpleVertex(String s, int i, float f, boolean b, Integer oI, Float oF, Boolean oB) {
        this.s = s;
        this.i = i;
        this.f = f;
        this.b = b;
        this.oI = oI;
        this.oF = oF;
        this.oB = oB;
    }

    public SimpleVertex(){
        this.s = "string";
        this.i = 1;
        this.f = 0.1f;
        this.b = true;
        this.oI = new Integer(100);
        this.oF = new Float(1.1);
        this.oB = new Boolean(true);
    }
    
    public Object getRid() {
        return rid;
    }
    
    public String getS(){
        return this.s;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public void setS(String s) {
        this.s = s;
    }
    
    public void testSVMethod() {
        System.out.println("in SV");
    }

    @Override
    public String toString() {
        return this.s + " - " + this.b +  " - " + this.i +  " - " + this.f;
    }
    
    
}
