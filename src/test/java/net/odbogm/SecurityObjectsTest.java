package net.odbogm;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import net.odbogm.exceptions.CircularReferenceException;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.security.AccessRight;
import net.odbogm.security.GroupSID;
import net.odbogm.security.SObject;
import net.odbogm.security.UserSID;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import test.SSimpleVertex;
import test.TestConfig;

/**
 *
 * @author jbertinetti
 */
public class SecurityObjectsTest {

    private SessionManager sm;

    @Before
    public void setUp() {
        sm = new SessionManager(TestConfig.TESTDB, "admin", "nimda")
                    //.setClassLevelLog(TransparentDirtyDetectorInstrumentator.class, Level.INFO)
                    //.setClassLevelLog(InstrumentableClassDetector.class, Level.INFO)
                    //.setClassLevelLog(ClassCache.class, Level.FINER)
                ;
        sm.begin();
    }

    @After
    public void tearDown() {
        sm.shutdown();
    }
    
    /**
     * Test security of SObjects
     */
    @Test
    public void testSObjects() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("test security of SObjects");
        System.out.println("***************************************************************");

        // elminar los grupos
        ODatabaseSession db = sm.getDBTx();
        
        db.command(new OCommandSQL("delete vertex GroupSID")).execute();

        // eliminar los usuarios
        db.command(new OCommandSQL("delete vertex UserSID")).execute();

        // eliminar los SSVertex
        db.command(new OCommandSQL("delete vertex SSimpleVertex")).execute();
        
        db.close();

        // crear los grupos y los usuarios.
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n");
        System.out.println("Creando los grupos ----------------------------------");
        
        GroupSID gna = new GroupSID("gna", "gna");
        GroupSID gr = new GroupSID("gr", "gr");
        GroupSID gw = new GroupSID("gw", "gw");
        GroupSID go = new GroupSID("go", "go"); //other
        System.out.println("CL group: " + gna.getClass().getClassLoader() + " > " + gna.getClass().getCanonicalName());
        System.out.println("\n\n\nGuardando los grupos ----------------------------------");

        GroupSID sgna = this.sm.store(gna);
        GroupSID sgr = this.sm.store(gr);
        GroupSID sgw = this.sm.store(gw);
        GroupSID sgo = this.sm.store(go);

        // liberar las referencias
        gna = null;
        gr = null;
        gw = null;
        go = null;

        System.out.println("\n\n\nIniciando commit de grupos.............................");
        this.sm.commit();
        System.out.println("fin de grupos -----------------------------------------------\n\n\n");

        System.out.println("\n\n\nCreando usuarios ----------------------------------");
        UserSID una = new UserSID("una", "una");
        UserSID ur = new UserSID("ur", "ur");
        UserSID uw = new UserSID("uw", "uw");
        UserSID urw = new UserSID("urw", "urw");
        UserSID uo = new UserSID("uo", "uo");
        System.out.println("CL user: " + una.getClass().getClassLoader());

        una = this.sm.store(una);
        ur = this.sm.store(ur);
        uw = this.sm.store(uw);
        urw = this.sm.store(urw);
        uo = this.sm.store(uo);

        this.sm.commit();

        una.addGroup(sgna);
        una.addGroup(sgr);

        ur.addGroup(sgr);

        uw.addGroup(sgw);
        uw.addGroup(sgo);

        urw.addGroup(sgw);
        urw.addGroup(sgr);

        this.sm.commit();
        String sgnaRid = sm.getRID(sgna);
        System.out.println("ID de sgna: " + sgnaRid);

        //--------------------------------------------------------
        SSimpleVertex ssv = new SSimpleVertex();

        ssv = this.sm.store(ssv);
        this.sm.commit();

        String reg = ((IObjectProxy) ssv).___getRid();
        System.out.println("RID: " + reg);
        SSimpleVertex rssv = this.sm.get(SSimpleVertex.class, reg);

        System.out.println("SecurityState: " + rssv.getSecurityState());

        System.out.println("Agregando los acls...");
        rssv.setOwner(uw);
        rssv.setAcl(sgna, new AccessRight().setRights(AccessRight.NOACCESS));
        rssv.setAcl(sgr, new AccessRight().setRights(AccessRight.READ));
        rssv.setAcl(sgw, new AccessRight().setRights(AccessRight.WRITE));

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
        
        this.sm.setLoggedInUser(uo);
        System.out.println("Login UserOther");
        SSimpleVertex ssvo = this.sm.get(SSimpleVertex.class, reg);
        System.out.println("State: " + ssvo.getSecurityState());
        //default AccessRight for Others: READ
        assertTrue(ssvo.getSecurityState() == SObject.OTHERS_DEFAULT_ACCESS);

        rssv.removeAcl(sgna);
        sm.commit();

        // probar la eliminación de grupos.
        String unaRID = sm.getRID(una);
        una = null;
        sm.delete(sgna);
        sm.commit();
        assertThrows(UnknownRID.class, () -> sm.get(GroupSID.class, sgnaRid));

        sm.getCurrentTransaction().removeFromCache(unaRID);
        una = sm.get(UserSID.class, unaRID);

        assertEquals(1, una.getGroups().size());

        // probar remover una 
        String urwRID = sm.getRID(urw);
        urw.removeGroup(sgw);
        sm.commit();
        urw = sm.get(UserSID.class, urwRID);

        assertEquals(urw.getGroups().size(), 1);

        // Verificar la transitividad de los grupos.
        // la idea es que un usuario U1 es agregado al grupo g1
        // g1 es agregado a g2
        // g2 es agregado a g3
        // finalmente se agrega un ACL para g3 
        // y el usuario U1 debería poder acceder al SObject por transitividad.
        System.out.println("Preparando las entidads para probar la transitividad de permisos...");
        GroupSID g1 = new GroupSID("g1", "g1");
        GroupSID sg1 = sm.store(g1);
        GroupSID g2 = new GroupSID("g2", "g2");
        GroupSID sg2 = sm.store(g2);
        GroupSID g3 = new GroupSID("g3", "g3");
        GroupSID sg3 = sm.store(g3);
        UserSID u1 = new UserSID("u1", "u1");
        UserSID su1 = sm.store(u1);
        sm.commit();

        System.out.println("creando la transitividad...");
//        su1.addGroup(sg1);
        sg1.add(su1);
        sg2.add(sg1);
        sg3.add(sg2);
        sm.commit();

        System.out.println("logueando el usuario...");
        sm.setLoggedInUser(su1);

        System.out.println("creando el objeto...");

        SSimpleVertex testTransitivity = new SSimpleVertex();
        testTransitivity.setAcl(g3, new AccessRight().setRights(AccessRight.WRITE));
        SSimpleVertex stt = sm.store(testTransitivity);
        sm.commit();

        String sttRID = sm.getRID(stt);
        System.out.println("SObject Transitivity RID: " + sttRID);
        stt = null;

        System.out.println("refrescar los grupos");
        String sg1RID = sm.getRID(sg1);
        String sg2RID = sm.getRID(sg2);
        String sg3RID = sm.getRID(sg3);
        sg3 = sm.get(GroupSID.class, sg3RID);
        sg2 = sm.get(GroupSID.class, sg2RID);
        sg1 = sm.get(GroupSID.class, sg1RID);

        String su1RID = sm.getRID(su1);
        System.out.println("refrescar el usuario " + su1RID + "...");
        su1 = sm.get(UserSID.class, su1RID);
        sm.setLoggedInUser(su1);

        System.out.println("verificando los permisos...");
        for (String showSecurityCredential : su1.showSecurityCredentials()) {
            System.out.println(":: " + showSecurityCredential);
        }

        SSimpleVertex rtt = sm.get(SSimpleVertex.class, sttRID);
        int se = rtt.getSecurityState();
        System.out.println("Security state: " + se);
        assertTrue(se > 0);
    }
    
    @Test
    public void securityOnUser() throws Exception {
        String uuid = RandomStringUtils.randomAlphanumeric(10);
        UserSID user = new UserSID(uuid, uuid);
        user = sm.store(user);
        sm.commit();
        
        SSimpleVertex ssv = new SSimpleVertex("access");
        ssv.setAcl(user, new AccessRight(AccessRight.ACCESSCONTROL));
        sm.store(ssv);
        ssv = new SSimpleVertex("no access");
        ssv.setAcl(user, new AccessRight(AccessRight.NOACCESS));
        sm.store(ssv);
        sm.commit();
        
        String rid = sm.getRID(user);
        user = sm.get(UserSID.class, rid);
        sm.setLoggedInUser(user);
        
        int res = 0;
        for (var v : sm.query(SSimpleVertex.class)) {
            if (v.getSecurityState() == AccessRight.ACCESSCONTROL) res++;
        }
        assertEquals(1, res);
    }
    
    @Test
    public void others() throws Exception {
        //2 usuarios, logueado el user1
        String uid1 = RandomStringUtils.randomAlphanumeric(10);
        UserSID user1 = sm.store(new UserSID(uid1, uid1));
        String uid2 = RandomStringUtils.randomAlphanumeric(10);
        UserSID user2 = sm.store(new UserSID(uid2, uid2));
        
        SSimpleVertex so = sm.store(new SSimpleVertex());
        sm.commit();
        sm.setLoggedInUser(user1);
        String rid = sm.getRID(so);
        
        //sobject sin acl definidos -> full access
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(AccessRight.FULLACCESS, so.getSecurityState());
        
        //si le agrego algún acl, se valida con OTRO -> para user1 queda el
        //acceso por defecto para OTRO: READ
        assertEquals(SObject.OTHERS_DEFAULT_ACCESS, AccessRight.READ);
        so.setAcl(user2, new AccessRight(AccessRight.WRITE));
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(SObject.OTHERS_DEFAULT_ACCESS, so.getSecurityState());
        
        //cambio el acl para OTRO
        so.setOthersAcl(new AccessRight(AccessRight.LIST));
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(AccessRight.LIST, so.getSecurityState());
        
        //vuelvo al acl por defecto para OTRO
        so.removeOthersAcl();
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(SObject.OTHERS_DEFAULT_ACCESS, so.getSecurityState());
        
        //limpio acls -> vuelve full access
        so.removeAcl(user2);
        assertTrue(so.getAcls().isEmpty());
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(AccessRight.FULLACCESS, so.getSecurityState());
        
        //sólo dejo acl definido para OTRO
        so.setOthersAcl(new AccessRight(AccessRight.DELETE));
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(AccessRight.DELETE, so.getSecurityState());
        
        //user2 es OTRO ahora
        sm.setLoggedInUser(user2);
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(AccessRight.DELETE, so.getSecurityState());
        
        //vuelvo al acl por defecto para OTRO
        so.removeOthersAcl();
        so.setAcl(user1, new AccessRight(AccessRight.NOACCESS));
        so = sm.get(SSimpleVertex.class, rid);
        assertEquals(SObject.OTHERS_DEFAULT_ACCESS, so.getSecurityState());
    }
    
    @Test
    public void avoidGroupsLoop() throws Exception {
        System.out.println("\n\n\n\n\n\n\n\n\n");
        System.out.println("************************************************************");
        System.out.println("avoidGroupsLoop");
        System.out.println("************************************************************");
        System.out.println("\n\n\n\n\n\n\n\n\n");
        GroupSID g1 = sm.store(new GroupSID("g1", "g1"));
        GroupSID g2 = sm.store(new GroupSID("g2", "g2"));
        GroupSID g3 = sm.store(new GroupSID("g3", "g3"));
        
        //this must not happen:
        //g1 -> g2 -> g3 -> g1
        g1.add(g2);
        g2.add(g3);
        assertThrows(CircularReferenceException.class, () -> g3.add(g1));
        
        //this neither:
        assertThrows(CircularReferenceException.class, () -> g1.add(g1));
    }
    
}
