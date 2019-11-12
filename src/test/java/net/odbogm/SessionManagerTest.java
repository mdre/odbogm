package net.odbogm;

import static org.junit.Assert.*;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import net.odbogm.agent.ITransparentDirtyDetector;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.RID;
import net.odbogm.cache.SimpleCache;
import net.odbogm.exceptions.ConcurrentModification;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.InvalidObjectReference;
import net.odbogm.exceptions.OdbogmException;
import net.odbogm.exceptions.ReferentialIntegrityViolation;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.security.*;
import net.odbogm.utils.DateHelper;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.RandomStringUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import test.EdgeAttrib;
import test.EnumTest;
import test.Enums;
import test.Foo;
import test.IndirectObject;
import test.InterfaceTest;
import test.SVExChild;
import test.SimpleVertex;
import test.SimpleVertexEx;
import test.SimpleVertexInterfaceAttr;
import test.SimpleVertexWithEmbedded;
import test.SimpleVertexWithImplement;

/**
 *
 * @author SShadow
 */
public class SessionManagerTest {

    private final Field orientdbTransactField;

    private SessionManager sm;


    public SessionManagerTest() throws Exception {
        orientdbTransactField = Transaction.class.getDeclaredField(
                "orientdbTransact");
        orientdbTransactField.setAccessible(true);
    }
    
    @Before
    public void setUp() {
        System.out.println("Iniciando session manager...");
        sm = new SessionManager("remote:localhost/Test", "admin", "admin"
                , 1, 10
                )
//                .setClassLevelLog(ObjectProxy.class, Level.FINEST)
//                .setClassLevelLog(ClassCache.class, Level.FINER)
//                .setClassLevelLog(Transaction.class, Level.FINER)
//                .setClassLevelLog(ObjectProxy.class, Level.FINER)
//                .setClassLevelLog(SimpleCache.class, Level.FINER)
//                .setClassLevelLog(ArrayListLazyProxy.class, Level.FINER)
//                .setClassLevelLog(ObjectMapper.class, Level.FINEST)
//                .setClassLevelLog(SObject.class, Level.FINER)
//                .setClassLevelLog(TransparentDirtyDetectorInstrumentator.class, Level.FINER)
                ;
        System.out.println("Begin");
        this.sm.begin();
        sm.getCurrentTransaction().setCacheCleanInterval(1);
        System.out.println("fin setup.");
    }

    
    @After
    public void tearDown() {
        sm.shutdown();
    }

    
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
        String rid = ((IObjectProxy) result).___getRid();
        expResult = this.sm.get(SimpleVertex.class, rid);

        assertEquals(0, sm.getDirtyCount());

        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);

        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());

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
        System.out.println("result hc: " + System.identityHashCode(sv));

        assertEquals(expResult.getFecha(), result.getFecha());
        sm.commit();
        assertEquals(expResult.getFecha(), result.getFecha());
        String rid = sm.getRID(result);
        System.out.println("RID: " + rid);
        SimpleVertex ret = sm.get(SimpleVertex.class, rid);
        System.out.println("get hc: " + System.identityHashCode(ret));

        assertEquals(expResult.getFecha(), ret.getFecha());

        // quitarlo del caché
        ret = null;
        sm.getCurrentTransaction().removeFromCache(rid);

        // verificar fecha y hora.
        ret = sm.get(SimpleVertex.class, rid);
        // asignarle una fecha y hora.
        Date d = new Date(2016, 8, 26, 10, 0, 0);
        System.out.println("Date: " + d);
        Date targetDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        System.out.println("Date: " + targetDate);
        ret.setFecha(targetDate);

        sm.commit();

        ret = null;
        sm.getCurrentTransaction().removeFromCache(rid);

        ret = sm.get(SimpleVertex.class, rid);
        assertEquals(targetDate, ret.getFecha());
        System.out.println("ret.date: " + ret.getFecha());
    }

    @Test
    public void testStorePrimitiveCol() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("store objeto con colecciones de primitivas");
        System.out.println("***************************************************************");

        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initArrayListString();
        sve.initHashMapString();

        SimpleVertexEx expResult = sve;

        assertEquals(0, sm.getDirtyCount());

        SimpleVertexEx result = sm.store(sve);

        System.out.println("store...");
        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        assertEquals(1, sm.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);

        // verificar que sean iguales antes de comitear
        assertEquals(expResult.alString.size(), result.alString.size());
        assertEquals(expResult.hmString.size(), result.hmString.size());

        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("commit");
        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy) result).___getRid();
        expResult = this.sm.get(SimpleVertexEx.class, rid);

        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        assertEquals(0, sm.getDirtyCount());

        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);

        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());

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
        String rid = ((IObjectProxy) result).___getRid();

        SimpleVertexEx expResult = this.sm.get(SimpleVertexEx.class, rid);

        assertEquals(0, sm.getDirtyCount());
        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);

        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());

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

        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        // guardar los objetos.
        System.out.println("guardando los objetos vacíos....");
        SimpleVertex ssv = sm.store(sv);
//        System.out.println("oc: "+sm.getCurrentTransaction().getObjectCache());
        SimpleVertexEx ssve = sm.store(sve);

        System.out.println("temp rid ssv: " + sm.getRID(ssv));
        System.out.println("temp rid ssve: " + sm.getRID(ssve));

        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        assertEquals(2, sm.getDirtyCount());
        System.out.println("commit...");
        sm.commit();

        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        assertEquals(0, sm.getDirtyCount());
        System.out.println("----------------\n\n\n");

        String svRid = sm.getRID(ssv);
        String sveRid = sm.getRID(ssve);
        System.out.println("svRid: " + svRid);
        System.out.println("sveRid: " + sveRid);

        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        assertEquals(0, sm.getDirtyCount());

        // recuperar los objetos desde la base.
        System.out.println("Recuperar los objetos sin vincular....");
        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        SimpleVertex rsv = sm.get(SimpleVertex.class, svRid);
        SimpleVertexEx rsve = sm.get(SimpleVertexEx.class, sveRid);
        System.out.println("rsv: " + sm.getRID(rsv));
        System.out.println("rsve: " + sm.getRID(rsve));
        System.out.println("\n\n");
        // asociar los objetos
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        System.out.println("Vinculando: rsve.setSvinner(rsv)");
        rsve.setSvinner(rsv);
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        assertEquals(1, sm.getDirtyCount());

        // guardar
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("fin del grabado vinculado.\n\n\n\n");

        // recuperar nuevamente
        System.out.println("Recupearndo el objeto vinculado...");
        SimpleVertexEx completo = sm.get(SimpleVertexEx.class, sveRid);
        System.out.println("c.svinner: " + completo.getSvinner());
        assertNotNull(completo.getSvinner());
        System.out.println("c.svinner: " + sm.getRID(completo.getSvinner()) + " <---> " + svRid);
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
        System.out.println("sve pre store: " + sve.getSvinner().getS());
        SimpleVertexEx result = this.sm.store(sve);
        assertEquals(7, sm.getDirtyCount());

        System.out.println("sve post store: " + sve.getSvinner().getS());

        sm.commit();
        assertEquals(0, sm.getDirtyCount());

        System.out.println("sve post commit: " + sve.getSvinner().getS());

        System.out.println("");
        String rid = ((IObjectProxy) result).___getRid();
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: " + rid);
        System.out.println("");
        System.out.println("");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);

        System.out.println("");
        System.out.println("");
        System.out.println(" get completado. Iniciando los asserts");
        System.out.println("");
        System.out.println("");

        // verificar que todos los valores sean iguales
        assertEquals("verificando rids...", ((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());

        assertEquals("verificando int...", expResult.getI(), sve.getI());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals("verificando strings...", expResult.getS(), sve.getS());
        assertEquals("verificando booleans...", expResult.getoB(), sve.getoB());
        assertEquals("verificando float...", expResult.getoF(), sve.getoF());
        assertEquals("verificando Integer...", expResult.getoI(), sve.getoI());
        assertEquals("verificando SVEX...", expResult.getSvex(), sve.getSvex());
        assertEquals("verificando ENUM...", expResult.getEnumTest(), sve.getEnumTest());

        System.out.println("sve: " + sve.getSvinner().getS());
        System.out.println("expResult: " + expResult.getSvinner().getS());

        assertEquals("verificando svinner.string ...", expResult.getSvinner().getS(), sve.getSvinner().getS());
        assertEquals("verificando AL.size()...", expResult.getAlSV().size(), sve.getAlSV().size());
        assertEquals("verificando HM.size()...", expResult.getHmSV().size(), sve.getHmSV().size());

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
        System.out.println("result.svinner: " + result.getSvinner().getS() + "      toS: " + result.getSvinner().toString());
        // bajarlo a la base
        System.out.println("=========== inicio segundo commit <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sm.commit();
        assertEquals(0, sm.getDirtyCount());
        System.out.println("=========== fin segundo commit ====================================");
        System.out.println("dirty count: " + sm.getDirtyCount());
        if (sm.getActivationStrategy() == SessionManager.ActivationStrategy.CLASS_INSTRUMENTATION) {
            System.out.println("isDirty" + ((ITransparentDirtyDetector) result).___ogm___isDirty());
            System.out.println("isDirty" + ((ITransparentDirtyDetector) result.svinner).___ogm___isDirty());

            System.out.println("result.svinner: " + result.getSvinner().getS());
            System.out.println("isDirty" + ((ITransparentDirtyDetector) result).___ogm___isDirty());

            System.out.println("dirty count: " + sm.getDirtyCount());
            System.out.println("isDirty" + ((ITransparentDirtyDetector) result).___ogm___isDirty());
        }
        System.out.println("      toS: " + result.getSvinner().toString());
        System.out.println("dirty count: " + sm.getDirtyCount());
        // recuperar el objeto en otra instancia
        String rid = ((IObjectProxy) result).___getRid();

        assertEquals(0, sm.getDirtyCount());
        System.out.println("============================================================================");
        System.out.println("RID: " + rid);
        System.out.println("============================================================================");

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("========= comienzo del get =================================================");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        assertEquals(0, sm.getDirtyCount());
        System.out.println("========= fin del get =================================================");

        assertEquals(((IObjectProxy) expResult).___getRid(), rid);

        System.out.println("++++++++++++++++ result: " + result.getSvinner().toString());
        System.out.println("++++++++++++++++ expResult: " + expResult.getSvinner().toString());

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
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        String rid = ((IObjectProxy) result).___getRid();
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: " + rid);
        System.out.println("");
        System.out.println("");

        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        System.out.println("AL actual");
        System.out.println("alSV: " + result.getAlSV());
        System.out.println("---------");
        result.i++;
        System.out.println("agregar un elemento...");
        result.getAlSV().add(new SimpleVertex());
        System.out.println("--------------------");
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        assertEquals(1, sm.getDirtyCount());
        sm.commit();
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());
        assertEquals(0, sm.getDirtyCount());

        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);

        assertEquals(expResult.i, result.i);
        assertEquals(expResult.alSV.size(), result.alSV.size());
        System.out.println("Fin Update!.............");
    }

    @Test
    public void testLoop() {
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
        String rid = ((IObjectProxy) result).___getRid();
        System.out.println("1 >>>>>>>>>>>>>");
        String looprid = ((IObjectProxy) result.getLooptest()).___getRid();
        System.out.println("2 >>>>>>>>>>>>>");
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: " + rid + " loop rid: " + looprid);
        System.out.println("");
        System.out.println("");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);

        System.out.println("");
        System.out.println("");
        System.out.println(" get completado. Iniciando los asserts");
        System.out.println("");
        System.out.println("");

        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());
        System.out.println("-1-");
        assertEquals(((IObjectProxy) expResult.getLooptest()).___getRid(), ((IObjectProxy) result.getLooptest()).___getRid());
        System.out.println("-2-");
        String expRid = ((IObjectProxy) expResult.getLooptest().getLooptest()).___getRid();
        String rrid = ((IObjectProxy) result).___getRid();

        assertEquals(expRid, rrid);

        System.out.println("============================= FIN LoopTest ===============================");
    }

    /**
     * Verificar la correscta inicialización de los objetos en un arraylist
     */
    @Test
    public void testArrayList() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar el comportamiento de los ArrayLists");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();

        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();

        String rid = ((IObjectProxy) stored).___getRid();

        // validar que no se modifique la lista
        assertNull(stored.lSV);
        System.out.println("primer commit finalizado. RID: " + rid + " ------------------------------------------------------------");

        assertNull(stored.getAlSVE());

        System.out.println("Agrego un AL nuevo");
        ArrayList<SimpleVertexEx> nal = new ArrayList<>();
        stored.setAlSVE(nal);
        nal.add(new SimpleVertexEx());
        nal.add(new SimpleVertexEx());

        System.out.println("\ninicio segundo commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("segundo commit finalizado ----------------------------------------------------------\n");
        // validar que no se modifique la lista
        assertNull(stored.lSV);

        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("retrieved: " + retrieved + " : " + retrieved.getAlSVE());
        System.out.println("stored: " + stored + " : " + stored.getAlSVE() + "\n\n");
        int iretSize = retrieved.getAlSVE().size();
        int istoredSize = stored.getAlSVE().size();
        assertEquals(iretSize, istoredSize);

        // eliminar la referencia
        retrieved = null;

        System.out.println("\nagregamos un nuevo objeto al arraylist ya inicializado");
        stored.getAlSVE().add(new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");

        System.out.println("stored hc: " + stored.hashCode());
        System.out.println("cache: " + sm.getCurrentTransaction().getObjectCache());

        System.out.println("recuperar objetos.");
        retrieved = sm.get(SimpleVertexEx.class, rid);

        System.out.println("retrieved: " + retrieved + " : " + retrieved.getAlSVE() + "  hc: " + retrieved.hashCode());
        System.out.println("stored: " + stored + " : " + stored.getAlSVE() + "  hc: " + stored.hashCode());

        assertEquals(retrieved.getAlSVE().size(), stored.getAlSVE().size());
        // validar que no se modifique la lista
        assertNull(stored.lSV);

    }

    @Test
    public void testHashMap() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar el comportamiento de los HashMap simples");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();

        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();

        String rid = ((IObjectProxy) stored).___getRid();

        System.out.println("primer commit finalizado. RID: " + rid + " ------------------------------------------------------------");

        assertNull(stored.getHmSVE());

        System.out.println("Agrego un HM nuevo");
        HashMap<String, SimpleVertexEx> nhm = new HashMap<String, SimpleVertexEx>();
        stored.setHmSVE(nhm);
        nhm.put("key1", new SimpleVertexEx());
        nhm.put("key2", new SimpleVertexEx());

        System.out.println("\ninicio segundo commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("segundo commit finalizado ----------------------------------------------------------\n");

        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("retrieved: " + retrieved + " : " + retrieved.getHmSVE());
        System.out.println("stored: " + stored + " : " + stored.getHmSVE() + "\n\n");
        int iretSize = retrieved.getHmSVE().size();
        int istoredSize = stored.getHmSVE().size();
        assertEquals(iretSize, istoredSize);

        SimpleVertexEx hmsveGetted = retrieved.getHmSVE().get("key1");
        System.out.println("key1: " + (hmsveGetted == null ? " NULL!" : "Ok."));
        assertNotNull(hmsveGetted);

        System.out.println("\nagregamos un nuevo objeto al hashmap ya inicializado");
        stored.getHmSVE().put("key3", new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");

        retrieved = sm.get(SimpleVertexEx.class, rid);

        System.out.println("retrieved: " + retrieved + " : " + retrieved.getHmSVE());
        System.out.println("stored: " + stored + " : " + stored.getHmSVE());

        assertEquals(retrieved.getHmSVE().size(), stored.getHmSVE().size());

    }

    @Test
    public void testComplexHashMap() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Verificar el comportamiento de los HashMap con objetos como key");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();

        System.out.println("guardado del objeto limpio.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();

        String rid = ((IObjectProxy) stored).___getRid();

        System.out.println("primer commit finalizado. RID: " + rid + " ------------------------------------------------------------");

        assertNull(stored.getOhmSVE());

        System.out.println("Agrego un HM nuevo");
        HashMap<EdgeAttrib, SimpleVertexEx> ohm = new HashMap<>();
        stored.setOhmSVE(ohm);
        ohm.put(new EdgeAttrib("nota 1", DateHelper.getCurrentDate()), new SimpleVertexEx());
        ohm.put(new EdgeAttrib("nota 2", DateHelper.getCurrentDate()), new SimpleVertexEx());

        System.out.println("\ninicio segundo commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("segundo commit finalizado ----------------------------------------------------------\n");

        SimpleVertexEx retrieved = sm.get(SimpleVertexEx.class, rid);
        System.out.println("1 ----------");
//        System.out.println("retrieved: " + retrieved + " : " + retrieved.getOhmSVE());
        System.out.println("retrieved: " + retrieved + " : " + retrieved.getOhmSVE());
        System.out.println("2 ----------");
        System.out.println("stored: " + stored + " : " + stored.getOhmSVE() + "\n\n");
        System.out.println("3 ----------");
        int iretSize = retrieved.getOhmSVE().size();
        int istoredSize = stored.getOhmSVE().size();
        assertEquals(iretSize, istoredSize);

        System.out.println("\nagregamos un nuevo objeto al hashmap ya inicializado");
        stored.getOhmSVE().put(new EdgeAttrib("nota 3", DateHelper.getCurrentDate()), new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");

        retrieved = sm.get(SimpleVertexEx.class, rid);

        System.out.println("retrieved: " + retrieved + " : " + retrieved.getOhmSVE() + "  hc: " + retrieved.hashCode());
        System.out.println("stored: " + stored + " : " + stored.getOhmSVE() + "  hc: " + stored.hashCode());

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
     * Rollback simple de los atributos
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
        assertEquals(sve.getF(), stored.getF(), 0.0002);
        assertEquals(sve.isB(), stored.isB());
        assertEquals(sve.getoI(), stored.getoI());
        assertEquals(sve.getoF(), stored.getoF(), 0.0002);

        System.out.println("haciendo rollback de un store.....");
        assertEquals(0, this.sm.getDirtyCount());
        sve = new SimpleVertexEx();
        sve.setS("ROLLBACK");
        stored = this.sm.store(sve);
        assertEquals(1, this.sm.getDirtyCount());
        this.sm.rollback();
        assertEquals(0, this.sm.getDirtyCount());
        
        final SimpleVertexEx fstored = stored;
        assertThrows(InvalidObjectReference.class, () -> fstored.setS("error!"));
        assertEquals(0, this.sm.getDirtyCount());
    }

    
    @Test
    public void persistEnum() throws Exception {
        Enums e = new Enums();
        e.setTheEnum(EnumTest.OTRO_MAS);
        e = sm.store(e);
        sm.commit();
        String rid = sm.getRID(e);
        
        sm.getCurrentTransaction().clearCache();
        e = sm.get(Enums.class, rid);
        assertEquals(EnumTest.OTRO_MAS, e.getTheEnum());
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

        SimpleVertexEx stored = sm.store(sve);
        System.out.println("guardando el objeto con 3 elementos en el AL.");
        sm.commit();
        System.out.println("\n\nSTORE FINALIZADO ========================= \n\n");
        String rid = sm.getRID(stored);

        // modificar los campos.
        stored.alSV.add(new SimpleVertex());
        System.out.println("\n\nINICIANDO ROLLBACK ========================= \n\n");
        sm.rollback();

        System.out.println("" + sve.alSV.size() + " =|= " + stored.alSV.size());
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
        stored.hmSV.put("key rollback", new SimpleVertex());

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

        SimpleVertexInterfaceAttr sv = new SimpleVertexInterfaceAttr("simple vertex with interface attr");
        SimpleVertexInterfaceAttr expResult = sv;

        assertEquals(0, sm.getDirtyCount());

        SimpleVertexInterfaceAttr result = sm.store(sv);

        assertEquals(2, sm.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);

        assertEquals(expResult.getS(), result.getS());

        this.sm.commit();
        assertEquals(0, sm.getDirtyCount());

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy) result).___getRid();
        expResult = this.sm.get(SimpleVertexInterfaceAttr.class, rid);

        assertEquals(0, sm.getDirtyCount());

        System.out.println("1 dirty: " + sm.getDirtyCount());
        System.out.println("hc: " + expResult.hashCode());
        System.out.println("2 dirty: " + sm.getDirtyCount());

        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);

        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());
        System.out.println("3 dirty: " + sm.getDirtyCount() + " " + sm.getCurrentTransaction().getDirtyCache());

        assertEquals(expResult.getI(), sv.getI());
        System.out.println("4 dirty: " + sm.getDirtyCount() + " " + sm.getCurrentTransaction().getDirtyCache());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals(expResult.getS(), sv.getS());
        System.out.println("5 dirty: " + sm.getDirtyCount());
        assertEquals(expResult.getoB(), sv.getoB());
        System.out.println("6 dirty: " + sm.getDirtyCount());
        assertEquals(expResult.getoF(), sv.getoF());
        System.out.println("7 dirty: " + sm.getDirtyCount());
        assertEquals(expResult.getoI(), sv.getoI());
        System.out.println("8 dirty: " + sm.getDirtyCount());

        System.out.println("\n\nverificar el comportamiento de las listas con objetos interfaces");

        sv = new SimpleVertexInterfaceAttr("simple vertex with interface list attr");
        sv.iList.add(new SimpleVertexWithImplement("1"));
        sv.iList.add(new SimpleVertexWithImplement("2"));

        System.out.println("persisir el objeto");

        SimpleVertexInterfaceAttr rsv = sm.store(sv);
        sm.commit();

        rid = sm.getRID(rsv);
        System.out.println("RID: " + rid);

        rsv = null;
        sv = null;

        System.out.println("limpiar el cache...");
        sm.getCurrentTransaction().clearCache();
        System.out.println("recupear el objeto nuevamente...");
        rsv = sm.get(SimpleVertexInterfaceAttr.class, rid);

        assertEquals(2, rsv.iList.size());
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

        String rid = ((IObjectProxy) result).___getRid();

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

        // verificar el rollback con embedded.
        // agrego una string a la lista
        System.out.println("Verificando rollback....");
        SimpleVertexWithEmbedded retRollback = this.sm.get(SimpleVertexWithEmbedded.class, rid);
        int size = retRollback.getStringlist().size();
        System.out.println("Elementos en la lista: " + size);
        retRollback.getStringlist().add("rollback");
        System.out.println("Elementos en la lista después de agregar uno para el rollback: " + retRollback.getStringlist().size());
        this.sm.rollback();
        assertEquals(0, sm.getDirtyCount());
        assertEquals(size, retRollback.getStringlist().size());

        System.out.println("==========================================================");
    }

    @Test
    public void testEmbeddedRollback() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback sobre un objeto que aún no se persistió con commit");
        System.out.println("y tiene colecciones");
        System.out.println("***************************************************************");

        // test rollback
        UserSID usidRollback = new UserSID("rollback", "rollback");
        UserSID rusid = this.sm.store(usidRollback);

        System.out.println("Haciendo rollback...");
        this.sm.rollback();
        try {

            System.out.println("setName");
            rusid.setName("fail");
            System.out.println("getGroups...");
            List l = rusid.getGroups();
            fail("El objeto existe aún después de haberse hecho un rollback");
        } catch (Exception e) {

        }
        System.out.println("===========================================================");
    }

    /*
     * Test de Transacciones privadas múltiples.
     */
    @Test
    public void testTransaction() throws Exception {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Transacción múltiples privadas");
        System.out.println("***************************************************************");

        Transaction t1 = this.sm.getTransaction();
        Transaction t2 = this.sm.getTransaction();

        SimpleVertex sv = new SimpleVertex();
        SimpleVertex expResult = sv;

        assertEquals(0, t1.getDirtyCount());

        SimpleVertex result = t1.store(sv);

        assertEquals(1, t1.getDirtyCount());
        assertTrue(result instanceof IObjectProxy);

        assertEquals(expResult.i, result.i);

        t1.commit();
        assertEquals(0, t1.getDirtyCount());

        System.out.println("Recuperar el objeto de la base a traves de una Transacción");
        String rid = ((IObjectProxy) result).___getRid();
        System.out.println("RID: " + rid);

        expResult = t1.get(SimpleVertex.class, rid);

        assertEquals(0, t1.getDirtyCount());

        // verificar que el resultado implemente la interface 
        assertTrue(expResult instanceof IObjectProxy);

        // verificar que todos los valores sean iguales
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());

        assertEquals(expResult.getI(), sv.getI());
//        assertEquals((float)expResult.getF(), (float)sv.getF());
        assertEquals(expResult.getS(), sv.getS());
        assertEquals(expResult.getoB(), sv.getoB());
        assertEquals(expResult.getoF(), sv.getoF());
        assertEquals(expResult.getoI(), sv.getoI());

        // recuperar el mismo registro desde la otra Transacción
        SimpleVertex expResultT2 = t2.get(SimpleVertex.class, rid);

        // modificar el objeto en la T1
        expResult.setS("modificado en t1");

        assertNotEquals(t1.getDirtyCount(), t2.getDirtyCount());

        // hacer un commit en T1 y provocar la falla en T2
        t1.commit();

        System.out.println("Desde T1: " + expResult.getS());
        System.out.println("Desde T2: " + expResultT2.getS());

        expResultT2.setoI(2);

        ConcurrentModification ex = assertThrows(
                ConcurrentModification.class, () -> t2.commit());
        assertTrue(ex.canRetry());
        System.out.println(ex);
        System.out.println("Commit en T2");
        System.out.println("Desde T2: " + expResultT2.getS());
        
        //verificar que no quedó transacción abierta
        assertNull(orientdbTransactField.get(t2));
    }

    /**
     * Test of delete method, of class SessionManager.
     */
    @Test
    public void testDelete() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("delete objetos");
        System.out.println("***************************************************************");

        SimpleVertex sv = new SimpleVertex();
        SimpleVertex expResult = sv;

        assertEquals(0, sm.getDirtyCount());

        SimpleVertex result = sm.store(sv);

        this.sm.commit();

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy) result).___getRid();
        expResult = this.sm.get(SimpleVertex.class, rid);

        System.out.println("Eliminar el objeto: " + rid);
        sm.delete(expResult);
        sm.commit();

        try {
            sm.get(rid);
            fail("El objeto aún exite!!!");
        } catch (UnknownRID urid) {
            System.out.println("El objeto fue borrado!");
        }

        System.out.println("Testeando ingegridad referencial...");

        // crear un objeto simple.
        SimpleVertex irSV = sm.store(new SimpleVertex());
        sm.commit();
        String irSVrid = sm.getRID(irSV);

        // crear el objeto que referenciará al primero
        SimpleVertexEx irSVEX = new SimpleVertexEx();
        irSVEX.setSvinner(irSV);

        SimpleVertexEx rirSVEX = sm.store(irSVEX);
        String rirSVEXrid = sm.getRID(rirSVEX);

        System.out.println("Referencia creada: " + rirSVEXrid + "-->" + irSVrid);
        // liberar la referencia
        irSVEX = null;
        sm.commit();

        // intentar eliminar el objeto dependiente
        try {
            sm.delete(irSV);
            sm.commit();
            fail("El objeto fue borrado y debería haber saltado una excepción");
        } catch (ReferentialIntegrityViolation riv) {
            System.out.println("ReferencialIntegrityViolation ");
            
            System.out.println("\n\nllamando a ROLLBACK...");
            sm.rollback();
            System.out.println("dirtyDeleted: "+sm.getCurrentTransaction().getDirtyDeletedCount());
        }

        System.out.println("*************************");
        System.out.println("Verificando el comportamiento de la auditoría con objetos borrados");
        System.out.println("*************************");
        sm.setAuditOnUser("DeleteAudit");
        SimpleVertex svaudit = new SimpleVertex("DeleteAudit");
        SimpleVertex rsva = sm.store(svaudit);
        sm.commit();
        String svaRID = sm.getRID(rsva);
        System.out.println("RID: " + svaRID);
        svaudit = null;
        System.out.println("Eliminando el objeto...");
        sm.delete(rsva);
        sm.commit();
        try {
            sm.get(svaRID);
            fail("El objeto aún existe!!!");
        } catch (Exception e) {
            System.out.println("Todo ok!");
            
        }
        
        spaces(5);
        System.out.println("*************************");
        System.out.println("Verificando el comportamiento de CascadeDelete");
        System.out.println("*************************");
        System.out.println("\n\n--- En Listas ---");
        SimpleVertexEx cdsve = new SimpleVertexEx();
        cdsve.setS("CascadeDelete");
        cdsve.initArrayList();

        SimpleVertexEx csve = sm.store(cdsve);
        System.out.println("commit...");
        sm.commit();
        System.out.println("fin commit.");

        String csveRid = sm.getRID(csve);
        System.out.println("RID: " + csveRid);
        System.out.println("Referencias de objetos en el AL:");
        ArrayList<String> alRid = new ArrayList<>();
        for (SimpleVertex simpleVertex : csve.getAlSV()) {
            String srid = sm.getRID(simpleVertex);
            alRid.add(srid);
            System.out.println(srid);
        }
        spaces(4);
        System.out.println("Eliminar el objeto raíz");
        sm.delete(csve);
        sm.commit();
        System.out.println("Verificar que todo esté ok");

        try {
            sm.get(csveRid);
            fail("El objeto aún existe!!!");
        } catch (Exception e) {
            System.out.println("Todo ok!");
        }

        System.out.println("Verificar los CascadeDelete...");
        for (String object : alRid) {
            try {
                sm.get(object);
                fail("El objeto " + object + " aún existe!!!");
            } catch (Exception e) {
                System.out.println("Todo ok!");
            }
        }

        System.out.println("Verificando el borrado de objetos que han sido modificados durante la operación...");
        SimpleVertex svmodificado_a_borrar = sm.store(new SimpleVertex());
        SimpleVertexEx svExConVector = sm.store(new SimpleVertexEx());
        svExConVector.initArrayList();

        sm.commit();

        String svModif = sm.getRID(svmodificado_a_borrar);
        String svExCon = sm.getRID(svExConVector);

        System.out.println("svExCon " + svExCon);
        System.out.println("svModif " + svModif);

        System.out.println("agregando el sv al SVEx...");
        svExConVector.alSV.add(svmodificado_a_borrar);
        System.out.println("size: " + svExConVector.alSV.size());

        System.out.println("commit...");
        sm.commit();
        System.out.println("size: " + svExConVector.alSV.size());

        // liberar los objetos.
        svmodificado_a_borrar = null;
        svExConVector = null;

        // recupear de la base
        System.out.println("recuperar nuevamente...");
        svExConVector = sm.get(SimpleVertexEx.class, svExCon);
        System.out.println("size: " + svExConVector.alSV.size());

        for (SimpleVertex simpleVertex : svExConVector.alSV) {
            System.out.println(":: " + sm.getRID(simpleVertex));
        }
        // recupear el objeto desde el cache. Si se recuera desde la base de datos
        // se obtienen dos instancias y al realizar el borrado por un lado y la modificación
        // por otro, aparece una inconsistencia dependiendo de como lo acomode el hm dirty.
        svmodificado_a_borrar = sm.get(SimpleVertex.class, svModif);

        System.out.println("realizando una modificación previo al borrado");
        svmodificado_a_borrar.setS("modificado previo borrado");

        System.out.println("borrando...");
        svExConVector.alSV.remove(svmodificado_a_borrar);
        System.out.println("size: " + svExConVector.alSV.size());

        System.out.println("realizando commit...");
        sm.commit();
        svmodificado_a_borrar = null;
        svExConVector = null;

        try {
            svmodificado_a_borrar = sm.get(SimpleVertex.class, svModif);
            fail("El objeto aún existe!!! ");
        } catch (Exception e) {
            System.out.println("todo ok.");
        }

        spaces(5);
        System.out.println("--- En Maps ---");
    }
    
    
    @Test
    public void testAudit() throws Exception {
        sm.setAuditOnUser("test-user");
        assertTrue(sm.isAuditing());
        
        SimpleVertexEx sv = sm.store(new SimpleVertexEx());
        sm.commit();
        String rid = sm.getRID(sv);
        System.out.println("RID: " + rid);
        
        String query = String.format("select count(*) from ODBAuditLog where rid = '%s'", rid);
        long logs = sm.query(query, "");
        assertEquals(1, logs); //store log
        
        sm.getTransaction().clearCache();
        sv = sm.get(SimpleVertexEx.class, rid);
        sv.initArrayList();
        sm.commit();
        
        logs = sm.query(query, "");
        assertEquals(6, logs); //store, read, update and 3 added edges logs
        
        sm.setAuditOnUser(null);
        assertFalse(sm.isAuditing());
    }
    

    /**
     * Test of delete method, of class SessionManager.
     */
    @Test
    public void testRemoveOrphan() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("RemoveOrphan");
        System.out.println("***************************************************************");

        SimpleVertexEx sve = new SimpleVertexEx();
        sve.setSvinner(new SimpleVertex());

        SimpleVertex result = sm.store(sve);

        this.sm.commit();

        System.out.println("Recuperar el objeto de la base");
        String rid = ((IObjectProxy) result).___getRid();
        // liberar el objeto...
        System.out.println("Liberar el objeto  " + result + "...");
        result = null;

        SimpleVertexEx expResult = this.sm.get(SimpleVertexEx.class, rid);
        System.out.println("Nueva referencia: " + expResult);

        SimpleVertex sv = expResult.getSvinner();
        String svrid = this.sm.getRID(sv);
        System.out.println("Objeto referenciado: " + sm.getRID(sv));
        System.out.println("Eliminar la referencia");
        expResult.setSvinner(null);

        System.out.println("commit...");
        sm.commit();

        try {
            sm.get(svrid);
            fail("El objeto aún exite!!!");
        } catch (UnknownRID urid) {
            System.out.println("El objeto fue borrado!");
        }

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("**********************************");
        System.out.println("Borrar el objeto raíz y verificar que el objeto dependiente se ha borrado");
        System.out.println("**********************************");

        SimpleVertexEx svOuter = new SimpleVertexEx();
        svOuter.setSvinner(new SimpleVertex("toBeRemovedByOrphan"));
        SimpleVertexEx rsvo = sm.store(svOuter);
        svOuter = null;
        System.out.println("Guardando el objeto.......");
        sm.commit();

        System.out.println("");
        System.out.println("");
        System.out.println("");
        String srvso = sm.getRID(rsvo);
        String srsvinner = sm.getRID(rsvo.getSvinner());
        System.out.println("RID: " + srvso);
        System.out.println("RID svinner: " + srsvinner);

        System.out.println("----------------------------------");
        System.out.println("Eliminar el objeto...");
        sm.delete(rsvo);
        sm.commit();
        System.out.println("----------------------------------");
        try {
            sm.get(srvso);
            fail("El objeto padre aún exite!!!");
        } catch (UnknownRID urid) {
            System.out.println("El objeto padre fue borrado!");
        }
        try {
            sm.get(srsvinner);
            fail("El objeto Orphan aún exite!!!");
        } catch (UnknownRID urid) {
            System.out.println("El objeto Orphan fue borrado!");
        }

        System.out.println("\n\nVerificar el RemoveOrphan sobre los vectores");
        SimpleVertexEx svro = new SimpleVertexEx();
        SimpleVertexEx storedSVE = sm.store(svro);
        sm.commit();

        String ridRO = sm.getRID(storedSVE);

        svro = null;
        storedSVE = null;

        SimpleVertexEx rirSVEX = sm.get(SimpleVertexEx.class, ridRO);
        svro = null;

        System.out.println("rid principal: " + sm.getRID(rirSVEX));

        // persistir todo.
        System.out.println("inicializar el array list...");
        rirSVEX.initArrayList();
        sm.commit();

        rirSVEX = sm.get(SimpleVertexEx.class, sm.getRID(rirSVEX));
        String sRSV1 = sm.getRID(rirSVEX.getAlSV().get(0));
        SimpleVertex svToRemove = rirSVEX.getAlSV().get(1);
        String sRSV2 = sm.getRID(svToRemove);
        System.out.println("rid sv1: " + sRSV1);
        System.out.println("rid sv2: " + sRSV2);

        System.out.println("fin de la presistencia con los objetos referenciados");

        System.out.println("quitar uno de los objetos...");
        System.out.println("resultado: " + rirSVEX.getAlSV().remove(svToRemove));

        System.out.println("persistir...");
        sm.commit();

        System.out.println("verificar que el objeto no exista");
        try {
            SimpleVertex rsv1borrado = sm.get(SimpleVertex.class, sRSV1);
        } catch (UnknownRID urid) {
            System.out.println("Exito! El objeto fue borrado.");
        }

    }

    /**
     * Test Rollback on exception
     */
    @Test
    public void testRollbackOnException() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("test Rollback on Exception");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        String uuidExitente = sve.getUuid();
        System.out.println("uuid: "+uuidExitente);
        sve.setS("otro string");
        sve.setI(1000);
        sve.setSvex("otro svex");
        SimpleVertexEx ssve = sm.store(sve);
        
        System.out.println("ssve uuid: "+ssve.getUuid());
        System.out.println("commit...");
        sm.commit();
        String origRID = sm.getRID(ssve);
        System.out.println("ssve rid: "+origRID);
        System.out.println("ssve uuid: "+ssve.getUuid());
        
        System.out.println("Eliminar las referencias...");
        ssve = null;
        sve = null;
        sm.getCurrentTransaction().removeFromCache(origRID);
        
        // crear un nuevo SVE y duplicar el uuid
        System.out.println("\n1. Duplicando...");
        SimpleVertexEx dup = new SimpleVertexEx();
        dup.setUuid(uuidExitente);
        System.out.println("asignamos el UUIDs Exitente: " + uuidExitente);
        
        // intentar almacenar
        try {
            System.out.println("2. Haciendo el store...");
            SimpleVertexEx sDup = sm.store(dup);
            System.out.println("sDup.uuid: "+sDup.getUuid());
            System.out.println("3. haciendo commit");
            
            sm.commit();
            System.out.println("sDup rid: "+sm.getRID(sDup));
            System.out.println("sDup.uuid: "+sDup.getUuid());
            fail("FAIL! No se detectó la excepción!!!!");
        } catch (Exception e) {
            System.out.println("4. Excepcion!!! ");
            System.out.println("5. invocando a rollback...");
            sm.rollback();
            System.out.println("6. Finalizado!");
        }
        System.out.println("7. Objetos marcados: " + sm.getDirtyCount());

        System.out.println("8. Probando sobre un objeto existente ==============");
        // probar el error sobre un objeto que ya está administrado por el ogm.
        System.out.println("UUIDs Exitente: " + uuidExitente);
        SimpleVertexEx dup2 = new SimpleVertexEx();
        System.out.println("UUIDs Nuevo   : " + dup2.getUuid());

        SimpleVertexEx sDup2 = sm.store(dup2);
        String currentUUID = sDup2.getUuid();
        String rid = ((IObjectProxy) sDup2).___getRid();

        System.out.println("RID: " + rid);
        System.out.println("current UUID: " + currentUUID);
        System.out.println("Es válido: " + ((IObjectProxy) sDup2).___isValid());
        System.out.println("9. Objetos marcados: " + sm.getDirtyCount());
        System.out.println("10. haciendo commit");
        
        sm.commit();
        
        rid = ((IObjectProxy) sDup2).___getRid();
        System.out.println("RID: " + rid);
        System.out.println("current UUID: " + sDup2.getUuid());

        System.out.println("\n11. cambiando a un uuid existente");
        sDup2.setUuid(uuidExitente);
        // intentar almacenar
        try {
            System.out.println("12. haciendo commit del objeto existente");
            sm.commit();
        } catch (Exception e) {
            System.out.println("13. Excepcion!!! ");
            System.out.println("14. invocando a rollback...");
            sm.rollback();
        }
        System.out.println(currentUUID + " =<>= " + sDup2.getUuid());
        assertEquals(currentUUID, sDup2.getUuid());
        System.out.println("15. Finalizado!");
    }

    @Test
    public void testIndirect() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Probar las conecciones Indirectas.");
        System.out.println("***************************************************************");
        // limpiar el cache.
        sm.getCurrentTransaction().clearCache();

        // verificar que cuando se recuepera in indirectlinked desde el objeto padre
        // la indirección apunte exactamente a la instancia ya existente.
        IndirectObject ioPadre = new IndirectObject();
        IndirectObject ioIndirecto = new IndirectObject();

        ioPadre.setDirectLink(ioIndirecto);

        ioPadre = sm.store(ioPadre);

        System.out.println("commit");
        sm.commit();

        String ridPadre = sm.getRID(ioPadre);
        System.out.println("ridPadre: " + ridPadre);
        System.out.println("ObjectCache: " + sm.getCurrentTransaction().getObjectCache());

        // liberar las referencias.
        ioPadre = null;
        ioIndirecto = null;

        sm.getCurrentTransaction().removeFromCache(ridPadre);
        System.out.println("gc");
        System.gc();

//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(SessionManagerTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("ObjectCache: " + sm.getCurrentTransaction().getObjectCache());

        // recupear el padre nuevamente desde la base de datos.
        ioPadre = sm.get(IndirectObject.class, ridPadre);
        ioIndirecto = ioPadre.getDirectLink();

        // la referencia indirecta en ioIndirecto debe ser a ioPadre tanto en el nro de vertice con el la referncia a memoria.
        System.out.println("ObjectCache: " + sm.getCurrentTransaction().getObjectCache());
        int ioPadreIdent = System.identityHashCode(ioPadre);
        int ioIndirectPadreIdent = System.identityHashCode(ioIndirecto.getIndirectLink());
        System.out.println("Padre: " + sm.getRID(ioPadre) + " ref: " + ioPadreIdent + " ---> " + sm.getRID(ioPadre.getDirectLink()));
        System.out.println("Indirecto: " + sm.getRID(ioIndirecto)
                + " <--- "
                + sm.getRID(ioIndirecto.getIndirectLink())
                + " ref: " + ioIndirectPadreIdent);

        assertEquals(ioPadreIdent, ioIndirectPadreIdent);

        sm.getCurrentTransaction().removeFromCache(sm.getRID(ioPadre));
        sm.getCurrentTransaction().removeFromCache(sm.getRID(ioIndirecto));
        ioPadre = null;
        ioIndirecto = null;

        System.gc();
        System.out.println(sm.getCurrentTransaction().getObjectCache());
        System.out.println("Probar la indirección a partir del objeto indirecto y verificar que llega al padre.");
        System.out.println("Crear dos nuevos objetos");
        IndirectObject io = new IndirectObject();
        IndirectObject ioLinked = new IndirectObject();

        ioLinked.setDirectLink(io);

        IndirectObject sioLinked = sm.store(ioLinked);
        System.out.println("precommit:" + sm.getCurrentTransaction().getObjectCache());
        sm.commit();
        System.out.println("postcommit:" + sm.getCurrentTransaction().getObjectCache());
        String dLinked = sm.getRID(sioLinked);

        // liberar los objetos
        io = null;
        ioLinked = null;
        sioLinked = null;

//        System.gc();
        System.out.println("recuperar nuevamente el registro " + dLinked);
        IndirectObject rioLinked = sm.get(IndirectObject.class, dLinked);

        System.out.println("DM: " + ((IObjectProxy) rioLinked).___isDirty());
        System.out.println("fin commit.");

        System.out.println("1 - dc: " + sm.getCurrentTransaction().getDirtyCache());
        System.out.println("DM: " + ((IObjectProxy) rioLinked).___isDirty());
        System.out.println("DM: " + ((IObjectProxy) rioLinked).___isDirty());
        System.out.println("2 - dc: " + sm.getCurrentTransaction().getDirtyCache());
        System.out.println("    oc :" + sm.getCurrentTransaction().getObjectCache());
        String inLinked = sm.getRID(rioLinked.getDirectLink());
        System.out.println("3 - dc: " + sm.getCurrentTransaction().getDirtyCache());

        System.out.println("Linked RID: " + dLinked + "(" + System.identityHashCode(rioLinked)
                + ") ----> " + inLinked + "(" + System.identityHashCode(rioLinked.getDirectLink()) + ")");
        System.out.println(sm.getCurrentTransaction().getDirtyCache());

        rioLinked = null;

        System.out.println("\ngc...");
        System.gc();
        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        System.out.println("\nrecuperar el objeto nuevamente");
        IndirectObject sioIndirectLinked = sm.get(IndirectObject.class, inLinked);
        System.out.println(sm.getRID(sioIndirectLinked) + " indirectLinked to RID: " + sm.getRID(sioIndirectLinked.getIndirectLink()));
        assertNotNull(sioIndirectLinked.getIndirectLink());

        // test sobre los ArrayList
        System.out.println("");
        System.out.println("Test sobre los Arrays");
        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        IndirectObject ioAlLinked1 = new IndirectObject();
        IndirectObject ioAlLinked2 = new IndirectObject();
        ioAlLinked1.getAlDirectLinked().add(sioIndirectLinked);
        ioAlLinked2.getAlDirectLinked().add(sioIndirectLinked);

        sm.store(ioAlLinked1);
        sm.store(ioAlLinked2);
        sm.commit();

        ioAlLinked1 = null;
        ioAlLinked2 = null;
        sioIndirectLinked = null;

        System.out.println("limpiar la memoria...");
//        System.gc();
        System.out.println("oc: " + sm.getCurrentTransaction().getObjectCache());
        System.out.println("dc: " + sm.getCurrentTransaction().getDirtyCache());

        // refrescar el objeto indirecto
        System.out.println("recuperar nuevamente el objeto: " + inLinked);
        sioIndirectLinked = sm.get(IndirectObject.class, inLinked);
        assertEquals(2, sioIndirectLinked.getAlIndirectLinked().size());

        // Test sobre los HashMap
        System.out.println("");
        System.out.println("Test sobre los HashMap");
        IndirectObject ioHMLinked1 = new IndirectObject();
        IndirectObject ioHMLinked2 = new IndirectObject();

        ioHMLinked1.getHmDirectLinked().put("1", sioIndirectLinked);
        ioHMLinked2.getHmDirectLinked().put("2", sioIndirectLinked);

        ioHMLinked1 = sm.store(ioHMLinked1);
        ioHMLinked2 = sm.store(ioHMLinked2);

        sm.commit();

        System.out.println("Direct HM 1: " + sm.getRID(ioHMLinked1));
        System.out.println("Direct HM 2: " + sm.getRID(ioHMLinked2));

        // refrescar el objeto indirecto
        sioIndirectLinked = null;

        System.out.println("limpiar la memoria...");
//        System.gc();

        sioIndirectLinked = sm.get(IndirectObject.class, inLinked);
        assertEquals(sioIndirectLinked.getHmIndirectLinked().size(), 2);

        //=====================================================
        // crear un objeto que aputa a otros dos a través de un AL.
        // Los objeto que es indirectamente referenciado debe mantener 
        // la consistencia con el objeto origen de las referencias.
        // o ------> o1
        //   \-----> o2
        //   \-----> o3
        //
        IndirectObject origen = new IndirectObject();

        IndirectObject ind1 = new IndirectObject();
        IndirectObject ind2 = new IndirectObject();
        IndirectObject ind3 = new IndirectObject();

        origen.getAlDirectLinked().add(ind1);
        origen.getAlDirectLinked().add(ind2);
        origen.getAlDirectLinked().add(ind3);

        // guardar todo
        IndirectObject sOrigen = sm.store(origen);
        sm.commit();
        System.out.println("hc: " + System.identityHashCode(sOrigen));

        String origenRID = sm.getRID(sOrigen);

        // dereferenciar todo.
        System.out.println("limpiar la memoria...");
        ind1 = null;
        ind2 = null;
        ind3 = null;
        origen = null;
        sOrigen = null;
        System.gc();
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(SessionManagerTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("cache: " + sm.getCurrentTransaction().getObjectCache());
        // recupear el origen
        origen = sm.get(IndirectObject.class, origenRID);
        System.out.println("hc: " + System.identityHashCode(origen));

        origen.setTestData("modificado");
        System.out.println("cache: " + sm.getCurrentTransaction().getObjectCache());

        ind1 = origen.getAlDirectLinked().get(0);
        String ind1RID = sm.getRID(ind1);
        ind2 = origen.getAlDirectLinked().get(1);
        String ind2RID = sm.getRID(ind2);
        ind3 = origen.getAlDirectLinked().get(2);
        String ind3RID = sm.getRID(ind3);

        System.out.println("verificando que no sean null la referencias indirectas...");
        assertNotNull(ind1.getIndirectLinkedFromAL());
        assertNotNull(ind2.getIndirectLinkedFromAL());
        assertNotNull(ind3.getIndirectLinkedFromAL());
        System.out.println("verificar que todas apunten al mismo objeto...");
        int iOrigen = System.identityHashCode(origen);
        int iInd1 = System.identityHashCode(ind1.getIndirectLinkedFromAL());
        int iInd2 = System.identityHashCode(ind2.getIndirectLinkedFromAL());
        int iInd3 = System.identityHashCode(ind3.getIndirectLinkedFromAL());

        assertEquals(iOrigen, iInd1);
        assertEquals(iOrigen, iInd2);
        assertEquals(iOrigen, iInd3);
        assertEquals(ind1.getIndirectLinkedFromAL().getTestData(), ind3.getIndirectLinkedFromAL().getTestData());

        //-----------------------------------------------------
        // dereferenciar todo nuevamente
        ind1 = null;
        ind2 = null;
        origen = null;

        // ahora recupear un objeto indirecto y desde éste recuper el origen. 
        ind1 = sm.get(IndirectObject.class, ind1RID);

        origen = ind1.getIndirectLinkedFromAL();
        origen.setTestData("modif2");

        ind2 = sm.get(IndirectObject.class, ind2RID);

        assertEquals(ind1.getIndirectLinkedFromAL().getTestData(), ind2.getIndirectLinkedFromAL().getTestData());

        //=====================================================
    }

    @Test
    public void testTransactionCache() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Probar el cache de transacción");
        System.out.println("***************************************************************");

        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx ssve = sm.store(sve);
        System.out.println("" + sm.getCurrentTransaction().getObjectCache());
        sm.commit();
        ssve.setS("Referencia");
        System.out.println("" + sm.getCurrentTransaction().getObjectCache());
        String rid = sm.getRID(ssve);

        System.out.println("" + sm.getCurrentTransaction().getObjectCache());
        // obtener el objeto desde la transacción en curso. Debería ser el mismo que ssve.
        SimpleVertexEx rsve = sm.get(SimpleVertexEx.class, rid);

        System.out.println("ssve: " + System.identityHashCode(ssve));
        System.out.println("ssve: " + System.identityHashCode(rsve));

        assertEquals(ssve.getS(), rsve.getS());

    }

    @Test
    public void speedTest() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Test de velocidad");
        System.out.println("***************************************************************");

        long ldtInit = System.currentTimeMillis();
        List<SimpleVertex> lsv = sm.query(SimpleVertex.class);
        long ldtEnd = System.currentTimeMillis();
        System.out.println("enlapsed: " + (ldtEnd - ldtInit) + " Objects: " + lsv.size());

        String rid = sm.getRID(lsv.get(0));
        ldtInit = System.currentTimeMillis();
        SimpleVertex lsvo = sm.get(SimpleVertex.class, rid);
        ldtEnd = System.currentTimeMillis();
        System.out.println("enlapsed: " + (ldtEnd - ldtInit));

        ldtInit = System.currentTimeMillis();
        List<SimpleVertexEx> lsve = sm.query(SimpleVertexEx.class);
        ldtEnd = System.currentTimeMillis();
        System.out.println("enlapsed: " + (ldtEnd - ldtInit) + " Objects: " + lsve.size());
    }

//    @Test
    public void testObjectCache() throws Exception {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Probar el cache de objetos SimpleCache");
        System.out.println("***************************************************************");
        SimpleCache sc = new SimpleCache();
        sc.setTimeInterval(1);
        SimpleVertex sv1 = new SimpleVertex();
        SimpleVertex sv2 = new SimpleVertex();
        SimpleVertex sv3 = new SimpleVertex();
        sc.add("1", sv1);
        sc.add("2", sv2);
        sc.add("3", sv3);
        assertEquals(3, sc.size());
        System.out.println("1: " + sc.getCachedObjects());

        Object o = sc.get("1");
        System.out.println("class: " + o.getClass().getSimpleName());

        System.out.println("Dereferenciar el objeto 2");
        sv2 = null;
        System.gc();
        Thread.sleep(3000);
        assertEquals(2, sc.size());
        System.out.println(": " + sc.getCachedObjects());

        System.out.println("Dereferenciar el objeto 3");
        sv3 = null;
        System.gc();
        Thread.sleep(3000);
        assertEquals(1, sc.size());
        System.out.println(": " + sc.getCachedObjects());
    }
    
    /*
     * Sólo prueba que se pueda ir commiteando de a cada tanto.
     */
    @Test
    public void multipleCommits() throws Exception {
        SimpleVertexEx sv = new SimpleVertexEx();
        sv.lSV = new ArrayList<>();
        sv = sm.store(sv);
        sm.commit();
        String rid = sm.getRID(sv);
        System.out.println("RID: " + rid);
        
        sv.lSV.add(new SimpleVertex());
        sm.commit();
        
        sv.lSV.add(new SimpleVertex());
        sm.commit();
        
        sv.lSV.add(new SimpleVertex());
        sm.commit();
        
        sm.getCurrentTransaction().clearCache();
        sv = sm.get(SimpleVertexEx.class, rid);
        assertEquals(3, sv.lSV.size());
    }

//    @Test
//    public void testTransactions() {
//        try {
//            System.out.println("\n\n\n");
//            System.out.println("***************************************************************");
//            System.out.println("múltiples transacciones en paralelo.");
//            System.out.println("***************************************************************");
//            
//            SimpleVertexEx s1 = new SimpleVertexEx();
//            s1.setS("Transaction 1");
//            SimpleVertexEx s2 = new SimpleVertexEx();
//            s2.setS("Transaction 2");
//            
//            Transaction t1 = sm.getTransaction();
//            Transaction t2 = sm.getTransaction();
//            
//            // verificar que sean objetos distintos
//            assertNotEquals(t1.getGraphdb(), t2.getGraphdb());
//            
//            // persistir un objeto en cada transacción
//            System.out.println("Store de s1");
////            SimpleVertexEx ms1 = t1.store(s1);
////            assertEquals(1, t1.getDirtyCount());
//            
//            System.in.read();
//            
//            System.out.println("Store de s2");
//            SimpleVertexEx ms2 = t2.store(s2);
//            assertEquals(1, t2.getDirtyCount());
//            
//            System.out.println("RID S2: "+sm.getRID(ms2));
//            
//            // hacer commit en t1 y rollback en t2
////            System.out.println("commit en T1");
////            t1.commit();
////            System.in.read();
//            
//            System.out.println("rollback en t2");
//            t2.rollback();
//            
//            // ambas transacciones deben estar en 0
//            assertEquals(t1.getDirtyCount(), t2.getDirtyCount());
//        } catch (IOException ex) {
//            Logger.getLogger(SessionManagerTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    
//        
//    }
    
    private void spaces(int lines) {
        for (int j = 0; j < lines; j++) {
            System.out.println("");
        }
    }
    
    
    @Test
    public void testStoreRollbacked() throws Exception {
        SimpleVertexEx s1 = new SimpleVertexEx();
        s1 = sm.store(s1);
        String rid = sm.getRID(s1);
        assertNotNull(rid);
        System.out.println("RID: " + rid);
        sm.rollback();
        
        final SimpleVertexEx s1f = s1;
        Exception ex = assertThrows(InvalidObjectReference.class,
                new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                s1f.setS("modificado pero fue rollbacked antes");
            }
        });
    }
    
    
    @Test
    public void getInexistente() throws Exception {
        assertThrows(UnknownRID.class, () -> sm.get("lalala"));
    }
    
    
    @Test
    public void closeWithoutOpen() throws Exception {
        sm.getTransaction().closeInternalTx();
    }
    
    
    /*
     * Testea que cierre correctamente la transacción a la base después de
     * cada operación.
     */
    @Test
    public void closeTransactions() throws Exception {
        Transaction t = sm.getCurrentTransaction();
        assertNull(orientdbTransactField.get(t));
        
        SimpleVertexEx s1 = new SimpleVertexEx();
        s1 = t.store(s1);
        //mientras no comitee, el store mantiene la transacción
        assertNotNull(orientdbTransactField.get(t));
        //luego sí debe cerrarla
        t.commit();
        assertNull(orientdbTransactField.get(t));
        
        s1.setS("modificado");
        t.commit();
        assertNull(orientdbTransactField.get(t));
        
        t.refreshObject(s1);
        assertNull(orientdbTransactField.get(t));
        
        t.delete(s1);
        assertNull(orientdbTransactField.get(t));
        t.commit();
        assertNull(orientdbTransactField.get(t));
    }
    
    
    @Test
    public void finalizeTransactionsWithException() throws Exception {
        Transaction t = sm.getCurrentTransaction();
        try {
            t.get("unknown");
        } catch (UnknownRID ex) {
        }
        assertNull(orientdbTransactField.get(t));
    }
    
    
    /*
     * Testea que ante cualquier falla en el commit con objetos nuevos, se pueda
     * reintentar luego exitosamente.
     */
    @Test
    public void retryCommitNewObjects() throws Exception {
        Transaction t = sm.getCurrentTransaction();
        SimpleVertexEx s1 = new SimpleVertexEx();
        s1 = t.store(s1);
        assertTrue(((IObjectProxy)s1).___getVertex().getIdentity().isNew());
        
        //para simular una falla uso un mock:
        OrientGraph db = EasyMock.createNiceMock(OrientGraph.class);
        db.commit();
        EasyMock.expectLastCall().andThrow(new NotImplementedException());
        EasyMock.replay(db);
        
        orientdbTransactField.set(t, db);
        
        try { t.commit(); } catch (Exception ex) { /*falló, reintentar*/ }
        
        assertTrue(((IObjectProxy)s1).___getVertex().getIdentity().isNew());
        orientdbTransactField.set(t, null);
        t.commit();
        assertFalse(((IObjectProxy)s1).___getVertex().getIdentity().isNew());
    }
    
    
    /*
     * Testea que se pueda reintentar un commit con objetos modificados.
     */
    @Test
    public void retryCommit() throws Exception {
        SimpleVertexEx sv = new SimpleVertexEx();
        sv = sm.store(sv);
        sm.commit();
        String rid = sm.getRID(sv);
        
        Transaction t1 = sm.getTransaction();
        SimpleVertexEx s1 = t1.get(SimpleVertexEx.class, rid);
        
        Transaction t2 = sm.getTransaction();
        SimpleVertexEx s2 = t2.get(SimpleVertexEx.class, rid);

        s1.setS("en tran 1");
        t1.commit();
        
        s2.setS("en tran 2");
        assertThrows(ConcurrentModification.class, () -> t2.commit());
        
        //reintento
        t2.commit();
        
        //ver si se guardó correctamente el cambio de t2
        t2.clearCache();
        s2 = t2.get(SimpleVertexEx.class, rid);
        assertEquals("en tran 2", s2.getS());
        t1.clearCache();
        s1 = t1.get(SimpleVertexEx.class, rid);
        assertEquals("en tran 2", s1.getS());
    }
    
    
    /*
     * * Testea que se pueda reintentar un commit con eliminados.
     */
    @Test
    public void retryCommitDeleted() throws Exception {
        Transaction t = sm.getCurrentTransaction();
        SimpleVertexEx sv = new SimpleVertexEx();
        sv = t.store(sv);
        t.commit();
        String rid = sm.getRID(sv);
        
        //para simular una falla uso un mock:
        OrientGraph db = EasyMock.createNiceMock(OrientGraph.class);
        db.commit();
        EasyMock.expectLastCall().andThrow(new NotImplementedException());
        EasyMock.replay(db);
        orientdbTransactField.set(t, db);
        
        t.delete(sv);
        try { t.commit(); } catch (Exception ex) { /*falló, reintentar*/ }
        
        //comprobar que no se borró todavía de la base
        assertNotNull(sm.getTransaction().get(rid));
        
        //reintentar
        orientdbTransactField.set(t, null);
        t.commit();
        
        assertThrows(UnknownRID.class, () -> sm.getTransaction().get(rid));
        assertEquals(0, t.getDirtyCount());
        assertEquals(0, t.getDirtyDeletedCount());
    }
    
    
    @Test
    public void retryCommitNotRetryable() throws Exception {
        SimpleVertexEx sv = new SimpleVertexEx();
        sv = sm.store(sv);
        sm.commit();
        String rid = sm.getRID(sv);
        
        Transaction t1 = sm.getTransaction();
        SimpleVertexEx s1 = t1.get(SimpleVertexEx.class, rid);
        
        Transaction t2 = sm.getTransaction();
        SimpleVertexEx s2 = t2.get(SimpleVertexEx.class, rid);

        t1.delete(s1);
        t1.commit();
        
        s2.setS("en tran 2");
        OdbogmException ex = assertThrows(OdbogmException.class, () -> t2.commit());
        assertFalse(ex.canRetry());
        
        //reintento
        ex = assertThrows(OdbogmException.class, () -> t2.commit());
        assertFalse(ex.canRetry());
    }
    
    /*
     * Testea que tengo un objeto existente, lo modifico, le agrego objetos
     * nuevos a las colecciones y commiteo sin hacer store de esos objetos nuevos.
     */
    @Test
    public void commitNotStored() throws Exception {
        String random = RandomStringUtils.randomAlphanumeric(30);
        
        Foo foo = new Foo();
        foo = sm.store(foo);
        sm.commit();
        
        foo.add(new SimpleVertex(random));
        sm.commit(); //nunca hice store del SV random
        sm.getCurrentTransaction().clearCache();
        
        assertFalse(sm.query(SimpleVertex.class,
                String.format("where s = '%s'", random)).isEmpty());
    }
    
    /*
     * Testea que inyecte bien el RID si un campo es anotado para eso.
     */
    @Test
    public void ridInjection() throws Exception {
        SimpleVertex v = new SimpleVertex();
        assertNull(v.getRid());
        
        v = sm.store(v);
        assertNotNull(v.getRid());
        assertTrue(v.getRid().startsWith("#-"));
        
        sm.commit();
        assertFalse(v.getRid().startsWith("#-"));
        
        //cargo de nuevo el vértice desde la base
        sm.getCurrentTransaction().clearCache();
        String rid = v.getRid();
        v = sm.get(SimpleVertex.class, rid);
        assertEquals(rid, v.getRid());
    }

    /*
     * Testea que inyecte bien el RID en un objeto mapeado a una arista.
     */
    @Test
    public void injectRidInEdge() throws Exception {
        SimpleVertexEx value = new SimpleVertexEx();
        value.setS("value");
        
        SimpleVertexEx v = new SimpleVertexEx();
        v.setOhmSVE(new HashMap<>());
        v.getOhmSVE().put(new EdgeAttrib("edge1", new Date()), value);
        v = sm.store(v);
        sm.commit();
        
        v.getOhmSVE().forEach(new BiConsumer<EdgeAttrib, SimpleVertexEx>() {
            @Override
            public void accept(EdgeAttrib key, SimpleVertexEx val) {
                assertNotNull(key.getRid());
            }
        });
    }
    
    /*
     * Testea que falle si se anota como RID un campo que no es String.
     */
    @Test
    public void badRidField() throws Exception {
        @Entity class BadRid {
            @RID Integer rid;
        }
        BadRid br = new BadRid();
        assertThrows(IncorrectRIDField.class, () -> sm.store(br));
    }
    
    /*
     * Testea que persista y cargue correctamente una colección de enums.
     */
    @Test
    public void persistEnumCollection() throws Exception {
        Enums v = new Enums();
        v.enums = Arrays.asList(EnumTest.UNO, EnumTest.DOS, EnumTest.OTRO_MAS);
        v = sm.store(v);
        sm.commit();
        String rid = sm.getRID(v);
        
        sm.getCurrentTransaction().clearCache();
        v = sm.get(Enums.class, rid);
        assertEquals(3, v.enums.size());
        assertTrue(v.enums.contains(EnumTest.UNO));
        assertTrue(v.enums.contains(EnumTest.DOS));
        assertTrue(v.enums.contains(EnumTest.OTRO_MAS));
    }
    
    @Test
    @Ignore //@TODO: corregir los mapas embebidos con enums
    public void persistEnumMap() throws Exception {
        Enums v = new Enums();
        v.enumToString.put(EnumTest.UNO, "el primero");
        v.enumToString.put(EnumTest.DOS, "el segundo");
        
        v.stringToEnum.put("primer valor", EnumTest.UNO);
        v.stringToEnum.put("segundo valor", EnumTest.DOS);
        
        v = sm.store(v);
        sm.commit();
        String rid = sm.getRID(v);
        System.out.println("RID: " + rid);
        sm.getCurrentTransaction().clearCache();
        v = sm.get(Enums.class, rid);
        
        assertEquals(2, v.enumToString.size());
        assertEquals("el primero", v.enumToString.get(EnumTest.UNO));
        assertEquals("el segundo", v.enumToString.get(EnumTest.DOS));
        assertEquals(2, v.stringToEnum.size());
        assertEquals(EnumTest.UNO, v.stringToEnum.get("primer valor"));
        assertEquals(EnumTest.DOS, v.stringToEnum.get("segundo valor"));
    }
    
    /*
     * Testea los mapas que son persistidos como relaciones a nodos.
     */
    @Test
    public void edgeAttributes() throws Exception {
        SimpleVertexEx to = new SimpleVertexEx();
        SimpleVertexEx v = new SimpleVertexEx();
        v.setOhmSVE(new HashMap<>());
        
        EdgeAttrib e1 = new EdgeAttrib("relación 1", new Date());
        EdgeAttrib e2 = new EdgeAttrib("relación 2", new Date());
        v.ohmSVE.put(e1, to);
        v.ohmSVE.put(e2, to);
        
        v = sm.store(v);
        sm.commit();
        String rid = sm.getRID(v);
        sm.getCurrentTransaction().clearCache();
        
        v = sm.get(SimpleVertexEx.class, rid);
        assertEquals(2, v.ohmSVE.size());
        
        //elimino una relación
        v.ohmSVE.remove(e1);
        sm.commit();
        sm.getCurrentTransaction().clearCache();
        
        v = sm.get(SimpleVertexEx.class, rid);
        assertEquals(1, v.ohmSVE.size());
        assertNotNull(v.ohmSVE.get(e2));
        assertNull(v.ohmSVE.get(e1));
        
        v.ohmSVE.remove(e2);
        assertTrue(v.ohmSVE.isEmpty());
        sm.rollback();
        assertFalse(v.ohmSVE.isEmpty());
        
        //agregar más elementos al mapa
        //@TODO: ver bien, esto no anda como debiera
//        v.ohmSVE.clear();
//        EdgeAttrib e3 = new EdgeAttrib("nueva relación", new Date());
//        v.ohmSVE.put(e3, to);
//        sm.commit();
//        sm.getCurrentTransaction().clearCache();
//        
//        v = sm.get(SimpleVertexEx.class, rid);
//        assertEquals(1, v.ohmSVE.size());
//        assertEquals("nueva relación", v.ohmSVE.keySet().iterator().next().getNota());
    }
    
    /*
     * Testea que persista y cargue bien atributos de tipo enum en aristas.
     */
    @Test
    public void enumEdgeAttribute() throws Exception {
        SimpleVertexEx to = new SimpleVertexEx();
        SimpleVertexEx v = new SimpleVertexEx();
        v.setOhmSVE(new HashMap<>());
        
        EdgeAttrib e1 = new EdgeAttrib("relación 1", new Date());
        e1.setEnumValue(EnumTest.TRES);
        v.ohmSVE.put(e1, to);
        
        v = sm.store(v);
        sm.commit();
        String rid = sm.getRID(v);
        sm.getCurrentTransaction().clearCache();
        
        v = sm.get(SimpleVertexEx.class, rid);
        assertEquals(1, v.ohmSVE.size());
        assertEquals(EnumTest.TRES, v.ohmSVE.keySet().iterator().next().getEnumValue());
    }
    
    /*
     * Testea que persista y cargue bien nodos con otro nombre distinto a la clase Java.
     */
    @Test
    public void differentEntityName() throws Exception {
        //los objetos Foo se guardan en la base con la clase FooNode
        Foo foo = sm.store(new Foo("It's a FooNode"));
        foo.add(new SimpleVertex());
        sm.commit();
        String rid = sm.getRID(foo);
        sm.getCurrentTransaction().clearCache();
        foo = sm.get(Foo.class, rid);
        assertEquals("It's a FooNode", foo.getText());
        assertEquals(1, foo.getLsve().size());
        
        //usando interfaz
        sm.getCurrentTransaction().clearCache();
        InterfaceTest it = sm.get(InterfaceTest.class, rid);
        foo = (Foo)it;
        assertEquals("It's a FooNode", foo.getText());
        assertEquals(1, foo.getLsve().size());
    }
    
    /*
     * Testea que los mapas con clases de aristas heredados mantengan la clase
     * de la arista.
     */
    @Test
    public void inheritedMapEdge() throws Exception {
        long edges = sm.query("select count(*) from EdgeAttrib", "");
        long specificEdges = sm.query("select count(*) from SVExChild_ohmSVE", "");
        
        SVExChild v = new SVExChild();
        v.ohmSVE.put(new EdgeAttrib("nota", new Date()), new SimpleVertexEx());
        sm.store(v);
        sm.commit();
        
        //SVExChild_ohmSVE edges must be subclasses of EdgeAttrib:
        long newSpecificEdges = sm.query("select count(*) from SVExChild_ohmSVE", "");
        assertEquals(specificEdges + 1, newSpecificEdges);
        long newEdges = sm.query("select count(*) from EdgeAttrib", "");
        assertEquals(edges + 1, newEdges);
    }
    
    /*
     * Tests the fix of a bug that caused a NullPointerException when making
     * dirty an edge object.
     */
    @Test
    public void dirtyEdge() throws Exception {
        SimpleVertexEx value = new SimpleVertexEx();
        SimpleVertexEx v = new SimpleVertexEx();
        v.setOhmSVE(new HashMap<>());
        v.getOhmSVE().put(new EdgeAttrib("edge1", new Date()), value);
        v = sm.store(v);
        
        //make dirty the edge:
        v.getOhmSVE().entrySet().iterator().next().getKey().setNota("dirty");
        
        sm.commit();
        String rid = sm.getRID(v);
        sm.getCurrentTransaction().clearCache();
        
        v = sm.get(SimpleVertexEx.class, rid);
        assertEquals("dirty", v.getOhmSVE().entrySet().iterator().next().getKey().getNota());
        
        v.getOhmSVE().entrySet().iterator().next().getKey().setNota("dirty");
    }
    
    /*
     * Tests that indirect links are refreshed after commit.
     */
    @Test
    @Ignore //@TODO: deactivated refresh indirect temporally
    public void refreshIndirects() throws Exception {
        IndirectObject main = new IndirectObject("Main");
        IndirectObject sub = new IndirectObject("Sub");
        IndirectObject col1 = new IndirectObject("Col1");
        IndirectObject col2 = new IndirectObject("Col2");
        
        main.setDirectLink(sub);
        col1.getAlDirectLinked().add(main);
        col2.getAlDirectLinked().add(main);
        
        main = sm.store(main);
        sub = sm.store(sub);
        col1 = sm.store(col1);
        col2 = sm.store(col2);
        sm.commit();
        
        //after commit the indirect links must be loaded:
        //main --> sub
        assertNotNull(sub.getIndirectLink());
        assertEquals(main, sub.getIndirectLink());
        //col1 --> main
        //col2 --> main
        assertNotNull(main.getAlIndirectLinked());
        assertEquals(2, main.getAlIndirectLinked().size());
        assertTrue(main.getAlIndirectLinked().contains(col1));
        assertTrue(main.getAlIndirectLinked().contains(col2));
        //assert objects not made dirty:
        assertFalse(((IObjectProxy)main).___isDirty());
        assertFalse(((IObjectProxy)sub).___isDirty());
        assertFalse(((IObjectProxy)col1).___isDirty());
        assertFalse(((IObjectProxy)col2).___isDirty());
    }
    
}
