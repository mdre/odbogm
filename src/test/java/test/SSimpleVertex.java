/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import net.odbogm.annotations.Entity;
import net.odbogm.security.SObject;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class SSimpleVertex extends SObject {
//    @Ignore
//    private final static Logger LOGGER = Logger.getLogger(SSimpleVertex.class .getName());

    private String rid;
    private String s;
    
    
    public SSimpleVertex(String s) {
        this.s = s;
    }

    public SSimpleVertex(){
        this.s = "string";
    }
    
    public String getS(){
        return this.s;
    }

    public void setS(String s) {
        this.s = s;
    }
    
    @Override
    public String toString() {
        return this.s + " - State: "+this.getSecurityState() ;
    }
}
