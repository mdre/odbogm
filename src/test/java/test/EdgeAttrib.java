package test;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.RID;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class EdgeAttrib {
    private final static Logger LOGGER = Logger.getLogger(EdgeAttrib.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    @RID String rid;
    String nota;
    Date fecha;

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

    @Override
    public String toString() {
        return "EdgeAttrib{" + "nota=" + nota + ", fecha=" + fecha + "}";
    }

    public String getRid() {
        return rid;
    }
    
}
