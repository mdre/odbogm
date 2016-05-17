/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.Link;
import net.odbogm.annotations.LinkList;
import net.odbogm.annotations.RemoveOrphan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class SimpleVertexEx extends SimpleVertex {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SimpleVertexEx.class .getName());
    private String svex;

    @Link
    private SimpleVertexEx looptest;
    
    public EnumTest enumTest;
    
    @RemoveOrphan
    @Link
    public SimpleVertex svinner; 
    
    @LinkList(name = "alSV")
    public ArrayList<SimpleVertex> alSV;
    
    @LinkList(name = "mapSV")
    public HashMap<String,SimpleVertex> hmSV;
    
    public SimpleVertexEx(String svex, String s, int i, float f, boolean b, Integer oI, Float oF, Boolean oB) {
        super(s, i, f, b, oI, oF, oB);
        this.svex = svex;
        this.enumTest = EnumTest.UNO;
    }

    public SimpleVertexEx() {
        super();
        this.svex = "deault";
    }
    
    
    public void initEnum() {
        this.enumTest = EnumTest.UNO;
    }

    public void initArrayList(){
        this.alSV = new ArrayList<SimpleVertex>();
        this.alSV.add(new SimpleVertex());
        this.alSV.add(new SimpleVertex());
        this.alSV.add(new SimpleVertex());
    }
    
    public void initHashMap() {
        this.hmSV = new HashMap<String, SimpleVertex>();
        SimpleVertex sv = new SimpleVertex();
        this.hmSV.put("key1", sv);
        this.hmSV.put("key2", sv);
        this.hmSV.put("key3", new SimpleVertex());
    }
    
    
    public void initInner() {
        this.svinner = new SimpleVertex();
        this.svinner.setS("sv inner");
    }
    
    public void testSVEXMethod() {
        System.out.println("in SVEx");
    }

    public String getSvex() {
        return svex;
    }

    public EnumTest getEnumTest() {
        return enumTest;
    }

    public SimpleVertex getSvinner() {
        return svinner;
    }

    public ArrayList<SimpleVertex> getAlSV() {
        return alSV;
    }

    public HashMap<String, SimpleVertex> getHmSV() {
        return hmSV;
    }

    public SimpleVertexEx getLooptest() {
        return looptest;
    }

    public void setLooptest(SimpleVertexEx looptest) {
        this.looptest = looptest;
    }

        
}
