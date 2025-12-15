package test;

import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Sequence;

/**
 *
 * @author jbertinetti
 */
@Entity
public class Serial {
    
    @Sequence(sequenceName = "test_sequence")
    public Long s1;
    
    @Sequence(sequenceName = "test_sequence")
    public Long s2;


    public void setS1(Long l) {
        this.s1 = l;
    }
    public Serial() {
    }
    
}
