/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class TransparentDirtyDetectorInstrumentator implements ClassFileTransformer, TransparentDirtyDetectorDef {

    private final static Logger LOGGER = Logger.getLogger(TransparentDirtyDetectorInstrumentator.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.TransparentDirtyDetectorInstrumentator);
    }
    private String[] pkgs;

    public TransparentDirtyDetectorInstrumentator() {
        this.pkgs = TransparentDirtyDetectorAgent.pkgs;
    }

    public TransparentDirtyDetectorInstrumentator(String... _pkgs) {
        // agregar siempre net.odbogm.security porque los objetos del sistema
        // pueden extender a estas clases y es necesario que se activen como dirty 
        // cuando se invoque a algunos de sus métodos.
        this.pkgs = Arrays.copyOf(_pkgs, _pkgs.length + 1); //create new array from old array and allocate one more element
        this.pkgs[this.pkgs.length - 1] = "net.odbogm.security";
        
        LOGGER.log(Level.FINER, "Instrumentando clases de los siguientes paquetes: ");
        for (String pkg : this.pkgs) {
            LOGGER.log(Level.INFO, pkg);
            
        }
        
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
//        Class<T> taclazz = null;
        LOGGER.log(Level.FINEST, "preprocesando clase: {0}...", className);


        if (isInstrumentable(className)) {
            // forzar la recarga
//            clazz.getName().replace(".", "/")
            ClassReader cr = new ClassReader(classfileBuffer);
            if(isInterface(cr)){
                // No procesar las interfaces
                LOGGER.log(Level.FINER, "Interface detectada {0}. NO PROCESAR!",className);
                return classfileBuffer;
            }
            LOGGER.log(Level.FINER, "Redefiniendo on-the-fly {0}...",className);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            TransparentDirtyDetectorAdapter taa = new TransparentDirtyDetectorAdapter(cw);
            cr.accept(taa, 0);

            // instrumentar el método ___getDirty()
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, ISDIRTY, "()Z", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, DIRTYMARK, "Z");
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            // instrumentar el método ___setDirty()
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, SETDIRTY, "(Z)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, DIRTYMARK, "Z");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            writeToFile(className, cw.toByteArray());

            return cw.toByteArray();
        }
        return classfileBuffer;
    }

    private void writeToFile(String className, byte[] myByteArray) {
        try {
            File theDir = new File("/tmp/asm");
            if (!theDir.exists()) {
                theDir.mkdir();
            }
            
            FileOutputStream fos = new FileOutputStream("/tmp/asm/" + className.substring(className.lastIndexOf("/")) + ".class"); 
            fos.write(myByteArray);
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(TransparentDirtyDetectorInstrumentator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean isInstrumentable(String className) {
        boolean isIns = false;
        for (String pkg : pkgs) {
            if (className.startsWith(pkg.replace(".", "/")) && !className.contains("ByCGLIB")) {
                isIns = true;
            }
        }
        return isIns;
    }
    
    public boolean isInterface(ClassReader cr) {
        return ((cr.getAccess() & 0x200) != 0);
    }
}
