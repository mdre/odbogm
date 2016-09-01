/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Test;

import com.arshadow.utilitylib.DateHelper;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.SessionManager;
import net.odbogm.proxy.IObjectProxy;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.DbManager;

/**
 *
 * @author SShadow
 */
public class Test {

    private final static Logger LOGGER = Logger.getLogger(Test.class.getName());
    SessionManager sm;

    public ArrayList<SimpleVertex> testArrayList;

    public Map<String, SimpleVertex> testHashMap;

    public static void main(String[] args) {
        new Test();
    }

    public Test() {
        initSession();
        testSessionManager();
//        testDbManager();
//        lab();
//        testQuery();
//        store();
        sm.shutdown();
    }

    public void initSession() {
        System.out.println("Iniciando comunicación con la base....");
        sm = new SessionManager("remote:localhost/Test", "root", "toor");
        System.out.println("comunicación inicializada!");
        sm.begin();
    }

    public void testSessionManager() {
//        IObjectProxy iop;

        // correr test de store
//        this.store();
//          this.testStoreLink();
//        this.testUpdateLink();
//        this.testQuery();
//        testLoop();
        this.lab();

        try {
            sm.commit();
        } catch (OConcurrentModificationException ccme) {

        } finally {
        }
    }

    public void testDbManager() {
        DbManager dbm = new DbManager("remote:localhost/Test", "root", "toor");
        dbm.generateToConsole(new String[]{"Test"});
    }

    public void lab() {
        OrientVertex ov = sm.getGraphdb().getVertex("12:1177");

        // test de fechas.
//        LocalDate ld = LocalDate.now();
//        Date d = new Date(2016, 7, 29);
        Date dt = new Date(2016, 7, 29, 12, 0);

//        Calendar cal = Calendar.getInstance();
//        cal.set(Calendar.YEAR, 2016);
//        cal.set(Calendar.MONTH, 7);
//        cal.set(Calendar.DAY_OF_MONTH, 29);
//        
//        Date d = new Date(cal.getTimeInMillis());
        
        Date d = DateHelper.getDate(2016, 8, 29);
        ov.setProperty("DateHelper", d);
//        ov.setProperty("datetime", dt);



        ov.setProperty("date", d);

//        sm.commit();
//        
//        ArrayList<Integer> testal = new ArrayList<>();
//        testal.add(1);
//        testal.add(2);
//        testal.add(3);
//        
//        HashMap<String,Object> hmTest = new HashMap<>();
//        hmTest.put("hmalI", testal);
//        
//        ov.setProperties(hmTest);
//        
//        ArrayList<Integer> restAL= (ArrayList)ov.getProperty("hmalI");
//        System.out.println(""+restAL.size());
//        sm.getGraphdb().commit();
//        
//        ClassCache cc = new ClassCache();
//        SimpleVertexEx sve = new SimpleVertexEx();
//        sve.initArrayListString();
//        sve.initHashMapString();
//        
//        ClassDef cd = cc.get(SimpleVertexEx.class);
//        
//        System.out.println(""+cd.fields);
//        System.out.println(""+ov.getType().getCustom("javaClass"));
//        String jc = ov.getType().getCustom("javaClass");
//        System.out.println(""+jc.replaceAll("[\'\"]", ""));
//        System.out.println(""+ov.getGraph().getVertexBaseType().getCustom("javaClass"));
//        SimpleVertexEx svex = new SimpleVertexEx();
//        svex.initInner();
//        
//        try {
//            Field f = ReflectionUtils.findField(SimpleVertexEx.class, "svex");
//            f.setAccessible(true);
//            System.out.println("Value: "+f.get(svex));
//            FieldAttributes fa = f.getAnnotation(FieldAttributes.class);
////            System.out.println("Name: "+fa.name());
//            System.out.println("isEmpty: "+fa.defaultVal().isEmpty());
//            System.out.println("isNULL: "+(fa.defaultVal()==null));
//
//            
//        } catch (NoSuchFieldException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        Reflections reflections = new Reflections(SimpleVertex.class.getPackage());
//        Set<Class<? extends SimpleVertex>> r = reflections.getSubTypesOf(SimpleVertex.class);
//        r.stream()
//                .filter(className->className.getSimpleName().equals("SimpleVertexEx"))
//                .collect(Collectors.toList());
//        System.out.println("r: "+r);
//        ArrayListLazyProxy allp = new ArrayListLazyProxy();
//        System.out.println("IL: "+(allp instanceof ILazyCalls));
    }

    public void testQuery() {

        // test query
//        System.out.println("*******************************");
//        System.out.println("          Test Query           ");
//        System.out.println("*******************************");
//        List<SimpleVertex> svs = sm.query(SimpleVertex.class, " where s like '%inn%' ");
//        for (SimpleVertex sv : svs) {
//            System.out.println(">>>"+sv.i+"  rid: "+sm.getRID(sv));
//        }
//        
//        System.out.println("----------- prepared query -----------------");
//        List<SimpleVertexEx> svspq = sm.query(SimpleVertexEx.class, "select from SimpleVertexEx where s like ? and i=?","%wor%",1);
//        for (SimpleVertex sv : svspq) {
//            System.out.println(">>>"+sv.i+"  rid: "+sm.getRID(sv));
//        }
//        System.out.println("----------- 1 commit -----------------");
        long i = sm.query("select count(*) as size from SimpleVertexEx ", null);

        System.out.println("res: " + i);

    }

    public void store() {
        try {
            // usuado para hacer una pausa.
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            SimpleVertex svinner = new SimpleVertex();
            SimpleVertex svinner2 = new SimpleVertex();;
            SimpleVertexEx sv;
            SimpleVertexEx sv1;
            SimpleVertexEx sv2;

            SimpleVertexEx svex = new SimpleVertexEx();
            svex.initInner();
            svex.initArrayList();
            svex.initHashMap();
            svex.initEnum();

            // Test store
            System.out.println("*******************************");
            System.out.println("     Test store: agrego uno    ");
            System.out.println("*******************************");
            svex = sm.store(svex);
            System.out.println("idNew: " + ((IObjectProxy) svex).___getVertex().getIdentity().isNew());
            System.out.println("idTemporary: " + ((IObjectProxy) svex).___getVertex().getIdentity().isTemporary());
            sm.flush();

            System.out.println("----------- STORE commit -----------------");
            sm.commit();

            String testRID = sm.getRID(svex);
            System.out.println("Test: RID:" + testRID);

            System.out.println("*******************************");
            System.out.println("         Test hydrate          ");
            System.out.println("*******************************");

            System.out.println("Test hydrate " + testRID);
            sv = sm.get(SimpleVertexEx.class, testRID);

            //---------------- pausar
            System.out.print("Enter String");
            String s = br.readLine();
            System.out.println("continuando...");
            //-----------------------

            System.out.println("SVINNER.getS(): " + sv.getSvinner().getS());
            System.out.println("Enum test:" + sv.getEnumTest());

            System.out.println("Test - List:");
            for (Iterator<SimpleVertex> iterator = sv.getAlSV().iterator(); iterator.hasNext();) {
                SimpleVertex next = iterator.next();
                System.out.println(next.i);

            }
            System.out.println("Test - Map:");
            for (Map.Entry<String, SimpleVertex> entry : sv.getHmSV().entrySet()) {
                String key = entry.getKey();
                SimpleVertex value = entry.getValue();
                System.out.println("Key: " + key + " value: " + value.i);
            }

            //---------------- pausar
            System.out.print("Enter String");
            s = br.readLine();
            System.out.println("continuando...");
            //-----------------------

            System.out.println("*******************************");
            System.out.println("         Test update          ");
            System.out.println("*******************************");
            sv.i = 25;
            for (Iterator<SimpleVertex> iterator = sv.getAlSV().iterator(); iterator.hasNext();) {
                SimpleVertex next = iterator.next();
                next.i++;
                System.out.println(next.i);

            }
            System.out.println("Update map");
            for (Map.Entry<String, SimpleVertex> entry : sv.getHmSV().entrySet()) {
                String key = entry.getKey();
                SimpleVertex value = entry.getValue();
                value.i++;
            }

//            sv.alSV.add(new SimpleVertex());
//            // test update
//            sv1 = sm.get(SimpleVertexEx.class, "#13:2");
//            sv2 = sm.get(SimpleVertexEx.class, "#13:3");
//            System.out.println(""+sv.getS()+" i:"+sv.i+" svinner.i: "+sv.svinner.i);
//            sv.i++;
//            sv2.i++;
//////             Test de eliminación de un objeto de composición mediante la asignación de null
//            // crear el primer objeto
//            sv1 = new SimpleVertexEx();
//            sv2 = new SimpleVertexEx();
//            sv1.initInner();
//            sv2.svinner = sv1.svinner;
//            
//            sm.store(sv1);
//            sm.store(sv2);
//            
//            System.out.println("----------- 1 commit objetos iniciales-----------------");
//            sm.commit();
//            
//            System.out.println("eliminar la referencia del primero");
//            sv1.svinner =null;
            System.out.println("----------- 1 commit -----------------");
            try {
                sm.commit();
            } catch (OConcurrentModificationException ccme) {

            }
//            System.out.println("----------- 2 commit -----------------");
//            sm.commit();
////            sv.svinner.i++;
//            System.out.println("----------- 3 commit -----------------");
//            sm.commit();
        } catch (IncorrectRIDField | SecurityException | IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void testStoreLink() {

        System.out.println("store objeto sin Link y luego se le agrega uno");

        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx result = this.sm.store(sve);
        this.sm.commit();
        System.out.println("=========== fin primer commit ====================================");

        System.out.println("result.svinner: " + result.getSvinner() + "  sve.svinner:" + sve.getSvinner());

        // actualizar el objeto administrado
        result.initInner();
        System.out.println("result.svinner: " + result.getSvinner().getS() + "      toS: " + result.getSvinner().toString());
        // bajarlo a la base
        System.out.println("=========== inicio segundo commit <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sm.commit();
        System.out.println("=========== fin segundo commit ====================================");
        System.out.println("result.svinner: " + result.getSvinner().getS() + "      toS: " + result.getSvinner().toString());

        // recuperar el objeto en otra instancia
        String rid = ((IObjectProxy) result).___getRid();

        System.out.println("============================================================================");
        System.out.println("RID: " + rid);
        System.out.println("============================================================================");

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("========= comienzo del get =================================================");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
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

    public void testUpdateLink() {
        System.out.println("store objeto sin Link y luego se le agrega uno");

        SimpleVertexEx sve = new SimpleVertexEx();
        SimpleVertexEx result = this.sm.store(sve);
        this.sm.commit();
        System.out.println("=========== fin primer commit ====================================");

        // actualizar el objeto administrado
        result.initInner();
        System.out.println("result.svinner: " + result.getSvinner().getS() + "      toS: " + result.getSvinner().toString());
        // bajarlo a la base
        System.out.println("=========== inicio segundo commit <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sm.commit();
        System.out.println("=========== fin segundo commit ====================================");
        System.out.println("result.svinner: " + result.getSvinner().getS() + "      toS: " + result.getSvinner().toString());

        // recuperar el objeto en otra instancia
        String rid = ((IObjectProxy) result).___getRid();

        System.out.println("============================================================================");
        System.out.println("RID: " + rid);
        System.out.println("============================================================================");

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("========= comienzo del get =================================================");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        System.out.println("========= fin del get =================================================");

        System.out.println("++++++++++++++++ result: " + result.getSvinner().toString());
        System.out.println("++++++++++++++++ expResult: " + expResult.getSvinner().toString());

    }

    public void testLoop() {
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
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        System.out.println("1 >>>>>>>>>>>>>");
        String looprid = ((IObjectProxy) expResult.getLooptest()).___getRid();
        System.out.println("2 >>>>>>>>>>>>>");
        System.out.println("");
        System.out.println("");
        System.out.println("Objeto almacenado en: " + rid + " loop rid: " + looprid);
        System.out.println("");
        System.out.println("");
//        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);

        System.out.println("");
        System.out.println("");
        System.out.println(" get completado. Iniciando los asserts");
        System.out.println("");
        System.out.println("");

        // verificar que todos los valores sean iguales
//        assertEquals(((IObjectProxy)expResult).___getRid(), ((IObjectProxy)result).___getRid());
//        assertEquals(((IObjectProxy)expResult.getLooptest()).___getRid(), ((IObjectProxy)result.getLooptest()).___getRid());
//        assertEquals(((IObjectProxy)expResult.getLooptest().getLooptest()).___getRid(), ((IObjectProxy)result).___getRid());
        System.out.println("============================= FIN LoopTest ===============================");
    }

    /**
     * soporte desde JUnit
     *
     */
    /**
     * Asserts that two objects are equal. If they are not, an {@link AssertionError} without a message is thrown. If <code>expected</code> and
     * <code>actual</code> are <code>null</code>, they are considered equal.
     *
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     */
    static public void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two objects are equal. If they are not, an {@link AssertionError} is thrown with the given message. If <code>expected</code> and
     * <code>actual</code> are <code>null</code>, they are considered equal.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code> okay)
     * @param expected expected value
     * @param actual actual value
     */
    static public void assertEquals(String message, Object expected,
            Object actual) {
        if (equalsRegardingNull(expected, actual)) {
            return;
        } else if (expected instanceof String && actual instanceof String) {
            String cleanMessage = message == null ? "" : message;
            System.out.println("Expected: " + expected + " - actual: " + actual);
        } else {
            failNotEquals(message, expected, actual);
        }
    }

    private static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        }

        return isEquals(expected, actual);
    }

    private static boolean isEquals(Object expected, Object actual) {
        return expected.equals(actual);
    }

    static private void failNotEquals(String message, Object expected,
            Object actual) {
        System.out.println("ERROR: " + (format(message, expected, actual)));
    }

    static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null && !message.equals("")) {
            formatted = message + " ";
        }
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (expectedString.equals(actualString)) {
            return formatted + "expected: "
                    + formatClassAndValue(expected, expectedString)
                    + " but was: " + formatClassAndValue(actual, actualString);
        } else {
            return formatted + "expected:<" + expectedString + "> but was:<"
                    + actualString + ">";
        }
    }

    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }

}
