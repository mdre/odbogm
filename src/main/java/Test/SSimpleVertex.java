/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.odbogm.annotations.Ignore;
import java.util.logging.Logger;
import net.odbogm.annotations.Embedded;
import net.odbogm.security.SObject;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SSimpleVertex extends SObject {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SSimpleVertex.class .getName());

    private String rid;
    private String s;
    public int i;
    private float f;
    private boolean b;
    private Date fecha;

    //@Embedded
    private Map<String,Integer> map = new HashMap<>();
    
    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
    
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
    
    public SSimpleVertex(String s, int i, float f, boolean b, Integer oI, Float oF, Boolean oB) {
        this.s = s;
        this.i = i;
        this.f = f;
        this.b = b;
        this.oI = oI;
        this.oF = oF;
        this.oB = oB;
    }

    public SSimpleVertex(){
        this.s = "string";
        this.i = 1;
        this.f = 0.1f;
        this.b = true;
        this.oI = new Integer(100);
        this.oF = new Float(1.1);
        this.oB = new Boolean(true);
        
        this.map.put("key1", 1);
        this.map.put("key2", 2);
        this.map.put("key3", 3);
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

    public void setI(int i) {
        this.i = i;
    }

    public void setF(float f) {
        this.f = f;
    }

    public void setB(boolean b) {
        this.b = b;
    }

    public void setoI(Integer oI) {
        this.oI = oI;
    }

    public void setoF(Float oF) {
        this.oF = oF;
    }

    public void setoB(Boolean oB) {
        this.oB = oB;
    }
    
    
}
