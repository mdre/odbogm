package net.odbogm;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Structure to support the values of an object.
 * It maps the name of a field with its value.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectStruct {

    /**
     * Map of basic attributes (persisted inside the vertex).
     */
    public HashMap<String, Object> fields = new HashMap<>();
    
    /**
     * Map of links to other objects (edges).
     */
    public HashMap<String, Object> links = new HashMap<>();
    
    /**
     * Map of lists of links to other objects.
     */
    public HashMap<String, Object> linkLists = new HashMap<>();
    
    /**
     * List of removed attributes of the vertex.
     */
    public ArrayList<String> removedProperties = new ArrayList<>();
}
