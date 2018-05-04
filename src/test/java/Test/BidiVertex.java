/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Bidirectional;
import net.odbogm.annotations.Entity;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class BidiVertex {
    private final static Logger LOGGER = Logger.getLogger(BidiVertex.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }
    
    @Bidirectional(name = "bidi_al")
    ArrayList<BidiVertex> bidilink = new ArrayList<>();
    private String name; 
    
    public BidiVertex() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<BidiVertex> getBidilink() {
        return bidilink;
    }

    public void setBidilink(ArrayList<BidiVertex> bidilink) {
        this.bidilink = bidilink;
    }
    
    
}
