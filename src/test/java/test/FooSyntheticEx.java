/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package test;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class FooSyntheticEx extends FooSynthetic {
    private final static Logger LOGGER = Logger.getLogger(FooSyntheticEx.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }

    public FooSyntheticEx() {
    }

    public void helloEx(String name) {
        System.out.println("Hello Ex "+name);
    }
    
}
