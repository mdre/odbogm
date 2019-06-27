package test;

import java.util.List;
import net.odbogm.annotations.Entity;

/**
 *
 * @author jbertinetti
 */
@Entity
public class Enums {
    
    private String description;
    
    private EnumTest theEnum;
    
    public List<EnumTest> enums;


    public Enums() {
    }

    public Enums(String desc, EnumTest theEnum) {
        this.description = desc;
        this.theEnum = theEnum;
    }

    public String getDesc() {
        return description;
    }

    public void setDesc(String desc) {
        this.description = desc;
    }

    public EnumTest getTheEnum() {
        return theEnum;
    }

    public void setTheEnum(EnumTest theEnum) {
        this.theEnum = theEnum;
    }
    
}
