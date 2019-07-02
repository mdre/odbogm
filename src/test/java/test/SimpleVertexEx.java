package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.odbogm.annotations.Audit;
import net.odbogm.annotations.CascadeDelete;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.FieldAttributes;
import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.Indexed;
import net.odbogm.annotations.RemoveOrphan;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
@Audit(log = Audit.AuditType.ALL)
public class SimpleVertexEx extends SimpleVertex {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SimpleVertexEx.class .getName());

    @FieldAttributes(mandatory = FieldAttributes.Bool.TRUE)
    private String svex;

    private SimpleVertexEx looptest;
    
    public EnumTest enumTest;

    @Indexed(type = Indexed.IndexType.UNIQUE)
    private String svuuid;

    @RemoveOrphan
    public SimpleVertex svinner; 
    
    public ArrayList<String> alString;
    
    public HashMap<String,String> hmString;
    
    @RemoveOrphan
    @CascadeDelete
    public ArrayList<SimpleVertex> alSV;
    
    public List<SimpleVertex> lSV;
    
    public ArrayList<SimpleVertexEx> alSVE;
    
    @CascadeDelete
    public HashMap<String,SimpleVertex> hmSV;
    
    public HashMap<String ,SimpleVertexEx> hmSVE;
    
    public HashMap<EdgeAttrib,SimpleVertexEx> ohmSVE;
    
    public SimpleVertexEx(String svex, String s, int i, float f, boolean b, Integer oI, Float oF, Boolean oB) {
        super(s, i, f, b, oI, oF, oB);
        this.svex = svex;
        this.enumTest = EnumTest.UNO;
        this.svuuid = UUID.randomUUID().toString();
    }

    public SimpleVertexEx() {
        super();
        this.svex = "default";
        this.svuuid = UUID.randomUUID().toString();
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

    public void setSvex(String s) {
        this.svex = s;
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

    public String getUuid() {
        return this.svuuid;
    }

    public void setUuid(String uuid) {
        this.svuuid = uuid;
    }
        
}

