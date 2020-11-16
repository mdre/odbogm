package net.odbogm;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OVertex;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.odbogm.proxy.IObjectProxy;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import test.IndirectObject;
import test.SimpleVertexEx;
import test.TestConfig;

/**
 *
 * @author jbertinetti
 */
public class ConcurrencyTest {

    private final int poolSize = 5;

    private SessionManager sm;


    @Before
    public void setUp() {
        System.out.println("Initializing session manager...");
        sm = new SessionManager(TestConfig.TESTDB, "admin", "nimda", 1, poolSize)
                    .setClassLevelLog(Transaction.class, Level.INFO)
                ;
        sm.begin();
        System.out.println("End setup.");
    }


    @After
    public void tearDown() {
        sm.shutdown();
    }


    @Test
    public void testConcurrency() throws Exception {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Multiple parallel transactions read the same vertex");
        System.out.println("***************************************************************");

        SimpleVertexEx s1 = new SimpleVertexEx();
        s1 = sm.store(s1);
        sm.commit();
        String rid = sm.getRID(s1);
        assertNotNull(rid);
        System.out.println("RID: " + rid);
        
        s1.setSvex("changed");
        sm.commit();

        int N = 20;
        ExecutorService executor = Executors.newFixedThreadPool(N);
        List<Callable<String>> tasks = Stream.generate(() -> {
            return new Callable<String>() {
                @Override
                public String call() throws Exception {
                    System.out.println("running!!!!!!");
                    Transaction t = sm.getTransaction();
                    SimpleVertexEx vert = t.get(SimpleVertexEx.class, rid);
                    String s = vert.getSvex();
                    System.out.println(s);
                    return s;
                }
            };
        }).limit(N).collect(Collectors.toList());

        executor.invokeAll(tasks).stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException ex) {
                System.out.println(ex);
                return "error";
            }
        }).forEach(s -> assertEquals("changed", s));
        executor.shutdown();

        System.out.println(sm.openTransactionsCount() + " open transactions");
        System.gc();
        System.out.println(sm.openTransactionsCount() + " open transactions after GC");
        assertEquals(1, sm.openTransactionsCount());
    }


    @Test
    public void testCacheOrient() throws Exception {
        SimpleVertexEx s1 = new SimpleVertexEx();
        s1 = sm.store(s1);
        sm.commit();
        String rid = sm.getRID(s1);
        assertNotNull(rid);
        System.out.println("RID: " + rid);

        Transaction t1 = sm.getTransaction();
        Transaction t2 = sm.getTransaction();

        //retrieve with t1
        SimpleVertexEx t1s1 = t1.get(SimpleVertexEx.class, rid);
        assertEquals("default", t1s1.getSvex());

        //retrieve with t2
        SimpleVertexEx t2s1 = t2.get(SimpleVertexEx.class, rid);
        assertEquals("default", t2s1.getSvex());

        //change in t2
        t2s1.setSvex("modified");
        t2.commit();

        //clean ogm transaction cache, not Orient's
        t1.clearCache();
        t2.clearCache();

        //retrieve again, it must be correctly saved for both transactions
        SimpleVertexEx t1s2 = t1.get(SimpleVertexEx.class, rid);
        assertEquals("modified", t1s2.getSvex());
        SimpleVertexEx t2s2 = t2.get(SimpleVertexEx.class, rid);
        assertEquals("modified", t2s2.getSvex());
    }

    
    /*
     * See https://github.com/orientechnologies/orientdb/issues/9076
     */
    @Test
    public void sbtreeBonsaiRidbag() throws Exception {
        int val = OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.getValue();
        assertEquals(-1, val);
        
        ODatabaseSession g = sm.getDBTx();
        g.begin();
        
        OVertex v1 = g.newVertex();
        v1.save();
        OVertex v2 = g.newVertex();
        v2.save();
        g.commit();
        
        int version = v1.getProperty("@version");
        long out = StreamSupport.stream(v1.getEdges(ODirection.OUT).spliterator(),false).count();
        long in = StreamSupport.stream(v1.getEdges(ODirection.IN).spliterator(),false).count();
        System.out.println("Version: " + version);
        System.out.println("v1 out: " + out);
        System.out.println("v1 in: " + in);
        
        System.out.println("");
        System.out.println("addEdge");
        System.out.println("");
        g.begin();
        v1.addEdge(v2);
        v1.save();
        System.out.println("pre-commit");
        version = v1.getProperty("@version");
        out = StreamSupport.stream(v1.getEdges(ODirection.OUT).spliterator(),false).count();
        in = StreamSupport.stream(v1.getEdges(ODirection.IN).spliterator(),false).count();
        System.out.println("Version: " + version);
        System.out.println("v1 out: " + out);
        System.out.println("v1 in: " + in);
        
        g.commit();
        
        System.out.println("");
        System.out.println("post-commit");
        
        int version2 = v1.getProperty("@version");
        long out2 = StreamSupport.stream(v1.getEdges(ODirection.OUT).spliterator(),false).count();
        long in2 = StreamSupport.stream(v1.getEdges(ODirection.IN).spliterator(),false).count();
        System.out.println("After commit:");
        System.out.println("Version: " + version2);
        System.out.println("v1 out: " + out2);
        System.out.println("v1 in: " + in2);
        assertEquals(version+1, version2);
        assertEquals(out, out2);
        assertEquals(in, in2);
        
        String rid1 = v1.getIdentity().toString();
        System.out.println("RID: "+rid1);
        g.close();
        
        g = sm.getDBTx();
        g.begin();
        v1 = g.load(new ORecordId(rid1));
        version2 = v1.getProperty("@version");
        out2 = StreamSupport.stream(v1.getEdges(ODirection.OUT).spliterator(),false).count();
        in2 = StreamSupport.stream(v1.getEdges(ODirection.IN).spliterator(),false).count();
        System.out.println("New transaction:");
        System.out.println("Version: " + version2);
        System.out.println("v1 out: " + out2);
        System.out.println("v1 in: " + in2);
        assertEquals(version+1 , version2);
        assertEquals(out, out2);
        assertEquals(in, in2);
        
        OVertex v3 = g.newVertex();
        v1.addEdge(v3);
        v1.save();
        g.commit();
        version2 = v1.getProperty("@version");
        out2 = StreamSupport.stream(v1.getEdges(ODirection.OUT).spliterator(),false).count();
        in2 = StreamSupport.stream(v1.getEdges(ODirection.IN).spliterator(),false).count();
        System.out.println("New edge:");
        System.out.println("Version: " + version2);
        System.out.println("v1 out: " + out2);
        System.out.println("v1 in: " + in2);
        assertEquals(version+1 , version2);
        assertEquals(out + 1, out2);
        assertEquals(in, in2);
    }
    
    
    /*
     * Tests that incoming edges don't change the version of the vertex (when
     * the sbtree-based ridbag implementation is always active).
     */
    @Test
    public void bonsaiEnabled() throws Exception {
        int val = OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.getValue();
        assertEquals(-1, val);
        
        IndirectObject v = sm.store(new IndirectObject());
        
        IndirectObject aux1 = sm.store(new IndirectObject());
        aux1.getAlDirectLinked().add(v);
        sm.commit();
        
        int version = ((IObjectProxy)v).___getVertex().getProperty("@version");
        v.setTestData("changed");
        sm.commit();
        int version2 = ((IObjectProxy)v).___getVertex().getProperty("@version");
        assertEquals(version + 1, version2);
        
        v = clearAndGet(v);
        IndirectObject aux2 = sm.store(new IndirectObject());
        aux2.getAlDirectLinked().add(v); //new "in" edge must not change version
        sm.commit();
        
        v = clearAndGet(v);
        int version3 = ((IObjectProxy)v).___getVertex().getProperty("@version");
        assertEquals(version2, version3);
        assertEquals(2, v.getAlIndirectLinked().size());
    }
    
    private <T> T clearAndGet(T object) {
        String rid = sm.getRID(object);
        sm.getCurrentTransaction().clearCache();
        return (T)sm.get(rid);
    }
}
