package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.odbogm.annotations.Entity;

/**
 *
 * @author jbertinetti
 */
@Entity
public class Enums {
    
    private String description;
    
    private EnumTest theEnum;
    
    public List<EnumTest> enums = new ArrayList<>();
    
    public Map<EnumTest, String> enumToString = new HashMap<>();
    
    public Map<String, EnumTest> stringToEnum = new HashMap<>();
    
    public List<EnumTest> notInitializedEnums;


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
    
    public void nullEnums() {
        this.enums = null;
    }
    
}
