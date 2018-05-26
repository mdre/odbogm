/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Ignore;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class Foo {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(Foo.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private String text;
    private List<SimpleVertex> lsve;
    
    public Foo() {
    }

    public Foo(String text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public List<SimpleVertex> getLsve() {
        return lsve;
    }

    
    
}
