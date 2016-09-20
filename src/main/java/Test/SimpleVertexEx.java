/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.RemoveOrphan;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;
import net.odbogm.annotations.Audit;
import net.odbogm.annotations.FieldAttributes;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
@Audit(log = Audit.AuditType.ALL)
public class SimpleVertexEx extends SimpleVertex {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SimpleVertexEx.class .getName());

    @FieldAttributes(mandatory = FieldAttributes.Bool.TRUE)
    private String svex;

    private SimpleVertexEx looptest;
    
    public EnumTest enumTest;
    
    @RemoveOrphan
    private SimpleVertex svinner; 
    
    public ArrayList<String> alString;
    
    public HashMap<String,String> hmString;
    
    public ArrayList<SimpleVertex> alSV;
    
    public ArrayList<SimpleVertexEx> alSVE;
    
    public HashMap<String,SimpleVertex> hmSV;
    
    public HashMap<String ,SimpleVertexEx> hmSVE;
    
    public HashMap<EdgeAttrib,SimpleVertexEx> ohmSVE;
    
    public SimpleVertexEx(String svex, String s, int i, float f, boolean b, Integer oI, Float oF, Boolean oB) {
        super(s, i, f, b, oI, oF, oB);
        this.svex = svex;
        this.enumTest = EnumTest.UNO;
    }

    public SimpleVertexEx() {
        super();
        this.svex = "default";
    }
        
    public void initEnum() {
        this.enumTest = EnumTest.UNO;
    }

    public void initArrayListString() {
        this.alString = new ArrayList<>();
        this.alString.add("String 1");
        this.alString.add("String 2");
        this.alString.add("String 3");
    }
    
    public void initHashMapString() {
        this.hmString = new HashMap<>();
        this.hmString.put("hmString 1", "hmString 1");
        this.hmString.put("hmString 1", "hmString 2");
        this.hmString.put("hmString 1", "hmString 3");
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

    public HashMap<String, SimpleVertexEx> getHmSVE() {
        return hmSVE;
    }

    public void setHmSVE(HashMap<String, SimpleVertexEx> hmSVE) {
        this.hmSVE = hmSVE;
    }

    public HashMap<EdgeAttrib, SimpleVertexEx> getOhmSVE() {
        return ohmSVE;
    }

    public void setOhmSVE(HashMap<EdgeAttrib, SimpleVertexEx> ohmSVE) {
        this.ohmSVE = ohmSVE;
    }
    
    
    public void initInner() {
        this.svinner = new SimpleVertex();
        this.svinner.setS("sv inner");
    }
    
    public void testSVEXMethod() {
        System.out.println("in SVEx");
    }

    public void setSvinner(SimpleVertex svinner) {
        this.svinner = svinner;
    }

    public String getSvex() {
        return svex;
    }

    public EnumTest getEnumTest() {
        return enumTest;
    }

    public void setEnumTest(EnumTest e) {
        this.enumTest = e;
    }
    
    public SimpleVertex getSvinner() {
        return svinner;
    }

    public ArrayList<SimpleVertex> getAlSV() {
        return alSV;
    }

    public ArrayList<SimpleVertexEx> getAlSVE() {
        return alSVE;
    }

    public void setAlSVE(ArrayList<SimpleVertexEx> alSVE) {
        this.alSVE = alSVE;
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

class EdgeAttrib {
    String nota;
    Date fecha;

    public EdgeAttrib(String nota, Date fecha) {
        this.nota = nota;
        this.fecha = fecha;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
    
}