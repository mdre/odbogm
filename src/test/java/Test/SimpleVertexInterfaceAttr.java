/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;

@Entity
public class SimpleVertexInterfaceAttr extends SimpleVertex {
    private final static Logger LOGGER = Logger.getLogger(SimpleVertexInterfaceAttr.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    public InterfaceTest itest;

    public SimpleVertexInterfaceAttr() {
        super();
        this.setS("simple vertex with interface attr");
        
        itest = new SimpleVertexWithImplement();
    }

    @Override
    public String toString() {
        return "SimpleVertexImplAtrr{" + "itest=" + itest + '}';
    }
    
}
