/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test.asm;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.agent.ITransparentDirtyDetector;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ASMTargetInstrumented implements ITransparentDirtyDetector {
    public boolean ___dirty = false;
    private String s = "ASM";
    private Integer iI = 100;
    private int i = 1;
    
    public ASMTargetInstrumented() {
        this.ignore();
        this.init();
    }
    
    private void ignore() {
        int i = 5;
        i++;
    }
    
    private void init() {
        this.s = "ASM field";
        this.iI = 1000;
        this.i = 10;
        
        int localI = 1;
        localI++;
        
        String otroS = "S";
        this.___dirty = true;
    }

    @Override
    public boolean ___ogm___isDirty() {
        return this.___dirty;
    }

    @Override
    public void ___ogm___setDirty(boolean b) {
        this.___dirty = b;
    }

}
