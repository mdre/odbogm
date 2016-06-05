/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TestBB;

import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class Ex1 {
    private final static Logger LOGGER = Logger.getLogger(Ex1.class .getName());
    private int i = 0;
    public int x = 10;
    public Ex1() {
    }
    
    public int inc(){
        return ++i;
    }
    
    public int test() {
        return x + i;
    }
}
