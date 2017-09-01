/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import Test.EdgeAttrib;
import Test.EnumTest;
import Test.SSimpleVertex;
import net.odbogm.SessionManager;
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
import net.odbogm.security.AccessRight;
import net.odbogm.security.GroupSID;
import net.odbogm.security.UserSID;
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
        this.sm.setAuditOnUser("userAuditado");

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
    public void testDates() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("escritura/lectura de objectos con campos Date");
        System.out.println("***************************************************************");
        
        SimpleVertex sv = new SimpleVertex();
        sv.setFecha(new Date(2016, 8, 26));
        SimpleVertex expResult = sv;
        
        SimpleVertex result = sm.store(sv);
        
        assertEquals(expResult.getFecha(), result.getFecha());
        sm.commit();
        assertEquals(expResult.getFecha(), result.getFecha());
        String rid = sm.getRID(result);
        
        SimpleVertex ret = sm.get(SimpleVertex.class,rid);
        assertEquals(expResult.getFecha(), ret.getFecha());
        
    }
    
//    @Test
    public void testStorePrimitiveCol() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objeto con colecciones de primitivas");
        System.out.println("***************************************************************");
        
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initArrayListString();
        sve.initHashMapString();
        
        SimpleVertexEx expResult = sve;
        
        assertEquals(0, sm.getDirtyCount());
        
        SimpleVertexEx result = sm.store(sve);
        
        assertEquals(1, sm.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);
        
        // verificar que sean iguales antes de comitear
        assertEquals(expResult.alString.size(), result.alString.size());
        assertEquals(expResult.hmString.size(), result.hmString.size());
        
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy)result).___getRid();
        expResult = this.sm.get(SimpleVertexEx.class, rid);

        assertEquals(0, sm.getDirtyCount());
        
        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);
        
        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
        
        // verificar que sean iguales después de realizar el commit
        assertEquals(expResult.alString.size(), result.alString.size());
        assertEquals(expResult.hmString.size(), result.hmString.size());
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
        System.out.println("c.svinner: "+completo.getSvinner());
        assertNotNull(completo.getSvinner());
        System.out.println("c.svinner: "+sm.getRID(completo.getSvinner())+" <---> "+svRid);
        assertEquals(sm.getRID(completo.getSvinner()), svRid);
        
        System.out.println("*******************************************************************");
        System.out.println("\n\n\n");
    }
    
    
    @Test
    public void testStoreFullObject() {
        System.out.println("\n\n\n");
        System.out.println("******************************************************************************");
        System.out.println("StoreFullObject: Verificar un objecto completamente inicializado y almacenado.");
        System.out.println("******************************************************************************");
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
        assertEquals("verificando rids...",((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
        
        assertEquals("verificando int...",expResult.getI(), sve.getI());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals("verificando strings...",expResult.getS(), sve.getS());
        assertEquals("verificando booleans...",expResult.getoB(), sve.getoB());
        assertEquals("verificando float...",expResult.getoF(), sve.getoF());
        assertEquals("verificando Integer...",expResult.getoI(), sve.getoI());
        assertEquals("verificando SVEX...",expResult.getSvex(), sve.getSvex());
        assertEquals("verificando ENUM...",expResult.getEnumTest(), sve.getEnumTest());

        System.out.println("sve: "+sve.getSvinner().getS());
        System.out.println("expResult: "+expResult.getSvinner().getS());
        
        assertEquals("verificando svinner.string ...",expResult.getSvinner().getS(), sve.getSvinner().getS());
        assertEquals("verificando AL.size()...",expResult.getAlSV().size(), sve.getAlSV().size());
        assertEquals("verificando HM.size()...",expResult.getHmSV().size(), sve.getHmSV().size());
        
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
        System.out.println("-1-");
        assertEquals(((IObjectProxy)expResult.getLooptest()).___getRid(), ((IObjectProxy)result.getLooptest()).___getRid());
        System.out.println("-2-");
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
        
        System.out.println("primer commit finalizado. RID: "+rid+" ------------------------------------------------------------");
        
        assertNull(stored.getAlSVE());
        
        System.out.println("Agrego un AL nuevo");
        ArrayList<SimpleVertexEx> nal = new ArrayList<>();
        stored.setAlSVE(nal);
        nal.add(new SimpleVertexEx());
        nal.add(new SimpleVertexEx());
        
        System.out.println("\ninicio segundo commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("segundo commit finalizado ----------------------------------------------------------\n");
        
        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("retrieved: "+retrieved+" : "+retrieved.getAlSVE());
        System.out.println("stored: "+stored+" : "+stored.getAlSVE()+"\n\n");
        int iretSize = retrieved.getAlSVE().size();
        int istoredSize = stored.getAlSVE().size();
        assertEquals(iretSize,istoredSize);
        
        System.out.println("\nagregamos un nuevo objeto al arraylist ya inicializado");
        stored.getAlSVE().add(new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");
        
        retrieved = sm.get(SimpleVertexEx.class, rid);
        
        System.out.println("retrieved: "+retrieved+" : "+retrieved.getAlSVE());
        System.out.println("stored: "+stored+" : "+stored.getAlSVE());
        
        assertEquals(retrieved.getAlSVE().size(), stored.getAlSVE().size());
        
    }
    
    
    @Test
    public void testHashMap(){
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar el comportamiento de los HashMap simples");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        
        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = ((IObjectProxy)stored).___getRid();
        
        System.out.println("primer commit finalizado. RID: "+rid+" ------------------------------------------------------------");
        
        assertNull(stored.getHmSVE());
        
        System.out.println("Agrego un HM nuevo");
        HashMap<String,SimpleVertexEx> nhm = new HashMap<String, SimpleVertexEx>();
        stored.setHmSVE(nhm);
        nhm.put("key1",new SimpleVertexEx());
        nhm.put("key2",new SimpleVertexEx());
        
        System.out.println("\ninicio segundo commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("segundo commit finalizado ----------------------------------------------------------\n");
        
        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("retrieved: "+retrieved+" : "+retrieved.getHmSVE());
        System.out.println("stored: "+stored+" : "+stored.getHmSVE()+"\n\n");
        int iretSize = retrieved.getHmSVE().size();
        int istoredSize = stored.getHmSVE().size();
        assertEquals(iretSize,istoredSize);

        SimpleVertexEx hmsveGetted = retrieved.getHmSVE().get("key1");
        System.out.println("key1: "+(hmsveGetted==null?" NULL!":"Ok."));
        assertNotNull(hmsveGetted);
        
        System.out.println("\nagregamos un nuevo objeto al hashmap ya inicializado");
        stored.getHmSVE().put("key3",new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");
        
        retrieved = sm.get(SimpleVertexEx.class, rid);
        
        System.out.println("retrieved: "+retrieved+" : "+retrieved.getHmSVE());
        System.out.println("stored: "+stored+" : "+stored.getHmSVE());
        
        assertEquals(retrieved.getHmSVE().size(), stored.getHmSVE().size());
        
    }
    
    
    @Test
    public void testComplexHashMap(){
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar el comportamiento de los HashMap con objetos como key");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        
        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = ((IObjectProxy)stored).___getRid();
        
        System.out.println("primer commit finalizado. RID: "+rid+" ------------------------------------------------------------");
        
        assertNull(stored.getOhmSVE());
        
        System.out.println("Agrego un HM nuevo");
        HashMap<EdgeAttrib,SimpleVertexEx> ohm = new HashMap<>();
        stored.setOhmSVE(ohm);
        ohm.put(new EdgeAttrib("nota 1", DateHelper.getCurrentDate()),new SimpleVertexEx());
        ohm.put(new EdgeAttrib("nota 2", DateHelper.getCurrentDate()),new SimpleVertexEx());
        
        System.out.println("\ninicio segundo commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("segundo commit finalizado ----------------------------------------------------------\n");
        
        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("retrieved: "+retrieved+" : "+retrieved.getOhmSVE());
        System.out.println("stored: "+stored+" : "+stored.getOhmSVE()+"\n\n");
        int iretSize = retrieved.getOhmSVE().size();
        int istoredSize = stored.getOhmSVE().size();
        assertEquals(iretSize,istoredSize);

//        SimpleVertexEx ohmsveGetted = retrieved.getOhmSVE().get("key1");
//        System.out.println("key1: "+(ohmsveGetted==null?" NULL!":"Ok."));
//        assertNotNull(ohmsveGetted);
//        
        System.out.println("\nagregamos un nuevo objeto al hashmap ya inicializado");
        stored.getOhmSVE().put(new EdgeAttrib("nota 3", DateHelper.getCurrentDate()),new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");
        
        retrieved = sm.get(SimpleVertexEx.class, rid);
        
        System.out.println("retrieved: "+retrieved+" : "+retrieved.getOhmSVE());
        System.out.println("stored: "+stored+" : "+stored.getOhmSVE());
        
        assertEquals(retrieved.getOhmSVE().size(), stored.getOhmSVE().size());
        
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
                     
            //System.out.println("Query: "+object.getClass()+" - toString: "+object.getClass().getSimpleName());
        }
        assertTrue(isv>0);
        assertTrue(isve>0);
        
        
        System.out.println("***************************************************************");
        System.out.println("Fin SimpleQuery");
        System.out.println("***************************************************************");
    }
    
    
    /**
     * Rollback simple de los atributos
     * 
     */
    @Test
    public void testRollbackSimple() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback simple. Solo se restablecen los atributos directos.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
//        sve.initEnum();
//        sve.initInner();
//        sve.initArrayList();
//        sve.initHashMap();
        
        sve.setI(1);
        sve.setF(1.0f);
        sve.setB(true);
        sve.setS("init rollback");
        sve.setoI(10);
        sve.setoF(1.5f);

        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = sm.getRID(stored);
        
        // modificar los campos.
        stored.setI(42);
        stored.setF(3.0f);
        stored.setB(false);
        stored.setS("rollbak");
        stored.setoI(45);
        stored.setoF(4.5f);
        
        sm.rollback();
        
        assertEquals(sve.getI(), stored.getI());
        assertEquals(sve.getF(), stored.getF(),0.0002);
        assertEquals(sve.isB(), stored.isB());
        assertEquals(sve.getoI(), stored.getoI());
        assertEquals(sve.getoF(), stored.getoF(),0.0002);
    }
    
    
    @Test
    public void testRollbackEnum() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback Enum. Se restablecen los atributos Enum.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initEnum();
//        sve.initInner();
//        sve.initArrayList();
//        sve.initHashMap();
        
        sve.setEnumTest(EnumTest.UNO);

        System.out.println("guardado del objeto.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = sm.getRID(stored);
        
        // modificar los campos.
        stored.setEnumTest(EnumTest.DOS);
        
        sm.rollback();
        
        assertEquals(sve.getEnumTest(), stored.getEnumTest());
    }
    
    
    @Test
    public void testRollbackCollections() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback Collections. Se restablecen los atributos que hereden de Collection.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
//        sve.initEnum();
//        sve.initInner();
//        sve.initArrayList();
//        sve.initHashMap();
        sve.alSV = new ArrayList<SimpleVertex>();
        sve.alSV.add(new SimpleVertex());
        sve.alSV.add(new SimpleVertex());
        sve.alSV.add(new SimpleVertex());

        System.out.println("guardando el objeto con 3 elementos en el AL.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = sm.getRID(stored);
        
        // modificar los campos.
        stored.alSV.add(new SimpleVertex());
        
        sm.rollback();
        
        assertEquals(sve.alSV.size(), stored.alSV.size());
    }
    
    
    @Test
    public void testRollbackMaps() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback Maps. Se restablecen los atributos que hereden de Collection.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
//        sve.initEnum();
//        sve.initInner();
//        sve.initArrayList();
//        sve.initHashMap();
        sve.hmSV = new HashMap<String, SimpleVertex>();
        SimpleVertex sv = new SimpleVertex();
        sve.hmSV.put("key1", sv);
        sve.hmSV.put("key2", sv);
        sve.hmSV.put("key3", new SimpleVertex());
        
        System.out.println("guardando el objeto con 3 elementos en el HM.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();
        
        String rid = sm.getRID(stored);
        
        // modificar los campos.
        stored.hmSV.put("key rollback",new SimpleVertex());
        
        sm.rollback();
        
        assertEquals(sve.hmSV.size(), stored.hmSV.size());
    }
    
    
    /**
     * Test of store method, of class SessionManager.
     */
    @Test
    public void testStoreWithInterfaceAttr() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objeto with Interface as attr (SimpleVertexWithInterfaceAttr)");
        System.out.println("***************************************************************");
        
        SimpleVertexInterfaceAttr sv = new SimpleVertexInterfaceAttr();
        SimpleVertexInterfaceAttr expResult = sv;
        
        assertEquals(0, sm.getDirtyCount());
        
        SimpleVertexInterfaceAttr result = sm.store(sv);
        
        assertEquals(2, sm.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);
        
        assertEquals(expResult.getS(), result.getS());
        
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy)result).___getRid();
        expResult = this.sm.get(SimpleVertexInterfaceAttr.class, rid);

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
    
    
    /**
     * Test of store embedded list and maps.
     */
    @Test
    public void testEmbedded() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objet with List<Primiteve> and Map<Prim,Prim> as attr (SimpleVertexWithEmbedded)");
        System.out.println("***************************************************************");
        
        SimpleVertexWithEmbedded svemb = new SimpleVertexWithEmbedded();
        
        assertEquals(0, sm.getDirtyCount());
        
        SimpleVertexWithEmbedded result = this.sm.store(svemb);
        
        assertEquals(1, sm.getDirtyCount());
        
        this.sm.commit();
        
        assertEquals(0, sm.getDirtyCount());
        
        String rid = ((IObjectProxy)result).___getRid();
        
        SimpleVertexWithEmbedded ret = this.sm.get(SimpleVertexWithEmbedded.class, rid);
        
        assertEquals(svemb.getStringlist().size(), ret.getStringlist().size());
        assertEquals(svemb.getSimplemap().size(), ret.getSimplemap().size());
        
        System.out.println("Anexando uno a la lista");
        ret.addToList();

        assertNotEquals(svemb.getStringlist().size(), ret.getStringlist().size());
        
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("modificando el contenido de un elemento de la lista...");
        ret.getStringlist().set(1, "-1-");
        assertEquals(1, sm.getDirtyCount());
        this.sm.commit();
        
        System.out.println("==========================================================");
        System.out.println("Anexando uno al map");
        System.out.println("==========================================================");
        assertEquals(0, sm.getDirtyCount());
        ret.addToMap();
        assertEquals(1, sm.getDirtyCount());
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("==========================================================");
        ret.getSimplemap().put("key 1", 10);
        assertEquals(1, sm.getDirtyCount());
        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());
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
        this.sm.getGraphdb().command(new OCommandSQL("delete vertex GroupSID")).execute();
        
        // eliminar los usuarios
        this.sm.getGraphdb().command(new OCommandSQL("delete vertex UserSID")).execute();
        
        // eliminar los SSVertex
        this.sm.getGraphdb().command(new OCommandSQL("delete vertex SSimpleVertex")).execute();
        
        // crear los grupos y los usuarios.
        GroupSID gna = new GroupSID("gna","gna");
        GroupSID gr = new GroupSID("gr","gr");
        GroupSID gw = new GroupSID("gw","gw");
        
        GroupSID sgna = this.sm.store(gna);
        GroupSID sgr = this.sm.store(gr);
        GroupSID sgw = this.sm.store(gw);
        this.sm.commit();
        
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
                
        String reg = ((IObjectProxy)ssv).___getRid();
        System.out.println("RID: "+reg);
//        SSimpleVertex rssv = this.sm.get(SSimpleVertex.class, reg);
        
        System.out.println("Agregando los acls...");
        ssv.setAcl(gna, new AccessRight().setRights(AccessRight.NOACCESS));
        ssv.setAcl(gr, new AccessRight().setRights(AccessRight.READ));
        ssv.setAcl(gw, new AccessRight().setRights(AccessRight.WRITE));
        
        this.sm.commit();
        
        this.sm.setLoggedInUser(una);
        System.out.println("Login UserNoAccess");
        SSimpleVertex ssvna = this.sm.get(SSimpleVertex.class,reg);
        System.out.println("State: "+ssvna.getSecurityState());
        assertTrue(ssvna.getSecurityState()==AccessRight.NOACCESS);
        
        System.out.println("Login UserRead");
        this.sm.setLoggedInUser(ur);
        SSimpleVertex ssvr = this.sm.get(SSimpleVertex.class,reg);
        System.out.println("State: "+ssvr.getSecurityState());
        assertTrue(ssvr.getSecurityState()==AccessRight.READ);
        
        this.sm.setLoggedInUser(uw);
        System.out.println("Login UserWrite");
        SSimpleVertex ssvw = this.sm.get(SSimpleVertex.class,reg);
        System.out.println("State: "+ssvw.getSecurityState());
        assertTrue(ssvw.getSecurityState()==AccessRight.WRITE);
        
    }
    
    
}