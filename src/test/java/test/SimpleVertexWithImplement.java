/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.util.logging.Logger;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Ignore;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class SimpleVertexWithImplement extends SimpleVertex implements InterfaceTest {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(SimpleVertexWithImplement.class .getName());
    
    public SimpleVertexWithImplement(String s, int i, float f, boolean b, Integer oI, Float oF, Boolean oB) {
        super(s, i, f, b, oI, oF, oB);
    }

    public SimpleVertexWithImplement() {
    }
    
    public SimpleVertexWithImplement(String text){
        super();
        this.setS("SV with interface implementation");
    }
    
    @Override
    public void foo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
