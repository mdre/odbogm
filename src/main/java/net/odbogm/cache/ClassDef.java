package net.odbogm.cache;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;

/**
 * Structure to support the definition of a class.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ClassDef {

    private final static Logger LOGGER = Logger.getLogger(ClassDef.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ClassDef);
        }
    }
    
    /**
     * Name of the entity (class of associated vertices).
     */
    public String entityName;
    
    /**
     * If the class or some attribute must be eagerly loaded.
     */
    public boolean isEager;
    
    /**
     * Field to be used to inject the RID if the appropriate annotation exists.
     */
    public Field ridField;
    
    /**
     * Field to be used to inject the version of the vertex/edge if the appropriate annotation exists.
     */
    public Field versionField;
    
    /**
     * Map of all Field objects (doesn't include the ridField). All fields have
     * already set the private access (setAccessible as true).
     */
    public HashMap<String, Field> fieldsObject = new HashMap<>();
    
    /**
     * Map of basic attributes (includes the embeddedFields)
     */
    public HashMap<String, Class<?>> fields = new HashMap<>();
    
    /**
     * Map of enum attributes.
     */
    public HashMap<String, Class<?>> enumFields = new HashMap<>();
    
    /**
     * Map of attributes that are collections of enums (only Lists).
     */
    public HashMap<String, Class<?>> enumCollectionFields = new HashMap<>();
    
    /**
     * Map of embedded List and embedded HashMap (collections of primitives and
     * fields annotated with @Embedded).
     */
    public HashMap<String, Class<?>> embeddedFields = new HashMap<>();
    
    /**
     * Map of links to other objects.
     */
    public HashMap<String, Class<?>> links = new HashMap<>();
    
    /**
     * Map of lists of links to other objects.
     */
    public HashMap<String, Class<?>> linkLists = new HashMap<>();
    
    /**
     * Map of indirect links. They are IN references to the current object.
     */
    public HashMap<String, Class<?>> indirectLinks = new HashMap<>();
    
    /**
     * Map of lists of indirect links. They are IN references to the current object.
     */
    public HashMap<String, Class<?>> indirectLinkLists = new HashMap<>();
    
    /**
     * Map of attributes that are assigned a value from a DB sequence (maps
     * attribute name → sequence name).
     */
    public HashMap<String, String> sequenceFields = new HashMap<>();


    @Override
    public String toString() {
        return "ClassDef{" + "entityName=" + entityName + '}';
    }
    
}
