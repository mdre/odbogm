package test;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import net.odbogm.annotations.Ignore;
import java.util.logging.Logger;
import net.odbogm.annotations.Audit;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.RID;
import net.odbogm.annotations.Sequence;
import net.odbogm.annotations.Version;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
@Audit(log = Audit.AuditType.DELETE)
public class SimpleVertex {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SimpleVertex.class .getName());

    @RID private String rid;
    @Version private int version = -1;
    private String uuid;
    private String s;
    public int i;
    private float f;
    private boolean b;
    private Date fecha;
    @Sequence(sequenceName = "test_sequence") private final Long serial = null;
    
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

    public Long getSerial() {
        return serial;
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
        this.uuid = UUID.randomUUID().toString();
    }

    public SimpleVertex(){
        this.s = "string";
        this.i = 1;
        this.f = 0.1f;
        this.b = true;
        this.oI = 100;
        this.oF = 1.1f;
        this.oB = true;
        this.uuid = UUID.randomUUID().toString();
    }
    
    public SimpleVertex(String s) {
        super();
        this.s = s;
    }
    
    public String getRid() {
        return rid;
    }

    public int getVersion() {
        return version;
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

//    @Override
//    public String toString() {
//        return this.s + " - " + this.b +  " - " + this.i +  " - " + this.f;
//    }

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

//    @Override
//    public int hashCode() {
//        int hash = 7;
//        return hash;
//    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleVertex other = (SimpleVertex) obj;
        return Objects.equals(this.uuid, other.uuid);
    }
    
}
