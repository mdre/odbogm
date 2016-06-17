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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
        this.sm.setDeclaredClasses("Test");
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
    @Test
    public void testStoreSimple() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objeto simple (SimpleVertex)");
        System.out.println("***************************************************************");
        
        SimpleVertex sv = new SimpleVertex();
        SimpleVertex expResult = sv;
        
        assertEquals(0, sm.getDirtyCount());
        
        SimpleVertex result = sm.store(sv);
        
        assertEquals(1, sm.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);
        
        assertEquals(expResult.i, result.i);
        
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy)result).___getRid();
        expResult = this.sm.get(SimpleVertex.class, rid);

        assertEquals(0, sm.getDirtyCount());
        
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

    
    @Test
    public void testStoreExtendedObject() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objeto Extendido (SimpleVertexEx)");
        System.out.println("***************************************************************");
        
        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx result = this.sm.store(sve);
        
        assertEquals(1, sm.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);
        
        // TODO review the generated test code and remove the default call to fail.
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy)result).___getRid();
        
        SimpleVertexEx expResult = this.sm.get(SimpleVertexEx.class, rid);
        
        assertEquals(0, sm.getDirtyCount());
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
    
    @Test
    public void testStoreAndLinkToExistingObject() {
        System.out.println("\n\n\n");
        System.out.println("*******************************************************************");
        System.out.println("Verificar la creación de un objeto y el linkeo a otro ya existente.");
        System.out.println("*******************************************************************");
        SimpleVertex sv = new SimpleVertex();
        sv.setS("vinculado interno");
        SimpleVertexEx sve = new SimpleVertexEx();
        
        // guardar los objetos.
        System.out.println("guardando los objetos vacíos....");
        SimpleVertex ssv = sm.store(sv);
        SimpleVertexEx ssve = sm.store(sve);
        assertEquals(2, sm.getDirtyCount());
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("----------------\n\n\n");
        
        String svRid = sm.getRID(ssv);
        String sveRid = sm.getRID(ssve);
        System.out.println("svRid: "+svRid);
        System.out.println("sveRid: "+sveRid);

        assertEquals(0, sm.getDirtyCount());
        
        // recuperar los objetos desde la base.
        System.out.println("Recuperar los objetos sin vincular....");
        SimpleVertex rsv = sm.get(SimpleVertex.class,svRid);
        SimpleVertexEx rsve = sm.get(SimpleVertexEx.class,sveRid);
        System.out.println("rsv: "+sm.getRID(rsv));
        System.out.println("rsve: "+sm.getRID(rsve));
        System.out.println("\n\n");
        System.out.println("Vinculando...");
        // asociar los objetos
        rsve.setSvinner(rsv);
        assertEquals(1, sm.getDirtyCount());
        // guardar
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("fin del grabado vinculado.\n\n\n\n");
        
        // recuperar nuevamente
        System.out.println("Recupearndo el objeto vinculado...");
        SimpleVertexEx completo = sm.get(SimpleVertexEx.class, sveRid);
        System.out.println("c.svinner: "+completo.svinner);
        assertNotNull(completo.svinner);
        System.out.println("c.svinner: "+sm.getRID(completo.svinner)+" <---> "+svRid);
        assertEquals(sm.getRID(completo.svinner), svRid);
        
        System.out.println("*******************************************************************");
        System.out.println("\n\n\n");
    }
    
    
    @Test
    public void testStoreFullObject() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar un objecto completamente inicializado y almacenado.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initInner();        // 1 objeto
        sve.initEnum();
        sve.initArrayList();    // 3 objetos
        sve.initHashMap();      // 3 objetos
        
        assertEquals(0, sm.getDirtyCount());
        System.out.println("sve pre store: "+sve.getSvinner().getS());
        SimpleVertexEx result = this.sm.store(sve);
        assertEquals(7, sm.getDirtyCount());
        
        System.out.println("sve post store: "+sve.getSvinner().getS());
        
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        
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
    
    @Test
    public void testStoreLink() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objeto sin Link y luego se le agrega uno");
        System.out.println("***************************************************************");
        
        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx result = this.sm.store(sve);
        assertEquals(1, sm.getDirtyCount());
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("=========== fin primer commit ====================================");
        
        assertEquals(result.getSvinner(), sve.getSvinner());
        
        // actualizar el objeto administrado
        result.initInner();
        assertEquals(1, sm.getDirtyCount());
        System.out.println("result.svinner: "+result.getSvinner().getS()+ "      toS: "+result.getSvinner().toString());
        // bajarlo a la base
        System.out.println("=========== inicio segundo commit <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
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
        assertEquals(0, sm.getDirtyCount());
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
    
    
    
    @Test
    public void testUpdateObject() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificación de update de un objeto administrado.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initInner();        // un objeto
        sve.initEnum();     
        sve.initArrayList();    // tres objetos
        sve.initHashMap();      // tres objetos
        
        SimpleVertexEx result = this.sm.store(sve);
        
        assertEquals(7, sm.getDirtyCount());
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        
        String rid = ((IObjectProxy)result).___getRid();
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: "+rid);
        System.out.println("");
        System.out.println("");
        
        result.i++;
        result.getAlSV().add(new SimpleVertex());
        
        assertEquals(1, sm.getDirtyCount());
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        
        assertEquals(expResult.i, result.i);
        assertEquals(expResult.alSV.size(), result.alSV.size());
        System.out.println("Fin Update!.............");
    }
    
    
    
    @Test
    public void testLoop(){
        System.out.println("\n\n\n");
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
    
    
    /**
    * Verificar la correscta inicialización de los objetos en un arraylist
    */
    
    @Test
    public void testArrayList(){
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar el comportamiento de los ArrayLists");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        
        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = ((IObjectProxy)stored).___getRid();
        
        System.out.println("primer commit------------------------------------------------------------");
        
        assertNull(stored.getAlSVE());
        
        System.out.println("Agrego un AL nuevo");
        ArrayList<SimpleVertexEx> nal = new ArrayList<>();
        stored.setAlSVE(nal);
        nal.add(new SimpleVertexEx());
        nal.add(new SimpleVertexEx());
        
        sm.commit();
        System.out.println("segundo commit ----------------------------------------------------------");
        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("retrieved: "+retrieved.getAlSVE());
        System.out.println("stored: "+stored.getAlSVE());
        assertEquals(retrieved.getAlSVE().size(), stored.getAlSVE().size());
        
        System.out.println("agregamos un nuevo objeto al arraylist ya inicializado");
        stored.getAlSVE().add(new SimpleVertexEx());
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------");
        
        retrieved = sm.get(SimpleVertexEx.class, rid);
        assertEquals(retrieved.getAlSVE().size(), stored.getAlSVE().size());
        
    }
    
    @Test
    public void testGet() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Get(rid). Verificar que esté devolviendo correctamente los datos");
        System.out.println("de un GET basado solo en el RID");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = sm.getRID(stored);
        
        Object getted = this.sm.get(rid);
        assertTrue(getted instanceof SimpleVertexEx);
        
        System.out.println("***************************************************************");
        System.out.println("\n\n\n");
    }
    
    
    /**
     * Verificar que un query simple basado en una clase devueve el listado correcto
     * de objetos.
     */
    @Test
    public void testSimpleQuery() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Query basado en la clase: verificar que devuelve la clase y los");
        System.out.println("subtipos de la misma");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initEnum();
        sve.initInner();
        sve.initArrayList();
        sve.initHashMap();
        
        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        System.out.println("consultando por SimpleVertex....");
        List list = sm.query(SimpleVertex.class);
        int isv = 0;
        int isve = 0;
        for (Object object : list) {
            if (object instanceof SimpleVertexEx)
                isve++;
            else
                if (object instanceof SimpleVertex)
                    isv++;
                else
                    System.out.println("ERROR:  "+object.getClass()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                     
            System.out.println("Query: "+object.getClass()+" - toString: "+object.getClass().getSimpleName());
        }
        assertTrue(isv>0);
        assertTrue(isve>0);
        
        
        System.out.println("***************************************************************");
        System.out.println("Fin SimpleQuery");
        System.out.println("***************************************************************");
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