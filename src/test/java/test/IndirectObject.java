package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.DontLoadLinks;
import net.odbogm.annotations.Eager;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Indirect;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class IndirectObject {
    private final static Logger LOGGER = Logger.getLogger(IndirectObject.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }
    
    private String testData;
    
    private IndirectObject directLink;
    private ArrayList<IndirectObject> alDirectLinked = new ArrayList<>();
    private HashMap<String,IndirectObject> hmDirectLinked = new HashMap<>();
    
    
    @Indirect(linkName = "IndirectObject_directLink")
    private IndirectObject indirectLink;
    
    @Indirect(linkName = "IndirectObject_alDirectLinked")
    private IndirectObject indirectLinkedFromAL;
    
    @Indirect(linkName = "IndirectObject_alDirectLinked")
    private ArrayList<IndirectObject> alIndirectLinked = new ArrayList<>();
    
    @Indirect(linkName = "IndirectObject_hmDirectLinked")
    private HashMap<String,IndirectObject> hmIndirectLinked = new HashMap<>();
    
    @Eager
    @Indirect(linkName = "IndirectObject_directLink")
    private IndirectObject indirectEagerLink;
    

    public IndirectObject() {
    }
    
    public IndirectObject(String data) {
        this.testData = data;
    }
    
    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }
    
    public ArrayList<IndirectObject> getAlDirectLinked() {
        return alDirectLinked;
    }

    public void setAlDirectLinked(ArrayList<IndirectObject> alDirectLinked) {
        this.alDirectLinked = alDirectLinked;
    }

    public HashMap<String, IndirectObject> getHmDirectLinked() {
        return hmDirectLinked;
    }

    public void setHmDirectLinked(HashMap<String, IndirectObject> hmDirectLinked) {
        this.hmDirectLinked = hmDirectLinked;
    }

    public ArrayList<IndirectObject> getAlIndirectLinked() {
        return alIndirectLinked;
    }

    public void setAlIndirectLinked(ArrayList<IndirectObject> alIndirectLinked) {
        this.alIndirectLinked = alIndirectLinked;
    }

    public HashMap<String, IndirectObject> getHmIndirectLinked() {
        return hmIndirectLinked;
    }

    public void setHmIndirectLinked(HashMap<String, IndirectObject> hmIndirectLinked) {
        this.hmIndirectLinked = hmIndirectLinked;
    }
    
    public IndirectObject getDirectLink() {
        return directLink;
    }

    public void setDirectLink(IndirectObject directLink) {
        this.directLink = directLink;
    }

    public IndirectObject getIndirectLink() {
        return indirectLink;
    }

    public void setIndirectLink(IndirectObject indirectLink) {
        this.indirectLink = indirectLink;
    }

    public IndirectObject getIndirectLinkedFromAL() {
        return indirectLinkedFromAL;
    }

    @DontLoadLinks
    public IndirectObject getIndirectEagerLink() {
        return indirectEagerLink;
    }
    
    @DontLoadLinks
    public IndirectObject getIndirectDontLoad() {
        return indirectLink;
    }
    
}
