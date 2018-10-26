/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.utils;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ArrayUtils {
    private final static Logger LOGGER = Logger.getLogger(ArrayUtils.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }
    
    public static String Array2String(List l) {
        Object s = l.stream().map(Object::toString).collect(Collectors.joining(", "));
        return s.toString();
    }
    
//    public static void main(String[] args) {
//        ArrayList l = new ArrayList();
//        l.add("Hola");
//        l.add(new Integer(5));
//        l.add(new Float(4.5));
//        
//        System.out.println(""+Array2String(l));
//    }
}
