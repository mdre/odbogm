package net.odbogm.proxy;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OVertex;
import java.util.Date;
import java.util.HashMap;
import net.odbogm.SessionManager;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.utils.DateHelper;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import test.EdgeAttrib;
import test.EnumTest;
import test.SimpleVertex;
import test.SimpleVertexEx;
import test.TestConfig;

/**
 *
 * @author jbertinetti
 */
public class HashMapLazyProxyTest {

    private SessionManager sm;
    
    private SimpleVertexEx v;

    
    @Before
    public void setUp() {
        sm = new SessionManager(TestConfig.TESTDB, "admin", "nimda");
        sm.begin();
        v = sm.store(new SimpleVertexEx());
    }

    @After
    public void tearDown() {
        sm.shutdown();
    }

    private <T> T commitClearAndGet(T object) {
        sm.commit();
        String rid = sm.getRID(object);
        sm.getCurrentTransaction().clearCache();
        return (T)sm.get(rid);
    }
    
    @Test
    public void put() throws Exception {
        SimpleVertexEx to = sm.store(new SimpleVertexEx());
        EdgeAttrib e = new EdgeAttrib();
        v.ohmSVE.put(e, to);
        
        v = commitClearAndGet(v);
        assertEquals(1, v.ohmSVE.size());
        assertEquals(e, v.ohmSVE.keySet().iterator().next());
        assertEquals(to, v.ohmSVE.values().iterator().next());
    }
    
    @Test
    public void putAll() throws Exception {
        
    }
    
    @Test
    public void putIfAbsent() throws Exception {
        
    }
    
    @Test
    public void cascadeDelete() throws Exception {
        
    }
    
    @Test
    public void remove() throws Exception {
        SimpleVertexEx to = sm.store(new SimpleVertexEx());
        EdgeAttrib e = new EdgeAttrib();
        v.ohmSVE.put(e, to);
        sm.commit();
        
        v.ohmSVE.remove(e);
        v = commitClearAndGet(v);
        assertTrue(v.ohmSVE.isEmpty());
    }
    
    @Test
    public void remove2() throws Exception {
        SimpleVertexEx to = sm.store(new SimpleVertexEx());
        EdgeAttrib e = new EdgeAttrib();
        v.ohmSVE.put(e, to);
        v.ohmSVE.remove(e);
        sm.commit();
        v = commitClearAndGet(v);
        assertTrue(v.ohmSVE.isEmpty());
    }
    
    @Test
    @Ignore("Hasta corregir removeOrphan")
    public void removeOrphan() throws Exception {
        SimpleVertexEx to = sm.store(new SimpleVertexEx());
        EdgeAttrib e = new EdgeAttrib();
        v.hmRO.put(e, to);
        sm.commit();
        
        String ridDeleted = sm.getRID(to);
        v.hmRO.remove(e);
        v = commitClearAndGet(v);
        assertTrue(v.hmRO.isEmpty());
        assertThrows(UnknownRID.class, () -> sm.get(ridDeleted));
    }
    
    @Test
    public void clear() throws Exception {
        
    }
    
    @Test
    @Ignore("Hasta corregir removeOrphan")
    public void replaceOrphan() throws Exception {
        SimpleVertexEx to1 = sm.store(new SimpleVertexEx());
        SimpleVertexEx to2 = sm.store(new SimpleVertexEx());
        EdgeAttrib e = new EdgeAttrib();
        v.hmRO.put(e, to1);
        sm.commit();
        String ridDeleted = sm.getRID(to1);
        
        v.hmRO.put(e, to2);
        v = commitClearAndGet(v);
        assertEquals(1, v.hmRO.size());
        assertEquals(to2, v.hmRO.get(e));
        assertThrows(UnknownRID.class, () -> sm.get(ridDeleted));
    }
    
    @Test
    public void multiEdge() throws Exception {
        //one value, two keys
        //remove one
    }
    
    @Test
    @Ignore("Hasta corregir removeOrphan")
    public void sameValue() throws Exception {
        //one value in one key
        //remove (entityState REMOVED)
        //add same value another key (ADDED)
        
        //same case another form
        SimpleVertexEx to1 = sm.store(new SimpleVertexEx());
        SimpleVertexEx to2 = sm.store(new SimpleVertexEx());
        EdgeAttrib e1 = new EdgeAttrib();
        v.hmRO.put(e1, to1);
        sm.commit();
        
        v.hmRO.put(e1, to2);
        EdgeAttrib e2 = new EdgeAttrib();
        v.hmRO.put(e2, to1);
        v = commitClearAndGet(v);
        
        assertEquals(2, v.hmRO.size());
        assertEquals(to2, v.hmRO.get(e1));
        assertEquals(to1, v.hmRO.get(e2));
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
        sve.setOhmSVE(null);

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
    
    /*
     * Tests that maps are persisted as relations to vertices.
     */
    @Test
    public void edgeAttributes() throws Exception {
        SimpleVertexEx to = new SimpleVertexEx();
        SimpleVertexEx v = new SimpleVertexEx();
        
        EdgeAttrib e1 = new EdgeAttrib("relation 1", new Date());
        EdgeAttrib e2 = new EdgeAttrib("relation 2", new Date());
        v.ohmSVE.put(e1, to);
        v.ohmSVE.put(e2, to);
        
        v = sm.store(v);
        v = commitClearAndGet(v);
        assertEquals(2, v.ohmSVE.size());
        
        //remove a relation
        v.ohmSVE.remove(e1);
        v = commitClearAndGet(v);
        assertEquals(1, v.ohmSVE.size());
        to = v.ohmSVE.get(e2);
        assertNotNull(to);
        assertNull(v.ohmSVE.get(e1));
        
        v.ohmSVE.remove(e2);
        assertTrue(v.ohmSVE.isEmpty());
        sm.rollback();
        assertFalse(v.ohmSVE.isEmpty());
        
        //add more elements to the map
        v.ohmSVE.clear();
        EdgeAttrib e3 = new EdgeAttrib("new relation", new Date());
        v.ohmSVE.put(e3, to);
        v = commitClearAndGet(v);
        assertEquals(1, v.ohmSVE.size());
        assertEquals("new relation", v.ohmSVE.keySet().iterator().next().getNota());
    }
    
    /*
     * Bug fixed: in edge map, add an edge, commit, remove edge, commit, caused
     * and exception. Also, a recently added key didn't detect changes to be
     * persisted.
     */
    @Test
    public void edgeAttributes2() throws Exception {
        SimpleVertexEx to = sm.store(new SimpleVertexEx());
        SimpleVertexEx v = sm.store(new SimpleVertexEx());
        
        EdgeAttrib e1 = new EdgeAttrib();
        EdgeAttrib e2 = new EdgeAttrib();
        v.ohmSVE.put(e1, to);
        v.ohmSVE.put(e2, to);
        sm.commit();
        System.out.println("Rid: " + sm.getRID(v));
        assertEquals(2, v.getOhmSVE().size());
        
        v.ohmSVE.remove(e2);
        //if bug is fixed, this must not throw exception:
        sm.commit();
        assertEquals(1, v.getOhmSVE().size());
        
        //edit a new edge
        
        e1 = v.getOhmSVE().keySet().iterator().next();
        e1.setNota("a text");
        
        //the commit must persist the change
        v = commitClearAndGet(v);
        assertEquals(1, v.getOhmSVE().size());
        e1 = v.getOhmSVE().keySet().iterator().next();
        assertEquals("a text", e1.getNota());
    }
    
    /*
     * More tests with maps of edged.
     */
    @Test
    public void edgeAttributes3() throws Exception {
        SimpleVertexEx to1 = sm.store(new SimpleVertexEx());
        to1.setS("to1");
        SimpleVertexEx to2 = sm.store(new SimpleVertexEx());
        to2.setS("to2");
        SimpleVertexEx to3 = sm.store(new SimpleVertexEx());
        to3.setS("to3");
        SimpleVertexEx v = sm.store(new SimpleVertexEx());
        
        EdgeAttrib e = new EdgeAttrib();
        v.ohmSVE.put(e, to1);
        sm.commit();
        String rid = sm.getRID(v);
        System.out.println("Rid: " + rid);
        
        v.ohmSVE.put(e, to2);
        assertEquals(to2, v.getOhmSVE().get(e));
        v.ohmSVE.put(e, to3);
        assertEquals(to3, v.getOhmSVE().get(e));
        
        v = commitClearAndGet(v);
        
        SimpleVertexEx to = v.getOhmSVE().values().iterator().next();
        assertEquals(to, to3);
        assertEquals("to3", to.getS());
        
        v.ohmSVE.remove(e);
        v.ohmSVE.put(e, to1);
        assertEquals(to1, v.getOhmSVE().get(e));
        
        v = commitClearAndGet(v);
        
        to = v.getOhmSVE().values().iterator().next();
        assertEquals(to, to1);
        assertEquals("to1", to.getS());
        
        //same value
        v.ohmSVE.put(e, to1);
        assertEquals(to1, v.getOhmSVE().get(e));
        v = commitClearAndGet(v);
        to = v.getOhmSVE().values().iterator().next();
        assertEquals(to, to1);
        assertEquals("to1", to.getS());
        
        //same value, different key (two edges to same vertex)
        EdgeAttrib e2 = new EdgeAttrib();
        v.ohmSVE.put(e2, to1);
        assertEquals(2, v.getOhmSVE().size());
        
        v = commitClearAndGet(v);
        assertEquals(2, v.getOhmSVE().size());
        
        v.ohmSVE.remove(e);
        assertEquals(1, v.getOhmSVE().size());
        v = commitClearAndGet(v);
        assertEquals(1, v.getOhmSVE().size());
        assertEquals(e2, v.getOhmSVE().keySet().iterator().next());
        
        //verify the edges:
        try (var g = sm.getDBTx()) {
            OVertex vertex = g.getRecord(new ORecordId(rid));
            int cant = 0;
            var it = vertex.getEdges(ODirection.OUT, "SimpleVertexEx_ohmSVE").iterator();
            while (it.hasNext()) {
                cant++;
                it.next();
            }
            assertEquals(1, cant);
        }
    }
    
    /*
     * Testea que persista y cargue bien atributos de tipo enum en aristas.
     */
    @Test
    public void enumEdgeAttribute() throws Exception {
        SimpleVertexEx to = new SimpleVertexEx();
        SimpleVertexEx v = new SimpleVertexEx();
        
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
    
    @Test
    public void testRollbackMaps() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback Maps. Se restablecen los atributos que hereden de Collection.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.hmSV = new HashMap<>();
        SimpleVertex sv = new SimpleVertex();
        sve.hmSV.put("key1", sv);
        sve.hmSV.put("key2", sv);
        sve.hmSV.put("key3", new SimpleVertex());

        System.out.println("guardando el objeto con 3 elementos en el HM.");
        SimpleVertexEx stored = sm.store(sve);
        sm.commit();

        // modificar los campos.
        stored.hmSV.put("key rollback", new SimpleVertex());

        sm.rollback();

        assertEquals(sve.hmSV.size(), stored.hmSV.size());
    }
    
}
