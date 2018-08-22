/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.SessionManager;
import net.odbogm.proxy.IObjectProxy;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.DbManager;
import net.odbogm.Transaction;
import net.odbogm.exceptions.ReferentialIntegrityViolation;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.security.AccessRight;
import net.odbogm.security.GroupSID;
import net.odbogm.security.UserSID;
import net.odbogm.utils.DateHelper;
import net.odbogm.utils.ReflectionUtils;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class Test {

    private final static Logger LOGGER = Logger.getLogger(Test.class.getName());

    static {
        LOGGER.setLevel(Level.ALL);
    }
    SessionManager sm;

    public ArrayList<SimpleVertex> testArrayList;

    public Map<String, SimpleVertex> testHashMap;

    public static void main(String[] args) {
        new Test();
    }

    public Test() {
        initSession();
//        testSessionManager();
//        testDbManager();
//        lab();
//        testQuery();
//        store();
//          testDelete();
//        testEmbeddded();
//        setUpGroups();
//        testSObjects();
//        testLongQuery();
//        testMultiTran();
//        testRollbackEmbedded();
//        testRollbackSVE();
//        testTimeLoad();
//        testComplexHashMap();
//        testSimpleQuery();
//        testCmd();
        sm.shutdown();
    }

    public void initSession() {
        System.out.println("Iniciando comunicación con la base....");
        long millis = System.currentTimeMillis();
        sm = new SessionManager("remote:localhost/Test", "root", "toor")
                    .setActivationStrategy(SessionManager.ActivationStrategy.CLASS_INSTRUMENTATION)
//                    .setClassLevelLog(Transaction.class, Level.FINER)
//                    .setClassLevelLog(SessionManager.class, Level.FINER)
                ;
//        sm = new SessionManager("remote:localhost/quiencotiza", "root", "toor");
        System.out.println("Tiempo de inicio: " + (System.currentTimeMillis() - millis));
        System.out.println("comunicación inicializada!");
        sm.begin();
//        System.out.println("" + (System.currentTimeMillis() - millis));
//        sm.setAuditOnUser("userAuditado");
    }

    public void testSessionManager() {
//        IObjectProxy iop;

        // correr test de store
//        this.store();
//          this.testStoreLink();
//        this.testUpdateLink();
//        this.testQuery();
//        testLoop();
//        this.lab();
        try {
            sm.commit();
        } catch (OConcurrentModificationException ccme) {

        } finally {
        }
    }

    public void testDbManager() {
        DbManager dbm = new DbManager("remote:localhost/Test", "root", "toor");
//        dbm.generateToConsole(new String[]{"Test"});
        dbm.generateDBSQL("/tmp/1/test.sql", new String[]{"Test"});

    }

    public void testCmd() {
        Object o = sm.query("select from SimpleVertex");
        System.out.println("o: "+o.getClass());
    }
    
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
            if (object instanceof SimpleVertexEx) {
                isve++;
            } else if (object instanceof SimpleVertex) {
                isv++;
            } else {
                System.out.println("ERROR:  " + object.getClass() + " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }

            //System.out.println("Query: "+object.getClass()+" - toString: "+object.getClass().getSimpleName());
        }
        System.out.println("sv: "+isv);
        System.out.println("SVE: "+isve );

        System.out.println("***************************************************************");
        System.out.println("Fin SimpleQuery");
        System.out.println("***************************************************************");
    }
    
    
    
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

        //assertNull(stored.getOhmSVE());

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

//        SimpleVertexEx ohmsveGetted = retrieved.getOhmSVE().get("key1");
//        System.out.println("key1: "+(ohmsveGetted==null?" NULL!":"Ok."));
//        assertNotNull(ohmsveGetted);
//        
        System.out.println("\nagregamos un nuevo objeto al hashmap ya inicializado");
        stored.getOhmSVE().put(new EdgeAttrib("nota 3", DateHelper.getCurrentDate()), new SimpleVertexEx());
        System.out.println("\ninicio tercer commit ----------------------------------------------------------");
        sm.commit();
        System.out.println("tercer commit ----------------------------------------------------------\n");

        retrieved = sm.get(SimpleVertexEx.class, rid);

        System.out.println("retrieved: " + retrieved + " : " + retrieved.getOhmSVE());
        System.out.println("stored: " + stored + " : " + stored.getOhmSVE());

        assertEquals(retrieved.getOhmSVE().size(), stored.getOhmSVE().size());

    }
    
    
    
    public void testEmbeddded() {
        SimpleVertexWithEmbedded svemb = new SimpleVertexWithEmbedded();
        SimpleVertexWithEmbedded result = this.sm.store(svemb);
        this.sm.commit();

        String rid = ((IObjectProxy) result).___getRid();

        SimpleVertexWithEmbedded ret = this.sm.get(SimpleVertexWithEmbedded.class, rid);

        System.out.println("list.size: " + ret.stringlist.size());
        System.out.println("map.size: " + ret.simplemap.size());

        System.out.println("Anexando uno a la lista");
        ret.addToList();

        this.sm.commit();

        System.out.println("modificando el contenido de un elemento de la lista...");
        ret.stringlist.set(1, "-1-");

        this.sm.commit();

        System.out.println("==========================================================");
        System.out.println("Anexando uno al map");
        System.out.println("==========================================================");
        ret.addToMap();
        this.sm.commit();
        System.out.println("==========================================================");
        ret.simplemap.put("key 1", 10);

        this.sm.commit();

    }

    //======================================
    public void setUpGroups() {
        GroupSID gna = new GroupSID("gna", "gna");
        GroupSID gr = new GroupSID("gr", "gr");
        GroupSID gw = new GroupSID("gw", "gr");

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

//        una.addGroup(sgna);
//        una.addGroup(sgr);
//
//        ur.addGroup(sgr);
//
//        uw.addGroup(sgw);
//
//        urw.addGroup(sgw);
//        urw.addGroup(sgr);

        this.sm.commit();
    }

    public void testSObjects() {
        SSimpleVertex ssv = new SSimpleVertex();

        ssv = this.sm.store(ssv);
        this.sm.commit();

        String reg = ((IObjectProxy) ssv).___getRid();

//        SSimpleVertex rssv = this.sm.get(SSimpleVertex.class, reg);
        // recuperar los grupos
        GroupSID gna = this.sm.get(GroupSID.class, "#32:11");
        GroupSID gr = this.sm.get(GroupSID.class, "#32:10");
        GroupSID gw = this.sm.get(GroupSID.class, "#32:9");

        System.out.println("Agregando los acls...");
        ssv.setAcl(gna, new AccessRight(0));
        ssv.setAcl(gr, new AccessRight().setRights(AccessRight.READ));
        ssv.setAcl(gw, new AccessRight().setRights(AccessRight.WRITE));

        this.sm.commit();

        // testear SIN ACCESO
        UserSID una = this.sm.get(UserSID.class, "33:23");
        UserSID ur = this.sm.get(UserSID.class, "33:22");
        UserSID uw = this.sm.get(UserSID.class, "33:21");
        this.sm.setLoggedInUser(una);

        System.out.println("Security State NA: " + ssv.validate(una));
        System.out.println("Security State R: " + ssv.validate(ur));
        System.out.println("Security State W: " + ssv.validate(uw));

    }

    //======================================
    public void lab() {

        SimpleVertexEx sv1 = sm.get(SimpleVertexEx.class, "12:1177");
        SimpleVertexEx sv2 = sm.get(SimpleVertexEx.class, "12:1177");

        HashMap<Integer, Object> hmi = new HashMap<>();

        try {
            System.out.println(ReflectionUtils.findMethod(SimpleVertexEx.class, "toString", (Class<?>[]) null));
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("" + sv2.toString());
//        OrientVertex v1 = sm.getGraphdb().getVertex("12:3360");
//        OrientVertex v2 = sm.getGraphdb().getVertex("23:6275");
//        String edgeLabel = "SimpleVertexEx_svinner";
//        boolean connected=false;
//        Iterable<Edge> result = v1.getEdges(v2, Direction.OUT, edgeLabel==null?"E":edgeLabel);
//            for (Edge e : result) {
//                LOGGER.log(Level.FINER, "Conectados por el edge: "+e.getId());
//                connected = true;
//                break;
//            }
//        System.out.println("Conectados: "+connected);
        // test de fechas.
//        LocalDate ld = LocalDate.now();
//        Date d = new Date(2016, 7, 29);
//        Date dt = new Date(2016, 7, 29, 12, 0);

//        Calendar cal = Calendar.getInstance();
//        cal.set(Calendar.YEAR, 2016);
//        cal.set(Calendar.MONTH, 7);
//        cal.set(Calendar.DAY_OF_MONTH, 29);
//        
//        Date d = new Date(cal.getTimeInMillis());
//        Date d = DateHelper.getDate(2016, 8, 29);
//        ov.setProperty("DateHelper", d);
//        ov.setProperty("datetime", dt);
//        ov.setProperty("date", d);
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
        long i = sm.query("select count(*) as size from SimpleVertexEx ", "");

        System.out.println("res: " + i);

    }

    public void store() {
        try {
            System.out.println("*******************************");
            System.out.println("     Test store: agrego uno    ");
            System.out.println("*******************************");
            
            GroupSID gs = new GroupSID();
            System.out.println("---------------------------------------------------------");
            // usuado para hacer una pausa.
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            LOGGER.log(Level.FINER, "Test finer");
            SimpleVertex svinner = new SimpleVertex();
            SimpleVertex svinner2 = new SimpleVertex();
            SimpleVertexEx sv;
            SimpleVertexEx sv1;
            SimpleVertexEx sv2;

            SimpleVertexEx svex = new SimpleVertexEx();
            svex.initInner();
            svex.initArrayList();
            svex.initHashMap();
            svex.initEnum();

            // Test store
            System.out.println("iniciando STORE -------");
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
            System.out.print("1- Enter String");
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
            System.out.print("2- Enter String");
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
        System.out.println("result.svinner: " + result.getSvinner().getS() + "      toS: " + result.getSvinner().toString());
        assertEquals(0, sm.getDirtyCount());

        // recuperar el objeto en otra instancia
        String rid = ((IObjectProxy) result).___getRid();

        System.out.println("============================================================================");
        System.out.println("RID: " + rid);
        System.out.println("============================================================================");
        assertEquals(0, sm.getDirtyCount());

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("========= comienzo del get =================================================");
        SimpleVertexEx expResult = sm.get(SimpleVertexEx.class, rid);
        assertEquals(0, sm.getDirtyCount());
        System.out.println("========= fin del get =================================================");

        assertEquals(((IObjectProxy) expResult).___getRid(), rid);

        System.out.println("\n\n\n++++++++++++++++ result: " + result.getSvinner().toString());
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
        System.out.println("-1-");
        assertEquals(((IObjectProxy) expResult).___getRid(), ((IObjectProxy) result).___getRid());
        System.out.println("-2-");
        assertEquals(((IObjectProxy) expResult.getLooptest()).___getRid(), ((IObjectProxy) result.getLooptest()).___getRid());
        System.out.println("-3-");
        assertEquals(((IObjectProxy) expResult.getLooptest().getLooptest()).___getRid(), ((IObjectProxy) result).___getRid());
        System.out.println("============================= FIN LoopTest ===============================");
    }

    public void testDelete() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("delete objetos");
        System.out.println("***************************************************************");

//        SimpleVertex sv = new SimpleVertex();
//        SimpleVertex expResult = sv;
//
//        assertEquals(0, sm.getDirtyCount());
//
//        SimpleVertex result = sm.store(sv);
//
//        this.sm.commit();
//
//        System.out.println("Recuperar el objeto de la base");
//        String rid = ((IObjectProxy) result).___getRid();
//        expResult = this.sm.get(SimpleVertex.class, rid);
//
//        System.out.println("Eliminar el objeto: " + rid);
//        sm.delete(expResult);
//        sm.commit();
//
//        try {
//            sm.get(rid);
//            System.out.println("El objeto aún exite!!!");
//        } catch (UnknownRID urid) {
//            System.out.println("El objeto fue borrado!");
//        }

        System.out.println("Testeando ingegridad referencial...");
        
        // crear un objeto simple.
        SimpleVertex irSV = sm.store(new SimpleVertex());
        sm.commit();
        String irSVrid = sm.getRID(irSV);
        
        // crear el objeto que referenciará al primero
        SimpleVertexEx irSVEX = new SimpleVertexEx();
        irSVEX.setSvinner(irSV);
        
        SimpleVertexEx rirSVEX = sm.store(irSVEX);
        
        // liberar la referencia
        irSVEX = null;
        sm.commit();
        
        String rirSVEXrid = sm.getRID(rirSVEX);
        System.out.println("Referencia creada: "+rirSVEXrid+"-->"+irSVrid);
        
        pause();
        
        // intentar eliminar el objeto dependiente
        try {
            sm.delete(irSV);
            sm.commit();
            System.out.println("El objeto fue borrado y debería haber saltado una excepción");
        } catch(ReferentialIntegrityViolation riv) {
            System.out.println("ReferencialIntegrityViolation ");
            sm.rollback();
        }
        
        System.out.println("Verificar el RemoveOrphan sobre los vectores");
        System.out.println("rid principal: "+sm.getRID(rirSVEX));
        
        
        // persistir todo.
        System.out.println("inicializar el array list...");
        rirSVEX.initArrayList();
        sm.commit();
        
        rirSVEX = sm.get(SimpleVertexEx.class,sm.getRID(rirSVEX));
        String sRSV1 = sm.getRID(rirSVEX.getAlSV().get(0));
        SimpleVertex svToRemove = rirSVEX.getAlSV().get(1);
        String sRSV2 = sm.getRID(svToRemove);
        System.out.println("rid sv1: "+sRSV1);
        System.out.println("rid sv2: "+sRSV2);
        
        System.out.println("fin de la presistencia con los objetos referenciados");
        pause(); 
        
        System.out.println("quitar uno de los objetos...");
        System.out.println("resultado: "+rirSVEX.getAlSV().remove(svToRemove));
        
        System.out.println("persistir...");
        sm.commit();
        
        System.out.println("verificar que el objeto no exista");
        try {
            SimpleVertex rsv1borrado = sm.get(SimpleVertex.class, sRSV1);
        } catch (UnknownRID urid) {
            System.out.println("Exito! El objeto fue borrado.");
        }
        
    }

    
    private void pause() {
        System.out.println("presione ENTER para continuar...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
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

    private void testLongQuery() {
        
        for (int i = 0; i < 1000000; i++) {
            // crear un string
            String nombre = randomIdentifier();
            SimpleVertex s = new SimpleVertex(nombre);
            s.i = rand.nextInt(10000);
            
            sm.store(s);
            if (i % 100 == 0) {
                sm.commit();
                System.out.println("" + i);
            }

        }

    }

    // class variable
    final String lexicon = "aábcdeéfghiíjklmnñoópqrstuvwxyz";

    final java.util.Random rand = new java.util.Random();

    public String randomIdentifier() {
        StringBuilder builder = new StringBuilder();
        while (builder.toString().length() == 0) {
            int length = rand.nextInt(5) + 5;
            for (int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
            }
            
        }
        return builder.toString();
    }

    
    private void testMultiTran() {
        
        OrientGraphFactory fact = new OrientGraphFactory("remote:localhost/Test", "root", "toor").setupPool(1, 10);
        
        OrientGraph t1 = fact.getTx();
        OrientGraph t2 = fact.getTx();
        
        t1.addVertex("class:Test", "text","transac 1");
        t2.addVertex("class:Test", "text","transac 2");
        
        t1.commit();
        
        t2.rollback();
        
        t1.shutdown();
        t2.shutdown();
                
        
    }
    
    
    private void testRollbackEmbedded() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("test embedded rollback SObjects");
        System.out.println("***************************************************************");
        
        
        // guardarlo
        Foo foo = new Foo();
        
        System.out.println("\n\nIniciando Store ========================= \n\n");
        Foo rfoo = this.sm.store(foo);
        this.sm.commit();
        System.out.println("\n\nStore finalizado ========================= \n\n");
        String rid = this.sm.getRID(rfoo);
        // dereferenciar el objeto
        rfoo = null;
        
        // obtener nuevamente el objeto desde la base.
        System.out.println("\n\nIniciando GET ========================= \n\n");
        rfoo = this.sm.get(Foo.class, rid);
        System.out.println("\n\nGET FINALIZADO ========================= \n\n");
        rfoo.setText("rollback");
        
        System.out.println("\n\nINICIANDO ROLLBACK ========================= \n\n");
        this.sm.rollback();
        
        
    }
    
    private void testRollbackSVE() {
        System.out.println("\n\n\n");
        System.out.println("***************************************************************");
        System.out.println("Rollback Collections. Se restablecen los atributos que hereden de Collection.");
        System.out.println("***************************************************************");
        SimpleVertexEx sve = new SimpleVertexEx();
        sve.initEnum();
        sve.initInner();
        sve.initArrayList();
        sve.initHashMap();
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
        
        System.out.println(""+sve.alSV.size()+ " =|= "+stored.alSV.size());
        assertEquals(sve.alSV.size(), stored.alSV.size());
    }

    private void testTimeLoad() {
        long initTime = System.currentTimeMillis();
        List<SimpleVertexEx> svexs = sm.query(SimpleVertexEx.class);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Tiempo: "+(endTime - initTime) + " - Size: "+svexs.size()+"\n\n");
        
        // forzar la instanciación: 
        initTime = System.currentTimeMillis();
        for (SimpleVertexEx svex : svexs) {
            String trash = svex.getS()+", "+svex.getI()+", "+svex.getF()+", "+svex.getFecha();
        }
        endTime = System.currentTimeMillis();
        System.out.println("Tiempo: "+(endTime - initTime) + "\n\n");
    }
    
    
}
