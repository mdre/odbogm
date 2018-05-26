/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;

@Entity
public class SimpleVertexWithEmbedded {
    private final static Logger LOGGER = Logger.getLogger(SimpleVertexWithEmbedded.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    // las siguientes listas se deben introducir como embebidas porque son de tipos básicos.
    List<String> stringlist = new ArrayList<>();
    Map<String,Integer> simplemap = new HashMap<String, Integer>();
    
//    @Embedded
//    List<SimpleVertex> svlist = new ArrayList<>();
//    @Embedded
//    Map<String,SimpleVertex> svmap = new HashMap<>();

    public SimpleVertexWithEmbedded() {
        stringlist.add("lst 1");
        stringlist.add("lst 2");
        stringlist.add("lst 3");
        
        simplemap.put("key 1",1);
        simplemap.put("key 2",3);
        simplemap.put("key 3",3);
        
//        svlist.add(new SimpleVertex("e1"));
//        svlist.add(new SimpleVertex("e2"));
//        svlist.add(new SimpleVertex("e3"));
//        
//        svmap.put("key 1", new SimpleVertex("m1"));
//        svmap.put("key 2", new SimpleVertex("m2"));
//        svmap.put("key 3", new SimpleVertex("m3"));
    }
    
    public void addToList() {
        stringlist.add("lst 4");
    }
    
    public void addToMap() {
        simplemap.put("key 4",4);
    }

    public List<String> getStringlist() {
        return stringlist;
    }

    public Map<String, Integer> getSimplemap() {
        return simplemap;
    }
    
}
