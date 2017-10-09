/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import net.odbogm.annotations.CascadeDelete;
import net.odbogm.exceptions.NoOpenTx;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.ReferentialIntegrityViolation;
import net.odbogm.exceptions.UnknownObject;
import net.odbogm.cache.ClassDef;
import net.odbogm.exceptions.CollectionNotSupported;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.exceptions.UnmanagedObject;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.proxy.ObjectProxyFactory;
import net.odbogm.utils.ReflectionUtils;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientConfigurableGraph;
import com.tinkerpop.blueprints.impls.orient.OrientDynaElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Audit;
import net.odbogm.auditory.Auditor;
import net.odbogm.exceptions.ClassToVertexNotFound;
import net.odbogm.exceptions.NoUserLoggedIn;
import net.odbogm.exceptions.VertexJavaClassNotFound;
import net.odbogm.security.SObject;
import net.odbogm.security.UserSID;
import net.odbogm.utils.ThreadHelper;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class SessionManager implements Actions.Store, Actions.Get {

    private final static Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.SessionManager);
    }

    private OrientGraph graphdb;
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

    private Transaction publicTransaction; 
    
    // usuario a registrar en la tabla de auditoría.
    private Auditor auditor;

    // usuario logueado sobre el que se ejecutan los controles de seguridad si corresponden
    private UserSID loggedInUser;

    public SessionManager(String url, String user, String passwd) {
//        this.url = url;
//        this.user = user;
//        this.passwd = passwd;
        this.factory = new OrientGraphFactory(url, user, passwd).setupPool(1, 10);
//        vertexs = new ConcurrentHashMap<>();
//        edges = new HashMap<>();
//        this.factory.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
        this.objectMapper = new ObjectMapper();
    }

    private void init() {
        this.publicTransaction.clear();
    }

    /**
     * Inicia una transacción contra el servidor.
     */
    public void begin() {
        graphdb = factory.getTx();
        graphdb.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
        publicTransaction = new Transaction(this);
//        graphdb.getRawGraph().activateOnCurrentThread();
//        graphdb.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
//        ODatabaseRecordThreadLocal.INSTANCE.set(graphdb.getRawGraph());
    }

    /**
     * Devuelve una transacción privada. Los objetos solicitados a través de esta transacción se mantienen en 
     * forma independiente de los recuperados en otras.
     * 
     * @return un objeto Transaction para operar.
     */
    
    public Transaction getTransaction() {
        return new Transaction(this);
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
        if (graphdb == null) {
            throw new NoOpenTx();
        }
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
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.publicTransaction.flush();
    }

    /**
     * Vuelve a cargar todos los objetos que han sido marcados como modificados con los datos desde las base.
     * Los objetos marcados como Dirty forman parte del siguiente commit
     */
    public synchronized void refreshDirtyObjects() {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.publicTransaction.refreshDirtyObjects();
    }

    
    /**
     * Vuelve a cargar el objeto con los datos desde las base.
     * Esta accion no se propaga sobre los objetos que lo componen.
     * 
     * @param o objeto recuperado de la base a ser actualizado.
     */
    public synchronized void refreshObject(IObjectProxy o) {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
            
        this.publicTransaction.refreshObject(o);
    }
    
    
    
    /**
     * realiza un rollback sobre la transacción activa.
     */
    public synchronized void rollback() {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.publicTransaction.rollback();
        
    }

    /**
     * Finaliza la comunicación con la base.
     */
    public void shutdown() {
        graphdb.shutdown();
        this.init();
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
        return graphdb;
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
     * Devuelve el objecto de definición de la clase en la base.
     *
     * @param clase nombre de la clase
     * @return OClass o null si la clase no existe
     */
    public OClass getDBClass(String clase) {
        return graphdb.getRawGraph().getMetadata().getSchema().getClass(clase);
    }

    /**
     * Comienza a auditar los objetos y los persiste con el nombre de usuario.
     *
     * @param user User String only.
     */
    public void setAuditOnUser(String user) {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.auditor = new Auditor(this, user);

    }

    /**
     * Comienza a auditar los objetos y los persiste con el nombre de usuario actualmente logueado.
     *
     */
    public void setAuditOnUser() throws NoUserLoggedIn {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        if (this.loggedInUser == null) {
            throw new NoUserLoggedIn();
        }

        this.auditor = new Auditor(this, this.loggedInUser.getUUID());
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
        if (this.isAuditing()) {
            auditor.auditLog(o, at, label, data);
        }
    }

    /**
     * determina si se está guardando un log de auditoría
     * @return true si la auditoría está activa
     */
    public boolean isAuditing() {
        return this.auditor != null;
    }
    
    Auditor getAuditor() {
        return this.auditor;
    }
    
    public UserSID getLoggedInUser() {
        return this.loggedInUser;
    }
}
