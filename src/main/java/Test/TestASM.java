/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Test;

import Test.asm.ASMTarget;
import Test.asm.ASMTargetEx;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.SessionManager;
import net.odbogm.agent.ITransparentDirtyDetector;
import net.odbogm.agent.TransparentDirtyDetectorAgent;
import net.odbogm.security.GroupSID;
import net.odbogm.security.UserSID;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class TestASM {

    private final static Logger LOGGER = Logger.getLogger(TestASM.class.getName());

    static {
        LOGGER.setLevel(Level.INFO);
    }

//    static {
//        TransparentDirtyDetectorAgent.initialize("ar.gov.santafe.mpa.labmaven.asm.test");
//    }
    public TestASM() {
//        TransparentDirtyDetectorAgent.initialize("Test.asm");

        SessionManager sm = new SessionManager("remote:localhost/Test", "root", "toor")
                .setActivationStrategy(SessionManager.ActivationStrategy.CLASS_INSTRUMENTATION,"Test");
        
        System.out.println("1");
        ASMTarget asmti = new ASMTarget();

        System.out.println("2");
        System.out.println("Dirty: " + ((ITransparentDirtyDetector) asmti).___ogm___isDirty());

        System.out.println("3 - invocando ignore()");
        asmti.ignore();
        System.out.println("Dirty: " + ((ITransparentDirtyDetector) asmti).___ogm___isDirty());
        System.out.println("4 - invocando set()");
        asmti.set();
        System.out.println("Dirty: " + ((ITransparentDirtyDetector) asmti).___ogm___isDirty());

        System.out.println("5 - probando ASMTargetEx...");
        ASMTargetEx asmte = new ASMTargetEx();

        System.out.println("6 - invocando ignore()");
        asmte.ignore();
        System.out.println("Dirty: " + ((ITransparentDirtyDetector) asmte).___ogm___isDirty());

        System.out.println("7 - invocando set()");
        asmte.set();
        System.out.println("Dirty: " + ((ITransparentDirtyDetector) asmte).___ogm___isDirty());

        System.out.println("8 - invocando init()");
        asmte.init();
        System.out.println("Dirty: " + ((ITransparentDirtyDetector) asmte).___ogm___isDirty());

        System.out.println("9 - reseteando a false");
        ((ITransparentDirtyDetector) asmte).___ogm___setDirty(false);
        System.out.println("Ex Dirty: " + ((ITransparentDirtyDetector) asmte).___ogm___isDirty());
        System.out.println("orig Dirty: " + ((ITransparentDirtyDetector) (ASMTarget) asmte).___ogm___isDirty());

        GroupSID gs = new GroupSID("xxx", "xxx");
        System.out.println("g: " + gs.getName());
        UserSID us = new UserSID("ss", "ss");
        System.out.println("u: " + us.getUUID());
    }

    public static void main(String[] args) {
        new TestASM();
    }
}
