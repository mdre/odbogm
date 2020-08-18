package test;

import java.util.ArrayList;
import java.util.List;
import net.odbogm.annotations.Entity;
import net.odbogm.security.SObject;

/**
 *
 * @author jbertinetti
 */
@Entity
public class Secure extends SObject {

    public List<SubSecure> subs = new ArrayList<>();
    
    private String s;

    
    public Secure() {
    }
    
    public Secure(String s) {
        this.s = s;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

}
