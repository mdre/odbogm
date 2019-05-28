package test;

import net.odbogm.annotations.Entity;
import net.odbogm.security.SObject;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class SSimpleVertex extends SObject {

    private String s;
    
    
    public SSimpleVertex(String s) {
        this.s = s;
    }

    public SSimpleVertex(){
        this.s = "string";
    }
    
    public String getS(){
        return this.s;
    }

    public void setS(String s) {
        this.s = s;
    }
    
    @Override
    public String toString() {
        return this.s + " - State: "+this.getSecurityState() ;
    }
}
