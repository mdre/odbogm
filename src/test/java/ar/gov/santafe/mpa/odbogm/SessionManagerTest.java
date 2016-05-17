/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ar.gov.santafe.mpa.odbogm;

import net.odbogm.SessionManager;
import Test.SimpleVertex;
import Test.SimpleVertexEx;
import net.odbogm.proxy.IObjectProxy;
import com.orientechnologies.orient.core.sql.OCommandSQL;
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
public class SessionManagerTest {
    SessionManager sm;
    
    public SessionManagerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        sm = new SessionManager("remote:localhost/Test", "root", "toor");
        this.sm.begin();
        // borrar todos los vértices 
//        this.sm.getGraphdb().command(new OCommandSQL("delete vertex V")).execute();
        
    }
    
    @After
    public void tearDown() {
        sm.shutdown();
    }

//    /**
//     * Test of begin method, of class SessionManager.
//     */
//    //@Test
//    public void testBegin() {
//        System.out.println("begin");
//        SessionManager instance = null;
//        instance.begin();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of store method, of class SessionManager.
     */
    //@Test
    public void testStoreSimple() {
        
        System.out.println("store objeto simple (SimpleVertex)");
        
        SimpleVertex sv = new SimpleVertex();
        SimpleVertex expResult = sv;
        
        SimpleVertex result = sm.store(sv);
        
        assertEquals(expResult.i, result.i);
        assertTrue(result instanceof IObjectProxy);
        
        // TODO review the generated test code and remove the default call to fail.
        this.sm.commit();
        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy)result).___getRid();
        expResult = this.sm.get(SimpleVertex.class, rid);
        
        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);
        
        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
        
        assertEquals(expResult.getI(), sv.getI());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals(expResult.getS(), sv.getS());
        assertEquals(expResult.getoB(), sv.getoB());
        assertEquals(expResult.getoF(), sv.getoF());
        assertEquals(expResult.getoI(), sv.getoI());
    }

    
    //@Test
    public void testStoreExtendedObject() {
        
        System.out.println("store objeto Extendido (SimpleVertexEx)");
        
        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx result = this.sm.store(sve);
        
        assertTrue(result instanceof IObjectProxy);
        
        // TODO review the generated test code and remove the default call to fail.
        this.sm.commit();
        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy)result).___getRid();
        
        SimpleVertexEx expResult = this.sm.get(SimpleVertexEx.class, rid);
        
        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);
        
        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
        
        assertEquals(expResult.getI(), sve.getI());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals(expResult.getS(), sve.getS());
        assertEquals(expResult.getoB(), sve.getoB());
        assertEquals(expResult.getoF(), sve.getoF());
        assertEquals(expResult.getoI(), sve.getoI());
        assertEquals(expResult.getSvex(), sve.getSvex());

        assertEquals(expResult.getSvinner(), sve.getSvinner());
        assertEquals(expResult.getEnumTest(), sve.getEnumTest());
        assertEquals(expResult.getHmSV(), sve.getHmSV());
        assertEquals(expResult.getAlSV(), sve.getAlSV());
        
        System.out.println("============================= FIN testStoreExtendedObject ===============================");
    }
    
    //@Test
    public void testStoreFullObject() {
        System.out.println("Verificar un objecto completamente inicializado y almacenado.");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initInner();
        sve.initEnum();
        sve.initArrayList();
        sve.initHashMap();
        
        System.out.println("sve pre store: "+sve.getSvinner().getS());
        
        SimpleVertexEx result = this.sm.store(sve);
        
        System.out.println("sve post store: "+sve.getSvinner().getS());
        
        sm.commit();
        
        System.out.println("sve post commit: "+sve.getSvinner().getS());
        
        System.out.println("");
        String rid = ((IObjectProxy)result).___getRid();
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: "+rid);
        System.out.println("");
        System.out.println("");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        
        System.out.println("");
        System.out.println("");
        System.out.println(" get completado. Iniciando los asserts");
        System.out.println("");
        System.out.println("");
        
        
        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
        
        assertEquals(expResult.getI(), sve.getI());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals(expResult.getS(), sve.getS());
        assertEquals(expResult.getoB(), sve.getoB());
        assertEquals(expResult.getoF(), sve.getoF());
        assertEquals(expResult.getoI(), sve.getoI());
        assertEquals(expResult.getSvex(), sve.getSvex());
        assertEquals(expResult.getEnumTest(), sve.getEnumTest());

        System.out.println("sve: "+sve.getSvinner().getS());
        System.out.println("expResult: "+expResult.getSvinner().getS());
        
        assertEquals(expResult.getSvinner().getS(), sve.getSvinner().getS());
        assertEquals(expResult.getAlSV().size(), sve.getAlSV().size());
        assertEquals(expResult.getHmSV().size(), sve.getHmSV().size());
        assertEquals(expResult.getEnumTest(), sve.getEnumTest());
        
        System.out.println("============================= FIN testStoreFullObject ===============================");
    }
    
    //@Test
    public void testStoreLink() {
        
        System.out.println("store objeto sin Link y luego se le agrega uno");
        
        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx result = this.sm.store(sve);
        this.sm.commit();
        System.out.println("=========== fin primer commit ====================================");
        
        assertEquals(result.getSvinner(), sve.getSvinner());
        
        // actualizar el objeto administrado
        result.initInner();
        System.out.println("result.svinner: "+result.getSvinner().getS()+ "      toS: "+result.getSvinner().toString());
        // bajarlo a la base
        System.out.println("=========== inicio segundo commit <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sm.commit();
        System.out.println("=========== fin segundo commit ====================================");
        System.out.println("result.svinner: "+result.getSvinner().getS()+ "      toS: "+result.getSvinner().toString());
        
        // recuperar el objeto en otra instancia
        String rid = ((IObjectProxy)result).___getRid();
        
        System.out.println("============================================================================");
        System.out.println("RID: "+rid);
        System.out.println("============================================================================");
        
        
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("========= comienzo del get =================================================");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        System.out.println("========= fin del get =================================================");
        
        assertEquals(((IObjectProxy)expResult).___getRid(), rid);
        
        System.out.println("++++++++++++++++ result: "+result.getSvinner().toString());
        System.out.println("++++++++++++++++ expResult: "+expResult.getSvinner().toString());
        
        assertEquals(expResult.getSvinner().getI(), result.getSvinner().getI());
        assertEquals(expResult.getSvinner().getS(), result.getSvinner().getS());
        assertEquals(expResult.getSvinner().getoB(), result.getSvinner().getoB());
        assertEquals(expResult.getSvinner().getoF(), result.getSvinner().getoF());
        assertEquals(expResult.getSvinner().getoI(), result.getSvinner().getoI());
    }
    
    
    
    //@Test
    public void testUpdateObject() {
        System.out.println("Verificación de update de un objeto administrado.");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initInner();
        sve.initEnum();
        sve.initArrayList();
        sve.initHashMap();
        
        SimpleVertexEx result = this.sm.store(sve);
        
        sm.commit();
        
        String rid = ((IObjectProxy)result).___getRid();
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: "+rid);
        System.out.println("");
        System.out.println("");
        
        result.i++;
        result.getAlSV().add(new SimpleVertex());
        
        sm.commit();
        
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        
        assertEquals(expResult.i, result.i);
        assertEquals(expResult.alSV.size(), result.alSV.size());
        System.out.println("Fin Update!.............");
    }
    
    
    
    @Test
    public void testLoop(){
        System.out.println("***************************************************************");
        System.out.println("Verificar el tratamiento de objetos con loops");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initInner();
        sve.initEnum();
        sve.initArrayList();
        sve.initHashMap();
        
        SimpleVertexEx sveLoop = new SimpleVertexEx();
        sveLoop.initInner();
        sveLoop.initEnum();
        sveLoop.initArrayList();
        sveLoop.initHashMap();
        
        // crear el loop
        sve.setLooptest(sveLoop);
        sveLoop.setLooptest(sve);
        
        System.out.println("pre store..............................");
        SimpleVertexEx result = this.sm.store(sve);
        System.out.println("store ok!");
        System.out.println("pre commit..............................");
        
        sm.commit();
        System.out.println("commit ok ==============================");
        
        System.out.println(" inicio de los test");
        String rid = ((IObjectProxy)result).___getRid();
        System.out.println("1 >>>>>>>>>>>>>");
        String looprid = ((IObjectProxy)result.getLooptest()).___getRid();
        System.out.println("2 >>>>>>>>>>>>>");
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: "+rid+" loop rid: "+looprid);
        System.out.println("");
        System.out.println("");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        
        System.out.println("");
        System.out.println("");
        System.out.println(" get completado. Iniciando los asserts");
        System.out.println("");
        System.out.println("");
        
        
        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
        assertEquals(((IObjectProxy)expResult.getLooptest()).___getRid(), ((IObjectProxy)result.getLooptest()).___getRid());
        
        String expRid = ((IObjectProxy)expResult.getLooptest().getLooptest()).___getRid();
        String rrid = ((IObjectProxy)result).___getRid();
        
        assertEquals(expRid,rrid);
        
        System.out.println("============================= FIN LoopTest ===============================");
    }
    
//    /**
//     * Test of setAsDirty method, of class SessionManager.
//     */
//    //@Test
//    public void testSetAsDirty() {
//        System.out.println("setAsDirty");
//        Object o = null;
//        SessionManager instance = null;
//        instance.setAsDirty(o);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getObjectMapper method, of class SessionManager.
//     */
//    //@Test
//    public void testGetObjectMapper() {
//        System.out.println("getObjectMapper");
//        SessionManager instance = null;
//        ObjectMapper expResult = null;
//        ObjectMapper result = instance.getObjectMapper();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRID method, of class SessionManager.
//     */
//    //@Test
//    public void testGetRID() {
//        System.out.println("getRID");
//        Object o = null;
//        SessionManager instance = null;
//        String expResult = "";
//        String result = instance.getRID(o);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of commit method, of class SessionManager.
//     */
//    //@Test
//    public void testCommit() {
//        System.out.println("commit");
//        SessionManager instance = null;
//        instance.commit();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of flush method, of class SessionManager.
//     */
//    //@Test
//    public void testFlush() {
//        System.out.println("flush");
//        SessionManager instance = null;
//        instance.flush();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of rollback method, of class SessionManager.
//     */
//    //@Test
//    public void testRollback() {
//        System.out.println("rollback");
//        SessionManager instance = null;
//        instance.rollback();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of shutdown method, of class SessionManager.
//     */
//    //@Test
//    public void testShutdown() {
//        System.out.println("shutdown");
//        SessionManager instance = null;
//        instance.shutdown();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTxConflics method, of class SessionManager.
//     */
//    //@Test
//    public void testGetTxConflics() {
//        System.out.println("getTxConflics");
//        SessionManager instance = null;
//        instance.getTxConflics();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getGraphdb method, of class SessionManager.
//     */
//    //@Test
//    public void testGetGraphdb() {
//        System.out.println("getGraphdb");
//        SessionManager instance = null;
//        OrientGraph expResult = null;
//        OrientGraph result = instance.getGraphdb();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of get method, of class SessionManager.
//     */
//    //@Test
//    public void testGet() {
//        System.out.println("get");
//        SessionManager instance = null;
//        Object expResult = null;
//        Object result = instance.get(null);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getEdgeAsObject method, of class SessionManager.
//     */
//    //@Test
//    public void testGetEdgeAsObject() {
//        System.out.println("getEdgeAsObject");
//        SessionManager instance = null;
//        Object expResult = null;
//        Object result = instance.getEdgeAsObject(null);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of delete method, of class SessionManager.
//     */
//    //@Test
//    public void testDelete() {
//        System.out.println("delete");
//        Object toRemove = null;
//        SessionManager instance = null;
//        instance.delete(toRemove);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of query method, of class SessionManager.
//     */
//    //@Test
//    public void testQuery_String() {
//        System.out.println("query");
//        String sql = "";
//        SessionManager instance = null;
//        Object expResult = null;
//        Object result = instance.query(sql);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of query method, of class SessionManager.
//     */
//    //@Test
//    public void testQuery_Class() {
//        System.out.println("query");
//        SessionManager instance = null;
//        List expResult = null;
//        List result = instance.query(null);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of query method, of class SessionManager.
//     */
//    //@Test
//    public void testQuery_Class_String() {
//        System.out.println("query");
//        SessionManager instance = null;
//        List expResult = null;
//        List result = instance.query(null);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of query method, of class SessionManager.
//     */
//    //@Test
//    public void testQuery_3args() {
//        System.out.println("query");
//        SessionManager instance = null;
//        List expResult = null;
//        List result = instance.query(null);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDBClass method, of class SessionManager.
//     */
//    //@Test
//    public void testGetDBClass() {
//        System.out.println("getDBClass");
//        Class clase = null;
//        SessionManager instance = null;
//        OClass expResult = null;
//        OClass result = instance.getDBClass(clase);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    
}