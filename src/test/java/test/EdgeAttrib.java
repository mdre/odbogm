package test;

import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.RID;

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
    
    @RID String rid;
    String nota;
    Date fecha;
    EnumTest enumValue;

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

    public String getRid() {
        return rid;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.nota);
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
        if (!(obj instanceof EdgeAttrib)) {
            return false;
        }
        final EdgeAttrib other = (EdgeAttrib) obj;
        return Objects.equals(this.nota, other.nota);
    }
    
}
