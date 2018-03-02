/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import Test.EdgeAttrib;
import Test.EnumTest;
import Test.SSimpleVertex;
import Test.SimpleVertex;
import Test.SimpleVertexEx;
import Test.SimpleVertexInterfaceAttr;
import Test.SimpleVertexWithEmbedded;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import net.odbogm.proxy.IObjectProxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import net.odbogm.agent.ITransparentDirtyDetector;
import net.odbogm.agent.TransparentDirtyDetectorAgent;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.security.*;
import net.odbogm.utils.DateHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author SShadow
 */
public class SessionManagerSObjectsTest {

    SessionManager sm;

    public SessionManagerSObjectsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
////        TransparentDirtyDetectorAgent.initialize("Test");
////        
////        System.out.println("1-----");
////        GroupSID gs = new GroupSID("dd", "uu");
////        System.out.println("2-----");
//        
//        System.out.println("Iniciando session manager...");
//        sm = new SessionManager("remote:localhost/Test", "root", "toor")
//                .setActivationStrategy(SessionManager.ActivationStrategy.ONMETHODACCESS);
//        
//        System.out.println("Begin");
//        this.sm.begin();
//        
//        System.out.println("fin setup.");
////        this.sm.setAuditOnUser("userAuditado");
//
//        // GroupSID todos los vértices 
////        this.sm.getGraphdb().command(new OCommandSQL("delete vertex V")).execute();
    }

    @After
    public void tearDown() {
//        sm.shutdown();
    }

    /**
     * Test security of SObjects
     */
//    @Test
    public void testSObjects() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("test security of SObjects");
        System.out.println("***************************************************************");

        // elminar los grupos
        this.sm.getGraphdb().command(new OCommandSQL("delete vertex GroupSID")).execute();

        // eliminar los usuarios
        this.sm.getGraphdb().command(new OCommandSQL("delete vertex UserSID")).execute();

        // eliminar los SSVertex
        this.sm.getGraphdb().command(new OCommandSQL("delete vertex SSimpleVertex")).execute();

        // crear los grupos y los usuarios.
        System.out.println("\n\n\nCreando los grupos ----------------------------------");
        GroupSID gna = new GroupSID("gna", "gna");
        GroupSID gr = new GroupSID("gr", "gr");
        GroupSID gw = new GroupSID("gw", "gw");
        System.out.println("\n\n\nGuardando los grupos ----------------------------------");

        GroupSID sgna = this.sm.store(gna);
        GroupSID sgr = this.sm.store(gr);
        GroupSID sgw = this.sm.store(gw);
        System.out.println("\n\n\nIniciando commit de grupos.............................");
        this.sm.commit();
        System.out.println("fin de grupos -----------------------------------------------\n\n\n");
        UserSID una = new UserSID("una", "una");
        UserSID ur = new UserSID("ur", "ur");
        UserSID uw = new UserSID("uw", "uw");
        UserSID urw = new UserSID("urw", "urw");

        una = this.sm.store(una);
        ur = this.sm.store(ur);
        uw = this.sm.store(uw);
        urw = this.sm.store(urw);

        this.sm.commit();

        una.addGroup(sgna);
        una.addGroup(sgr);

        ur.addGroup(sgr);

        uw.addGroup(sgw);

        urw.addGroup(sgw);
        urw.addGroup(sgr);

        this.sm.commit();

        //--------------------------------------------------------
        SSimpleVertex ssv = new SSimpleVertex();

        ssv = this.sm.store(ssv);
        this.sm.commit();

        String reg = ((IObjectProxy) ssv).___getRid();
        System.out.println("RID: " + reg);
//        SSimpleVertex rssv = this.sm.get(SSimpleVertex.class, reg);

        System.out.println("Agregando los acls...");
        ssv.setAcl(gna, new AccessRight().setRights(AccessRight.NOACCESS));
        ssv.setAcl(gr, new AccessRight().setRights(AccessRight.READ));
        ssv.setAcl(gw, new AccessRight().setRights(AccessRight.WRITE));

        this.sm.commit();

        this.sm.setLoggedInUser(una);
        System.out.println("Login UserNoAccess");
        SSimpleVertex ssvna = this.sm.get(SSimpleVertex.class, reg);
        System.out.println("State: " + ssvna.getSecurityState());
        assertTrue(ssvna.getSecurityState() == AccessRight.NOACCESS);

        System.out.println("Login UserRead");
        this.sm.setLoggedInUser(ur);
        SSimpleVertex ssvr = this.sm.get(SSimpleVertex.class, reg);
        System.out.println("State: " + ssvr.getSecurityState());
        assertTrue(ssvr.getSecurityState() == AccessRight.READ);

        this.sm.setLoggedInUser(uw);
        System.out.println("Login UserWrite");
        SSimpleVertex ssvw = this.sm.get(SSimpleVertex.class, reg);
        System.out.println("State: " + ssvw.getSecurityState());
        assertTrue(ssvw.getSecurityState() == AccessRight.WRITE);

    }


}
