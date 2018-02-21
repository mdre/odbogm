/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test.asm;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ASMTarget {
    private String s = "ASM";
    private Integer iI = 100;
    private int i = 1;
    private boolean b = false;
    
    public ASMTarget() {
        this.i = 2;
    }
    
    public void ignore() {
        int i = 5;
        i++;
    }
    
    public void set() {
        this.s = "ASM field";
        this.iI = 1000;
        this.i = 10;
        this.b = true;
        
        int localI = 1;
        localI++;
        
        String otroS = "S";
        
    }
}
