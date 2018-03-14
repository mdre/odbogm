/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.agent;

import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class WriteAccessActivatorAdapter extends MethodVisitor implements ITransparentDirtyDetectorDef {

    private final static Logger LOGGER = Logger.getLogger(WriteAccessActivatorAdapter.class.getName());
    private boolean activate = false;
    private String owner;

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.WriteAccessActivatorAdapter);
        }
    }
    
    public WriteAccessActivatorAdapter(MethodVisitor mv) {
        super(Opcodes.ASM4, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        
        if ((this.activate)&&((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW)) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, SETDIRTY, "(Z)V", false);
//            mv.visitFieldInsn(Opcodes.PUTFIELD, owner, "__ogm__dirtyMark", "Z");
        } 
        mv.visitInsn(opcode);
        
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (opcode == Opcodes.PUTFIELD) {
            this.activate = true;
            this.owner = owner;
        } 
        mv.visitFieldInsn(opcode, owner, name, desc); 
    }
    
    
    
}

