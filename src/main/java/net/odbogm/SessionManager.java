/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.agent.TransparentDirtyDetectorAgent;
import net.odbogm.auditory.Auditor;
import net.odbogm.exceptions.ClassToVertexNotFound;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.NoOpenTx;
import net.odbogm.exceptions.NoUserLoggedIn;
import net.odbogm.exceptions.ReferentialIntegrityViolation;
import net.odbogm.exceptions.UnknownObject;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.exceptions.UnmanagedObject;
import net.odbogm.exceptions.VertexJavaClassNotFound;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.security.UserSID;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SessionManager implements IActions.IStore, IActions.IGet {

    private final static Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.SessionManager);
        }
    }

//    private OrientGraph graphdb;
    private OrientGraphFactory factory;

    // uso un solo objectMapper para ahorar memoria. Estas instancia se comparte entre las transacciones.
    private ObjectMapper objectMapper;
//    private String url;
//    private String user;
//    private String passwd;
//    

//    // cache de los objetos recuperados de la base. Si se encuentra en el caché en un get, se recupera desde 
//    // acá. En caso contrario se recupera desde la base.
//    private ConcurrentHashMap<String, WeakReference<Object>> objectCache = new ConcurrentHashMap<>();
//
//    private ConcurrentHashMap<String, Object> dirty = new ConcurrentHashMap<>();

//    // se utiliza para guardar los objetos recuperados durante un get a fin de evitar los loops
//    private int getTransactionCount = 0;
//    ConcurrentHashMap<String, Object> transactionCache = new ConcurrentHashMap<>();
//
//    // Los RIDs temporales deben ser convertidos a los permanentes en el proceso de commit
//    List<String> newrids = new ArrayList<>();
//
//    // determina si se está en el proceso de commit.
//    private boolean commiting = false;
//    private ConcurrentHashMap<Object, Object> commitedObject = new ConcurrentHashMap<>();
//
//    int newObjectCount = 0;

    public enum ActivationStrategy {
        ONMETHODACCESS,             // cada vez que se invoca a un método se verifica si hay cambio.
        CLASS_INSTRUMENTATION        // modifica con un agente la clase para agregar la detección de escritura
    }
    
    private ActivationStrategy activationStrategy = ActivationStrategy.ONMETHODACCESS;
    
    private List<WeakReference<Transaction>> openTransactionList = new ArrayList<>();
    private Transaction publicTransaction; 
    
    // usuario a registrar en la tabla de auditoría.
//    private Auditor auditor;

    // usuario logueado sobre el que se ejecutan los controles de seguridad si corresponden
    private UserSID loggedInUser;

    
    public SessionManager(String url, String user, String passwd) {
        this.init(url, user, passwd, 1, 10);
    }

    public SessionManager(String url, String user, String passwd, int minPool, int maxPool) {
        this.init(url, user, passwd, minPool, maxPool);
    }
    
    
    private void init(String url, String user, String passwd, int minPool, int maxPool) {
        LOGGER.log(Level.INFO, "ODBOGM Session Manager initialization...");
//        this.url = url;
//        this.user = user;
//        this.passwd = passwd;
        this.factory = new OrientGraphFactory(url, user, passwd).setupPool(minPool, maxPool);
//        vertexs = new ConcurrentHashMap<>();
//        edges = new HashMap<>();
//        this.factory.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
        this.objectMapper = new ObjectMapper();
        
    }

    /**
     * Establece la estrategia a utilizar para detectar los cambios en los objetos.
     * ONMETHODACCESS: cada vez que se invoca a un método se verifica si hay cambio.
     * ONCOMMIT:       cuando se invoca a un método se marca el objeto para ser verificado en el commit
     * 
     * @param as Estrategia de detección de dirty
     * @return this
     */
    public SessionManager setActivationStrategy(ActivationStrategy as) {
        
        return this.setActivationStrategy(as, true);
    }

    /**
     * Establece la estrategia a utilizar para detectar los cambios en los objetos.
     * ONMETHODACCESS: cada vez que se invoca a un método se verifica si hay cambio.
 ONCOMMIT:       cuando se invoca a un método se marca el objeto para ser verificado en el commit
     * 
     * @param as Estrategia de detección de dirty
     * @param loadAgent determina si se debe cargar el agente.
     * @return this
     */    
    public SessionManager setActivationStrategy(ActivationStrategy as, boolean loadAgent) {
        this.activationStrategy = as;
        LOGGER.log(Level.INFO, "ActivationStrategy using "+as);
        if (this.activationStrategy == ActivationStrategy.CLASS_INSTRUMENTATION && loadAgent) {
            TransparentDirtyDetectorAgent.initialize();
        }
        return this;
    }
    
    public ActivationStrategy getActivationStrategy() {
        return this.activationStrategy;
    }
    
    /**
     * Retorna el factory inicializado por el SessionManager
     * @return 
     */
    OrientGraphFactory getFactory() {
        return this.factory;
    }
    
    /**
     * Inicia una transacción contra el servidor.
     */
    public void begin() {
        // si no hay una transacción creada, abrir una...
        if (this.publicTransaction == null) {
            
            publicTransaction = getTransaction();
    //        graphdb.getRawGraph().activateOnCurrentThread();
    //        graphdb.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
    //        ODatabaseRecordThreadLocal.INSTANCE.set(graphdb.getRawGraph());
        } else {
            // en caso contrario, iniciar una transacción anidada.
            this.publicTransaction.begin();
        }
    }

    /**
     * Devuelve una transacción privada. Los objetos solicitados a través de esta transacción se mantienen en 
     * forma independiente de los recuperados en otras, pero se comparte la comunicación subyacente a la base 
     * de datos.
     * 
     * @return un objeto Transaction para operar.
     */    
    public Transaction getTransaction() {
        Transaction t = new Transaction(this);
        openTransactionList.add(new WeakReference<>(t));
        return t;
    }
    
//    /**
//     * Devuelve una transacción sobre una nueva conexión a la base
//     * solicitada al pool de transacciones
//     * @return Transaction
//     */
//    public Transaction getNewTxTransaction() {
//        return new Transaction(this);
//    }
    
    /**
     * Devuelve la transacción por defecto que está utilizando el SessionManager.
     * @return publicTransaction
     */
    public Transaction getCurrentTransaction() {
        return this.publicTransaction;
    }
    
    /**
     * Crea a un *NUEVO* vértice en la base de datos a partir del objeto. Retorna el RID del objeto que se agregó a la base.
     *
     * @param <T> clase base del objeto.
     * @param o objeto de referencia a almacenar.
     */
    @Override
    public synchronized <T> T store(T o) throws IncorrectRIDField, NoOpenTx, ClassToVertexNotFound {
        return this.publicTransaction.store(o);
    }

    /**
     * Remueve un vértice y todos los vértices apuntados por él y marcados con @RemoveOrphan
     *
     * @param toRemove referencia al objeto a remover
     */
    public void delete(Object toRemove) throws ReferentialIntegrityViolation, UnknownObject {
        this.publicTransaction.delete(toRemove);
    }
    
    
    /**
     * Marca un objecto como dirty para ser procesado en el commit
     *
     * @param o objeto de referencia.
     */
    public synchronized void setAsDirty(Object o) throws UnmanagedObject {
//        graphdb.getRawGraph().activateOnCurrentThread();
        this.publicTransaction.setAsDirty(o);
        
    }

    /**
     * Retorna el ObjectMapper asociado a la sesión
     *
     * @return retorna un mapa del objeto
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    /**
     * Recupera el @RID asociado al objeto. Se debe tener en cuenta que si el objeto aún no se ha persistido la base devolverá un RID temporal con los
     * ids en negativo. Ej: #-10:-2
     *
     * @param o objeto de referencia
     * @return el RID del objeto o null.
     */
    public String getRID(Object o) {
        if ((o != null) && (o instanceof IObjectProxy)) {
            return ((IObjectProxy) o).___getVertex().getId().toString();
        }
        return null;
    }

    /**
     * Persistir la información pendiente en la transacción
     *
     * @throws NoOpenTx si no hay una trnasacción abierta.
     */
    public synchronized void commit() throws NoOpenTx, OConcurrentModificationException {
        
//        this.graphdb.getRawGraph().activateOnCurrentThread();

        // bajar todos los objetos a los vértices
        // this.commitObjectChanges();
        // cambiar el estado a comiteando
        this.publicTransaction.commit();
    }

    /**
     * Transfiere todos los cambios de los objetos a las estructuras subyacentes.
     */
    public synchronized void flush() {
        
        this.publicTransaction.flush();
    }

    /**
     * Vuelve a cargar todos los objetos que han sido marcados como modificados con los datos desde las base.
     * Los objetos marcados como Dirty forman parte del siguiente commit
     */
    public synchronized void refreshDirtyObjects() {
        
        this.publicTransaction.refreshDirtyObjects();
    }

    
    /**
     * Vuelve a cargar el objeto con los datos desde las base.
     * Esta accion no se propaga sobre los objetos que lo componen.
     * 
     * @param o objeto recuperado de la base a ser actualizado.
     */
    public synchronized void refreshObject(IObjectProxy o) {
            
        this.publicTransaction.refreshObject(o);
    }
    
    
    
    /**
     * realiza un rollback sobre la transacción activa.
     */
    public synchronized void rollback() {
        this.publicTransaction.rollback();
        
    }

    /**
     * Finaliza la comunicación con la base. 
     * Todas las transacciones abiertas son ROLLBACK y finalizadas.
     */
    public void shutdown() {
//        for (WeakReference<Transaction> weakReference : openTransactionList) {
//            Transaction t = weakReference.get();
//            if (t!=null) {
//                t.();
//                t.rollback();
//            }
//        }
        this.factory.close();
    }

    public void getTxConflics() {
//        this.graphdb.getRawGraph().getTransaction().getCurrentRecordEntries()
    }

    /**
     * Devuelve el objeto de comunicación con la base.
     *
     * @return retorna la referencia directa al driver del la base.
     */
    public OrientGraph getGraphdb() {
        return this.publicTransaction.getGraphdb();
    }

    /**
     * Retorna la cantidad de objetos marcados como Dirty. Utilizado para los test
     * @return retorna la cantidad de objetos marcados para el próximo commit
     */
    public int getDirtyCount() {
        return this.publicTransaction.getDirtyCount();
    }

    
    /**
     * Recupera un objecto desde la base a partir del RID del Vértice.
     *
     * @param rid: ID del vértice a recupear
     * @return Retorna un objeto de la clase javaClass del vértice.
     */
    @Override
    public Object get(String rid) throws UnknownRID, VertexJavaClassNotFound {
        return this.publicTransaction.get(rid);
    }

    
    /**
     * Recupera un objeto a partir de la clase y el RID correspondiente.
     *
     * @param <T> clase a devolver
     * @param type clase a devolver
     * @param rid RID del vértice de la base
     * @return objeto de la clase T
     */
    @Override
    public <T> T get(Class<T> type, String rid) throws UnknownRID {
        return this.publicTransaction.get(type, rid);
    }
    
    
    @Override
    public <T> T getEdgeAsObject(Class<T> type, OrientEdge e) {
        return this.publicTransaction.getEdgeAsObject(type, e);
    }

    /**
     * Detecta los objetos que hayan cambiado y prepara el Vertex correspondiente para que sea enviado en el commit.
     */
//    private void commitObjectChanges() {
//        for (Map.Entry<String, Object> e : dirty.entrySet()) {
//            String rid = e.getKey();
//            IObjectProxy o = (IObjectProxy)e.getValue();
//            
//            // actualizar todos los objetos antes de bajarlos.
//            o.___commit();
//            
//        }
//    }
    
    

    /**
     * Realiza un query direto a la base de datos y devuelve el resultado directamente sin procesarlo.
     *
     * @param <T> clase a devolver
     * @param sql sentencia a ejecutar 
     * @return resutado de la ejecución de la sentencia SQL
     */
    public <T> T query(String sql) {
        
        return this.publicTransaction.query(sql);
    }

    /**
     * Ejecuta un comando que devuelve un número. El valor devuelto será el primero que se encuentre en la lista de resultado.
     *
     * @param sql comando a ejecutar
     * @param retVal nombre de la propiedad a devolver
     * @return retorna el valor de la propiedad indacada obtenida de la ejecución de la consulta
     *
     * ejemplo: int size = sm.query("select count(*) as size from TestData","size");
     */
    public long query(String sql, String retVal) {
        
        return this.publicTransaction.query(sql, retVal);
    }

    /**
     * Realiza un query direto a la base de datos y devuelve el resultado directamente sin procesarlo.
     *
     * @param <T> clase a devolver
     * @param sql sentencia a ejecutar
     * @param param parámetros a utilizar en el query
     * @return resutado de la ejecución de la sentencia SQL
     */
    public <T> T query(String sql, Object... param) {
        return this.publicTransaction.query(sql, param);
    }
    
    /**
     * Return all record of the reference class.
     * Devuelve todos los registros a partir de una clase base.
     *
     * @param <T> Reference class
     * @param clazz reference class 
     * @return return a list of object of the refecence class.
     */
    public <T> List<T> query(Class<T> clazz) {
        return this.publicTransaction.query(clazz);
    }

    /**
     * Devuelve todos los registros a partir de una clase base en una lista, filtrando los datos por lo que se agregue en el body.
     *
     * @param <T> clase base que se utilizará para el armado de la lista
     * @param clase clase base.
     * @param body cuerpo a agregar a la sentencia select. Ej: "where ...."
     * @return Lista con todos los objetos recuperados.
     */
    public <T> List<T> query(Class<T> clase, String body) {
        return this.publicTransaction.query(clase, body);
    }

    /**
     * Ejecuta un prepared query y devuelve una lista de la clase indicada.
     *
     * @param <T> clase de referencia para crear la lista de resultados
     * @param clase clase de referencia
     * @param sql comando a ejecutar 
     * @param param parámetros extras para el query parametrizado.
     * @return una lista de la clase solicitada con los objetos lazy inicializados.
     */
    public <T> List<T> query(Class<T> clase, String sql, Object... param) {
        
        return this.publicTransaction.query(clase, sql, param);
    }

    /**
         * Ejecuta un prepared query y devuelve una lista de la clase indicada.
         * Esta consulta acepta parámetros por nombre. 
         * Ej:
         * <pre> {@code 
         *  Map<String, Object> params = new HashMap<String, Object>();
         *  params.put("theName", "John");
         *  params.put("theSurname", "Smith");
         *
         *  graph.command(
         *       new OCommandSQL("UPDATE Customer SET local = true WHERE name = :theName and surname = :theSurname")
         *      ).execute(params)
         *  );
         *  }
         * </pre>
         * @param <T> clase de referencia para crear la lista de resultados
         * @param clase clase de referencia
         * @param sql comando a ejecutar
         * @param param parámetros extras para el query parametrizado.
         * @return una lista de la clase solicitada con los objetos lazy inicializados.
         */
        public <T> List<T> query(Class<T> clase, String sql, HashMap<String,Object> param) {
            return this.publicTransaction.query(clase, sql, param);
        }
    
    
    
    /**
     * Devuelve el objecto de definición de la clase en la base.
     *
     * @param clase nombre de la clase
     * @return OClass o null si la clase no existe
     */
    public OClass getDBClass(String clase) {
        return this.publicTransaction.getDBClass(clase);
    }

    /**
     * Comienza a auditar los objetos y los persiste con el nombre de usuario.
     *
     * @param user UserSID String only.
     */
    public void setAuditOnUser(String user) {
        this.publicTransaction.setAuditOnUser(user);
    }

    /**
     * Comienza a auditar los objetos y los persiste con el nombre de usuario actualmente logueado.
     *
     */
    public void setAuditOnUser() throws NoUserLoggedIn {

        this.publicTransaction.setAuditOnUser();
    }

    /**
     * Establece el usuario actualmente logueado.
     * 
     * @param usid referencia al usuario
     */
    public void setLoggedInUser(UserSID usid) {
        this.loggedInUser = usid;
    }

    /**
     * realiza una auditoría a partir del objeto indicado.
     *
     * @param o IOBjectProxy a auditar
     * @param at AuditType
     * @param label etiqueta de referencia
     * @param data objeto a loguear con un toString
     */
    public void auditLog(IObjectProxy o, int at, String label, Object data) {
        if (this.publicTransaction.isAuditing()) {
            this.publicTransaction.auditLog(o, at, label, data);
        }
    }

    /**
     * determina si se está guardando un log de auditoría
     * @return true si la auditoría está activa
     */
    public boolean isAuditing() {
        return this.publicTransaction.isAuditing();
    }
    
    Auditor getAuditor() {
        return this.publicTransaction.getAuditor();
    }
    
    public UserSID getLoggedInUser() {
        return this.loggedInUser;
    }
    
    
    public SessionManager setClassLevelLog(Class<?> clazz, Level level) {
        Logger L = LOGGER.getLogger( clazz.getName() );
        L.setLevel(level);
        return this;
    }
}
