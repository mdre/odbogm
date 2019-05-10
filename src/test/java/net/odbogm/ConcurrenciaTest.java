package net.odbogm;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import test.SimpleVertexEx;

/**
 *
 * @author jbertinetti
 */
public class ConcurrenciaTest {

    private final int poolSize = 5;

    private SessionManager sm;


    @Before
    public void setUp() {
        System.out.println("Iniciando session manager...");
        sm = new SessionManager("remote:localhost/Test", "admin", "admin",
                1, poolSize
        )
                .setActivationStrategy(SessionManager.ActivationStrategy.CLASS_INSTRUMENTATION)
                .setClassLevelLog(Transaction.class, Level.FINER);
        sm.begin();
        System.out.println("fin setup.");
    }


    @After
    public void tearDown() {
        sm.shutdown();
    }


    @Test
    public void testConcurrencia() throws Exception {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Múltiples transacciones en paralelo leen el mismo nodo");
        System.out.println("***************************************************************");

        SimpleVertexEx s1 = new SimpleVertexEx();
        s1 = sm.store(s1);
        sm.commit();
        String rid = sm.getRID(s1);
        assertNotNull(rid);
        System.out.println("RID: " + rid);
        
        s1.setSvex("modificado");
        sm.commit();

        int N = 20;
        ExecutorService executor = Executors.newFixedThreadPool(N);
        List<Callable<String>> tasks = Stream.generate(() -> {
            return new Callable<String>() {
                @Override
                public String call() throws Exception {
                    System.out.println("ejecutando!!!!!!");
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
        }).forEach(s -> assertEquals("modificado", s));
        executor.shutdown();

        System.out.println(sm.openTransactionsCount() + " transacciones abiertas");
        System.gc();
        System.out.println(sm.openTransactionsCount() +
                " transacciones abiertas luego del GC");
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

        //recupero con t1
        SimpleVertexEx t1s1 = t1.get(SimpleVertexEx.class, rid);
        assertEquals("default", t1s1.getSvex());

        //recupero con t2
        SimpleVertexEx t2s1 = t2.get(SimpleVertexEx.class, rid);
        assertEquals("default", t2s1.getSvex());

        //modifico en t2
        t2s1.setSvex("modificado");
        t2.commit();

        //limpio caché de la transacción del ogm, no la de Orient
        t1.clearCache();
        t2.clearCache();

        //recupero de nuevo y tiene que estar bien guardado para ambas transacciones
        SimpleVertexEx t1s2 = t1.get(SimpleVertexEx.class, rid);
        assertEquals("modificado", t1s2.getSvex());
        SimpleVertexEx t2s2 = t2.get(SimpleVertexEx.class, rid);
        assertEquals("modificado", t2s2.getSvex());
    }

}
