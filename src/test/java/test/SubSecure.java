package test;

import java.util.ArrayList;
import java.util.List;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Indirect;

/**
 *
 * @author jbertinetti
 */
@Entity
public class SubSecure {
    
    @Indirect(linkName = "Secure_subs")
    private Secure owner;
    
    public List<SimpleVertex> aList = new ArrayList<>();

    
    public SubSecure() {
    }

    public Secure getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return "If this is called during rollback, indirects get loaded again.";
    }
    
}
