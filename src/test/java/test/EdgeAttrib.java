package test;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.RID;
import net.odbogm.annotations.Sequence;
import net.odbogm.annotations.Version;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity(isEdgeClass = true)
public class EdgeAttrib {
    private final static Logger LOGGER = Logger.getLogger(EdgeAttrib.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    @RID private String rid;
    @Version private Integer version = -1;
    private final String uuid = UUID.randomUUID().toString();
    private String nota;
    private Date fecha;
    private EnumTest enumValue;
    @Sequence(sequenceName = "test_sequence") private Long serial;

    public EdgeAttrib() {
    }

    
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

    public EnumTest getEnumValue() {
        return enumValue;
    }

    public void setEnumValue(EnumTest enumValue) {
        this.enumValue = enumValue;
    }

    @Override
    public String toString() {
        return "EdgeAttrib{" + "nota=" + nota + ", fecha=" + fecha + "}";
    }

    public Integer getVersion() {
        return version;
    }

    public String getRid() {
        return rid;
    }

    public Long getSerial() {
        return serial;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.uuid);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!EdgeAttrib.class.isInstance(obj)) {
            return false;
        }
        final EdgeAttrib other = (EdgeAttrib) obj;
        return Objects.equals(this.uuid, other.uuid);
    }
    
}
