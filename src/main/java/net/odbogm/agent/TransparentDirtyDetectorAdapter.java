/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.agent;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class TransparentDirtyDetectorAdapter extends ClassVisitor implements ITransparentDirtyDetectorDef {

    private final static Logger LOGGER = Logger.getLogger(TransparentDirtyDetectorAdapter.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.TransparentDirtyDetectorAdapter);
        }
    }
    
    private boolean isFieldPresent = false;
//    private boolean isInstrumetable = false;

    public TransparentDirtyDetectorAdapter(ClassVisitor cv) {
        super(Opcodes.ASM4, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String[] addInterfaces = Arrays.copyOf(interfaces, interfaces.length + 1); //create new array from old array and allocate one more element
        addInterfaces[addInterfaces.length - 1] = ITransparentDirtyDetector.class.getName().replace(".", "/");
        LOGGER.log(Level.FINER, "visitando clase: " + name + " super: " + superName + " y agregando la interface.");
        cv.visit(version, access, name, signature, superName, addInterfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (name.equals(DIRTYMARK)) {
            isFieldPresent = true;
        }
        return cv.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv;
        LOGGER.log(Level.FINEST, "visitando método: " + name);
        mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if ((mv != null) && !name.equals("<init>") && !name.equals("<clinit>") ) {
            LOGGER.log(Level.FINER, ">>>>>>>>>>> Instrumentando método: " + name);
            mv = new WriteAccessActivatorAdapter(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (!isFieldPresent) {
            LOGGER.log(Level.FINER, "Agregando el campo");
            FieldVisitor fv = cv.visitField(Opcodes.ACC_PUBLIC, DIRTYMARK, org.objectweb.asm.Type.BOOLEAN_TYPE.getDescriptor(), null, null);
            if (fv != null) {
                fv.visitEnd();
                LOGGER.log(Level.FINER, "fv.visitEnd..");
            }
        }
        cv.visitEnd();
    }

}
