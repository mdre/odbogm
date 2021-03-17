package net.odbogm;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Audit;
import net.odbogm.annotations.CascadeDelete;
import net.odbogm.annotations.RemoveOrphan;
import net.odbogm.audit.Auditor;
import net.odbogm.cache.ClassCache;
import net.odbogm.cache.ClassDef;
import net.odbogm.cache.SimpleCache;
import net.odbogm.exceptions.ClassToVertexNotFound;
import net.odbogm.exceptions.CollectionNotSupported;
import net.odbogm.exceptions.ConcurrentModification;
import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.NoOpenTx;
import net.odbogm.exceptions.NoUserLoggedIn;
import net.odbogm.exceptions.OdbogmException;
import net.odbogm.exceptions.ReferentialIntegrityViolation;
import net.odbogm.exceptions.UnknownObject;
import net.odbogm.exceptions.UnknownRID;
import net.odbogm.exceptions.UnmanagedObject;
import net.odbogm.exceptions.VertexJavaClassNotFound;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.proxy.ObjectProxyFactory;
import net.odbogm.security.SObject;
import net.odbogm.utils.ODBResultSet;
import net.odbogm.utils.ReflectionUtils;
import net.odbogm.utils.ThreadHelper;
import net.odbogm.utils.VertexUtils;

/**
 * Constituye un marco de control para los objetos recuperados.
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class Transaction implements IActions.IStore, IActions.IGet, IActions.IQuery {

    private final static Logger LOGGER = Logger.getLogger(Transaction.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.Transaction);
        }
    }

    // Es el único objectMapper para todo el SM. 
    private ObjectMapper objectMapper;

    // cache de los objetos recuperados de la base. Si se encuentra en el caché en un get, se recupera desde 
    // acá. En caso contrario se recupera desde la base.
    private SimpleCache objectCache = new SimpleCache();

    private ConcurrentHashMap<String, Object> dirty = new ConcurrentHashMap<>();

    // objetos que han sido registrados para ser eliminados por el commit.
    private ConcurrentHashMap<String, Object> dirtyDeleted = new ConcurrentHashMap<>();
    
    // utilizado para determinar si existen transacciones anidadas.
    private int nestedTransactionLevel = 0;
    
    // se utiliza para guardar los objetos recuperados durante un get a fin de evitar los loops
    private int getTransactionCount = 0;
    ConcurrentHashMap<String, Object> transactionLoopCache = new ConcurrentHashMap<>();
    
    // Los RIDs temporales deben ser convertidos a los permanentes en el proceso de commit
    private final List<String> newrids = new ArrayList<>();
    
    // mapa objeto -> proxy: contiene los objetos que se van almacenando mientras no se haga commit
    private final ConcurrentHashMap<Object, Object> storedObjects = new ConcurrentHashMap<>();
    
    // objects that must be updated after database commit before ending transaction's commit
    private final Set<IObjectProxy> afterCommit = ConcurrentHashMap.newKeySet();
    
    // Auditor asociado a la transacción
    private Auditor auditor;
    
    private final SessionManager sm;
    private ODatabaseSession orientdbTransact;
    
    // indica el nivel de anidamiento de la transacción. Cuando se llega a 0 se cierra.
    private int orientTransacLevel = 0;
    
    /**
     * Devuelve una transacción con su propio espacio de caché.
     *
     * @param sm
     */
    Transaction(SessionManager sm) {
        this.sm = sm;
        LOGGER.log(Level.FINER, "current thread: {0}", Thread.currentThread().getName());
        this.objectMapper = this.sm.getObjectMapper();
    }

    /**
     * Inicia una transacción interna activándola en el hilo actual.
     */
    public synchronized void initInternalTx() {
        if (this.orientdbTransact == null) {
            LOGGER.log(Level.FINEST, "\nAbriendo una transacción...");
            LOGGER.log(Level.FINEST, "\ntransacLevel: {0}  (siempre debería ser 0)",orientTransacLevel);
            //inicializar la transacción
            orientdbTransact = this.sm.getDBTx();
            orientdbTransact.begin();
            //orientdbTransact.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
            orientTransacLevel = 1;
        } else {
            //aumentar el anidamiento de transacciones
            activateOnCurrentThread();
//            LOGGER.log(Level.FINEST, "Anidando transacción: {0} --> {1} - transact light edges: {2}",
//                    new Object[]{orientTransacLevel, (orientTransacLevel + 1), 
//                        orientdbTransact!=null?orientdbTransact.getConfiguration().getValue(OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD):
//                                "orientdbTransact = null"
//                                });
            LOGGER.log(Level.FINEST, "transacLevel: {0} --> {1}",new Object[]{orientTransacLevel, (orientTransacLevel + 1)});
            orientTransacLevel++;
        }
    }
    
    /**
     * cierra la transacción sin realizar el commit
     */
    public synchronized void closeInternalTx() {
        if (orientdbTransact == null) return; //no se abrió todavía
        //FIXME: existe la posibilidad de que se intente cerrar la base con vertex nuevos creados. 
        //       se debería como mínimo tirar una excepción.
        if (orientTransacLevel>0) {
            LOGGER.log(Level.FINEST, "decrementando la transacción: {0} --> {1}",
                                     new Object[]{orientTransacLevel, orientTransacLevel-1}
                      );
            orientTransacLevel--;
        } else {
            LOGGER.log(Level.FINEST, "\n\n\n\n\n\n");
            LOGGER.log(Level.FINEST, "la transacción ya estaba en 0!!!!!  New RIDs: {0}",newrids.size());
            LOGGER.log(Level.FINEST, "\n\n\n\n\n\n");
        }
        
        LOGGER.log(Level.FINEST, "transacLevel: {0} - newRids: {1}",new Object[]{orientTransacLevel,newrids.size()});
        if (orientTransacLevel <= 0 && newrids.isEmpty()) {
            LOGGER.log(Level.FINEST, "termnando la transacción\n");
            orientdbTransact.close(); //close, no commit
            orientdbTransact = null;
            orientTransacLevel = 0;
        }
    }
    
    public synchronized void shutdownInternalTx() {
        if (orientdbTransact != null) {
            orientTransacLevel = 0;
            if (newrids.isEmpty()) {
                orientdbTransact.close();
                orientdbTransact = null;
            }
        }
    }
    
    
    /**
     * Elimina cualquier objeto que esté marcado como dirty en esta transacción
     */
    public void clear() {
        this.dirty.clear();
    }
    
    /**
     * Quita todas las referencias del cache. Todas los futuros get's recuperarán instancias nuevas de objetos dentro de la transacción. Las
     * referencias existentes seguirán funcionando normalmente.
     */
    public void clearCache() {
        this.objectCache.clear();
    }

    /**
     * Marca un objecto como dirty para ser procesado en el commit
     *
     * @param o objeto de referencia.
     */
    public synchronized void setAsDirty(Object o) throws UnmanagedObject {
        if (o instanceof IObjectProxy) {
            String rid = ((IObjectProxy) o).___getRid();
            LOGGER.log(Level.FINER, "Marcando como dirty: {0} - {1}",
                    new Object[]{o.getClass().getSimpleName(), o.toString()});
            LOGGER.log(Level.FINEST, ThreadHelper.getCurrentStackTrace());
            this.dirty.put(rid, o);
        } else {
            throw new UnmanagedObject();
        }
    }

    /**
     * Agrega un objeto al cache de la transacción
     *
     * @param rid record id
     * @param o objeto a referenciar
     */
    public synchronized void addToCache(String rid, Object o) {
        this.objectCache.add(rid, o);
    }

    /**
     * Remueve un objeto del cache de la transacción. Esto producirá que 
     * al ser solicitado nuevamente, se cargue desde la base de datos.
     *
     * @param rid RecordID del objeto a remover
     */
    public synchronized void removeFromCache(String rid) {
        this.objectCache.remove(rid);
    }

    /**
     * Recupera un objecto desde el cache o devuelve null si no lo encuentra
     *
     * @param rid RecordID a recuperar
     * @return el objeto si se encotró o null.
     */
    public Object getFromCache(String rid) {
        return this.objectCache.get(rid);
    }

    /**
     * Vuelve a cargar todos los objetos que han sido marcados como modificados con los datos desde las base. Los objetos marcados como Dirty forman
     * parte del siguiente commit
     */
    public synchronized void refreshDirtyObjects() {
        initInternalTx();
        
        for (Map.Entry<String, Object> e : dirty.entrySet()) {
            String rid = e.getKey();
            IObjectProxy o = (IObjectProxy) e.getValue();

            // actualizar todos los objetos a nivel de cache de la base sin proceder a 
            // bajarlos efectívamente.
            o.___reload();
        }
        
        closeInternalTx();
    }

    /**
     * Vuelve a cargar el vértice asociado al objeto con los datos desde las base. Esto no supone que se 
     * sobreescriban los atributos actuales del objeto.
     * Esta accion no se propaga sobre los objetos que lo componen.
     *
     * @param o objeto recuperado de la base a ser actualizado.
     */
    public synchronized void refreshObject(Object o) {
        initInternalTx();
        ((IObjectProxy)o).___reload();
        closeInternalTx();
    }

    /**
     * Devuelve un objeto de comunicación con la base.
     *
     * @return retorna la referencia directa al driver del la base.
     */
    public ODatabaseSession getCurrentGraphDb() {
        return orientdbTransact;
    }

    
    /**
     * Inicia una transacción anidada
     */
    public synchronized void begin() {
        this.nestedTransactionLevel++;
    }

    /**
     * Persistir la información pendiente en la transacción.
     *
     * @throws ConcurrentModification Si un elemento fue modificado por una transacción anterior.
     * @throws OdbogmException Si ocurre alguna otra excepción de la base de datos.
     */
    public synchronized void commit() throws ConcurrentModification, OdbogmException {
        try {
            LOGGER.log(Level.FINEST, "dirties: {0}",this.dirty);
            LOGGER.log(Level.FINEST, "news: {0}",this.newrids);
            doCommit();
        } catch (OException ex) {
            throw new OdbogmException(ex, this);
        }
    }
    
    
    private void doCommit() throws ConcurrentModification, OdbogmException {
        LOGGER.log(Level.FINER, "COMMIT");
        initInternalTx();
        
        if (this.nestedTransactionLevel <= 0) {
            LOGGER.log(Level.FINER, "Iniciando COMMIT ==================================");
            LOGGER.log(Level.FINER, "Objetos marcados como Dirty: {0}", dirty.size());
            LOGGER.log(Level.FINER, "Objetos marcados como DirtyDeleted: {0}", dirtyDeleted.size());
            
            // process all the dirty objects:
            for (Map.Entry<String, Object> e : dirty.entrySet()) {
                String rid = e.getKey();
                IObjectProxy o = (IObjectProxy) e.getValue();
                if (!o.___isDeleted() && o.___isValid()) {
                    LOGGER.log(Level.FINER, "Commiting: {0} class: {1} isValid: {2}", new Object[]{rid, o.___getBaseClass(), o.___isValid()});
                    // actualizar todos los objetos antes de bajarlos.
                    
                    
                    //@TODO: CHEQUEAR VERSIÓN YA ACÁ
                    
                    o.___commit();
                }
            }
            
            // process all objects to be deleted:
            for (Map.Entry<String, Object> e : dirtyDeleted.entrySet()) {
                String rid = e.getKey();
                IObjectProxy o = (IObjectProxy) e.getValue();
                if (o.___isDeleted() && o.___isValid()) {
                    LOGGER.log(Level.FINER, "Commiting delete: {0}", rid);
                    this.internalDelete(e.getValue());
                }
            }
            
            // fill the sequence fields for the newly created objects:
            newrids.forEach(s -> {
                Object o = getFromCache(s);
                if (o != null) {
                    IObjectProxy p = (IObjectProxy)o;
                    if (!p.___isDeleted() && p.___isValid()) {
                        this.objectMapper.fillSequenceFields(o, this, p.___getVertex());
                    }
                }
            });
            
            LOGGER.log(Level.FINER, "Fin persistencia. <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
            // comitear los vértices
            LOGGER.log(Level.FINER, "llamando al commit de la base"); //---------------------------------------
            try {
                this.orientdbTransact.commit();
            } catch (OConcurrentModificationException ex) {
                closeInternalTx();
                throw new ConcurrentModification(ex, this);
            }
            LOGGER.log(Level.FINER, "finalizado."); //-------------------------------------------------........

            // si se está en modalidad audit, grabar los logs
            if (this.isAuditing()) {
                LOGGER.log(Level.FINER, "grabando auditoría...");
                this.getAuditor().commit();
                LOGGER.log(Level.FINER, "finalizado.");
            }
            
            //cleaning:
            
            //process objects that depend on database commit
            this.afterCommit.stream().filter(o -> !o.___isDeleted() && o.___isValid()).
                    forEach(o -> o.___uptadeVersion());
            this.afterCommit.clear();
            
            //refresh references from cache
            String newRid;
            LOGGER.log(Level.FINER, "update NewRIDs: {0}", newrids.size());
            for (Iterator<String> iterator = newrids.iterator(); iterator.hasNext();) {
                String tempRid = iterator.next();
                if (getFromCache(tempRid) != null) {
                    // reemplazar el rid con el que le asignó la base luego de persistir el objeto
                    Object o = getFromCache(tempRid);
                    removeFromCache(tempRid);
                    newRid = sm.getRID(o);
                    addToCache(newRid, o);
                    LOGGER.log(Level.FINEST, "{0} --> {1}",new String[]{tempRid,newRid});
                    //actualizar rid inyectado si corresponde
                    ((IObjectProxy)o).___injectRid();
                }
            }
            newrids.clear();
            
            //remove dirty mark of commited elements
            this.dirty.entrySet().stream().
                    map(e -> (IObjectProxy)e.getValue()).
                    filter(o -> !o.___isDeleted() && o.___isValid()).
                    forEach(o -> o.___removeDirtyMark());
            
//            for (var e : this.dirty.entrySet()) {
//                IObjectProxy o = (IObjectProxy)e.getValue();
//                if (!o.___isDeleted() && o.___isValid()) {
//                    o.___updateIndirectLinks();
//                    o.___removeDirtyMark();
//                    Object oo = o.___getProxiedObject();
//                }
//            }
            
            this.dirtyDeleted.clear();
            this.dirty.clear();
            this.storedObjects.clear();
            
        } else {
            LOGGER.log(Level.FINER, "TransactionLevel: {0}", this.nestedTransactionLevel);
            this.nestedTransactionLevel--;
        }
        LOGGER.log(Level.FINER, "FIN DE COMMIT! ----------------------------\n\n");
        closeInternalTx();
    }

    /**
     * realiza un rollback sobre la transacción activa.
     */
    public synchronized void rollback() {
        initInternalTx();
        LOGGER.log(Level.FINER, "Rollback ------------------------");
        LOGGER.log(Level.FINER, "Dirty objects: {0}", dirty.size());
        LOGGER.log(Level.FINER, "Dirty deleted objects: {0}", dirtyDeleted.size());
        LOGGER.log(Level.FINER, "New objects: {0}", this.newrids.size());
        
        this.orientdbTransact.rollback();
        
        // refrescar todos los objetos
        for (Map.Entry<String, Object> entry : dirty.entrySet()) {
            IObjectProxy value = (IObjectProxy) entry.getValue();
            value.___rollback();
        }

        // clean the cache of new and modified objects
        this.dirty.clear();
        this.storedObjects.clear();
        this.newrids.clear();
        // clean the cache of objects to delete
        this.dirtyDeleted.clear();
        // clean the invalid entries of auditor cache
        if (this.isAuditing()) {
            getAuditor().rollback();
        }
        
        this.nestedTransactionLevel = 0;
        this.orientTransacLevel = 0;
        LOGGER.log(Level.FINER, "FIN ROLLBACK.");
        closeInternalTx();
    }

    /**
     * Retorna el nivel de transacciones anidadas. El nivel superior es 0.
     *
     * @return nivel de transacciones anidadas.
     */
    public int getTransactionLevel() {
        return this.nestedTransactionLevel;
    }

    /**
     * Crea a un *NUEVO* vértice en la base de datos a partir del objeto.
     * Retorna el objeto que se agregó a la base administrado por el OGM.
     *
     * @param <T> clase base del objeto.
     * @param o objeto de referencia a almacenar.
     */
    @Override
    public synchronized <T> T store(T o) throws IncorrectRIDField, NoOpenTx, ClassToVertexNotFound {
        LOGGER.log(Level.FINER, "*****************************************************************************");
        LOGGER.log(Level.FINER, "STORE: {0}",o);
        LOGGER.log(Level.FINER, "*****************************************************************************");
        
        LOGGER.log(Level.FINEST, "dirty: size: {0}",new Object[]{this.dirty.mappingCount()});
        
        T proxy;
        // si el objeto ya fue guardado con anterioridad, devolver la instancia creada previamente.
        proxy = (T) this.storedObjects.get(o);
        if (proxy != null) {
            // devolver la instancia recuperada
            LOGGER.log(Level.FINER, "El objeto original ya había sido persistido. Se devuelve la instancia creada inicialmente.");

            // Aplicar los controles de seguridad.
            if ((this.sm.getLoggedInUser() != null) && (proxy instanceof SObject)) {
                LOGGER.log(Level.FINER, "SObject detectado. Aplicando seguridad de acuerdo al usuario logueado: {0}",
                        this.sm.getLoggedInUser().getName());
                ((SObject) proxy).validate(this.sm.getLoggedInUser());
            }

            return proxy;
        }
        
        initInternalTx();
        try {
            // Recuperar la definición de clase del objeto.
            ClassDef oClassDef = this.objectMapper.getClassDef(o);
            String classname = oClassDef.entityName;
            LOGGER.log(Level.FINER, "STORE: guardando objeto de la clase {0}", classname);
            
            // verificar que la clase existe
            if (getDBClass(classname) == null) {
                // arrojar una excepción en caso contrario.
                closeInternalTx();
                throw new ClassToVertexNotFound("No se ha encontrado la definición de la clase " + classname + " en la base!");
            }
            
            // Obtener un map del objeto.
            ObjectStruct oStruct = this.objectMapper.objectStruct(o);
            Map<String, Object> omap = oStruct.fields;

            LOGGER.log(Level.FINER, "object data: {0}", omap);
            OVertex v = this.orientdbTransact.newVertex(classname);
            LOGGER.log(Level.FINEST, "virtual RID pre-save: {0}",v.getIdentity().toString());
            // forzar el fijado del rid temporal
            
            //completar el template del vertex con los datos.
            VertexUtils.fillElement(v,omap);
            LOGGER.log(Level.FINEST, "fill RID: {0}",v.getIdentity().toString());
            
            // fijar el RID temporal.
            v.save();
            LOGGER.log(Level.FINEST, "virtual RID: {0}",v.getIdentity().toString());
            
            proxy = ObjectProxyFactory.create(o, v, this);

            // registrar el rid temporal para futuras referencias.
            newrids.add(v.getIdentity().toString());

            //=====================================================
            // transferir todos los valores al proxy
            for (Map.Entry<String, Field> entry : oClassDef.fieldsObject.entrySet()) {
                Field f = entry.getValue();
                try {
                    f.set(proxy, f.get(o));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                }
            }
            //=====================================================
            
            
            // convertir los embedded
            this.objectMapper.collectionsToEmbedded(proxy, oClassDef);

            if (this.isAuditing()) {
                this.auditLog((IObjectProxy) proxy, Audit.AuditType.WRITE, "STORE", omap);
            }

            LOGGER.log(Level.FINER, "Marcando como dirty: {0}", proxy.getClass().getSimpleName());
            this.dirty.put(v.getIdentity().toString(), proxy);
            LOGGER.log(Level.FINER, "dirty.size: {0}",this.dirty.size());
            
            // si se está en proceso de commit, registrar el objeto junto con el proxy para 
            // que no se genere un loop con objetos internos que lo referencien.
            this.storedObjects.put(o, proxy);

            /* 
            procesar los objetos internos. Primero se debe determinar
            si los objetos ya existían en el contexto actual. Si no existen
            deben ser creados.
             */
            LOGGER.log(Level.FINER, "Procesando los Links");
            for (Map.Entry<String, Object> link : oStruct.links.entrySet()) {
                String field = link.getKey();
                String graphRelationName = classname + "_" + field;
                LOGGER.log(Level.FINER, "Link: {0}", field);

                // verificar si no formaba parte de los objetos que se están comiteando
                Object innerO = this.storedObjects.get(link.getValue());
                // si no es así, recuperar el valor del campo
                if (innerO == null) {
                    LOGGER.log(Level.FINER, "{0}: No existe el objeto en el cache de objetos creados.", field);
                    innerO = link.getValue();
                }

                // verificar si ya está en el contexto
                if (!(innerO instanceof IObjectProxy)) {
                    LOGGER.log(Level.FINER, "innerO nuevo. Crear un vértice y un link");
                    innerO = this.store(innerO);
                    // actualizar la referencia del objeto.
                    this.objectMapper.setFieldValue(proxy, field, innerO);
                } else {
                    this.objectMapper.setFieldValue(proxy, field, innerO);
                }

                // crear un link entre los dos objetos.
                OEdge oe = this.orientdbTransact.newEdge(v, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                if (this.isAuditing()) {
                    this.auditLog((IObjectProxy) proxy, Audit.AuditType.WRITE, "STORE: " + graphRelationName, oe);
                }
            }

            /* 
            procesar los LinkList. Primero se deber determinar
            si los objetos ya existían en el contexto actual. Si no existen
            deben ser creados.
             */
            LOGGER.log(Level.FINER, "Procesando los LinkList");
            LOGGER.log(Level.FINER, "LinkLists: {0}", oStruct.linkLists.size());

            final T finalProxy = proxy;

            for (Map.Entry<String, Object> link : oStruct.linkLists.entrySet()) {
                String field = link.getKey();
                Object value = link.getValue();

                final String graphRelationName = classname + "_" + field;

                LOGGER.log(Level.FINER, "field: {0} clase: {1}", new Object[]{field, value.getClass().getName()});
                if (value instanceof List) {
                    // crear un objeto de la colección correspondiente para poder trabajarlo
                    // Class<?> oColection = oClassDef.linkLists.get(field);
                    Collection innerCol = (Collection) value;

                    // recorrer la colección verificando el estado de cada objeto.
                    LOGGER.log(Level.FINER, "Nueva lista: {0}: {1} elementos", new Object[]{graphRelationName, innerCol.size()});
                    for (Object llObject : innerCol) {
                        IObjectProxy ioproxied;
                        // verificar si ya está en el contexto
                        // verificar si no formaba parte de los objetos que se están comiteando
                        Object llO = this.storedObjects.get(llObject);
                        // si no es así, recuperar el valor del campo
                        if (llO == null) {
                            llO = llObject;
                        }

                        if (!(llO instanceof IObjectProxy)) {
                            LOGGER.log(Level.FINER, "llObject nuevo. Crear un vértice y un link");
                            ioproxied = (IObjectProxy) this.store(llO);
                        } else {
                            ioproxied = (IObjectProxy) llO;
                        }

                        // crear un link entre los dos objetos.
                        LOGGER.log(Level.FINE, "-----> agregando un edge a: {0}", ioproxied.___getVertex().getIdentity());
                        OEdge oe = this.orientdbTransact.newEdge(v, ioproxied.___getVertex(), graphRelationName);
                        if (this.isAuditing()) {
                            this.auditLog((IObjectProxy) proxy, Audit.AuditType.WRITE, "STORE: " + graphRelationName, oe);
                        }
                    }
                } else if (value instanceof Map) {
                    HashMap innerMap = (HashMap) value;
                    innerMap.forEach(new BiConsumer() {
                        @Override
                        public void accept(Object imk, Object imV) {
                            // para cada entrada, verificar la existencia del objeto y crear un Edge.
                            IObjectProxy ioproxied;

                            // verificar si ya no se había guardardo
                            Object imO = storedObjects.get(imV);
                            // si no es así, recuperar el valor del campo
                            if (imO == null) {
                                imO = imV;
                            }
                            if (imO instanceof IObjectProxy) {
                                ioproxied = (IObjectProxy) imO;
                            } else {
                                LOGGER.log(Level.FINER, "Link Map Object nuevo. Crear un vértice y un link");
                                ioproxied = (IObjectProxy) store(imO);
                            }
                            // crear un link entre los dos objetos.
                            LOGGER.log(Level.FINER, "-----> agregando el edges de {0} para {1} key: {2}", new Object[]{v.getIdentity().toString(), ioproxied.___getVertex().toString(), imk});
                            OEdge oe = orientdbTransact.newEdge(v, ioproxied.___getVertex(), graphRelationName);
                            if (isAuditing()) {
                                auditLog((IObjectProxy) finalProxy, Audit.AuditType.WRITE, "STORE: " + graphRelationName, oe);
                            }
                            // agragar la key como atributo.
                            if (Primitives.PRIMITIVE_MAP.get(imk.getClass()) != null) {
                                LOGGER.log(Level.FINER, "la prop del edge es primitiva");
                                // es una primitiva, agregar el valor como propiedad
                                oe.setProperty("key", imk);
                            } else {
                                LOGGER.log(Level.FINER, "la prop del edge es un Objeto. Se debe mapear!! ");
                                // mapear la key y asignarla como propiedades
                                objectMapper.simpleMap(imk).entrySet().stream().forEach(entry -> {
                                    oe.setProperty(entry.getKey(),entry.getValue());
                                });
                            }
                        }
                    });

                }
                // convertir la colección a Lazy para futuras referencias.
                this.objectMapper.collectionToLazy(proxy, field, v, this);
            }

            // guardar el objeto en el cache. Se usa el RID como clave
            addToCache(v.getIdentity().toString(), proxy);
            
            //if we must update version field, enqueue in transaction
            if (oClassDef.versionField != null) {
                this.processAfterDbCommit((IObjectProxy)proxy);
            }
            
            // grabar! MMAPI no lo hace solo como tinker
            v.save();

        } catch (IllegalArgumentException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
            closeInternalTx();
            throw ex;
        }

        LOGGER.log(Level.FINER, "FIN del Store ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

        // Aplicar los controles de seguridad.
        if ((this.sm.getLoggedInUser() != null) && (proxy instanceof SObject)) {
            LOGGER.log(Level.FINER, "SObject detectado. Aplicando seguridad de acuerdo al usuario logueado: {0}", this.sm.getLoggedInUser().getName());
            ((SObject) proxy).validate(this.sm.getLoggedInUser());
        }
        closeInternalTx();
        return proxy;
    }
    
    /**
     * Remueve un vértice y todos los vértices apuntados por él y marcados con @RemoveOrphan
     * o @CascadeDelete.
     * La eliminación se produce recién cuando se ejecuta commit. Los vértices relacionados 
     * permanencen inalterados hasta ese momento.
     * 
     * @param toRemove referencia al objeto a remover
     */
     public void delete(Object toRemove) throws ReferentialIntegrityViolation, UnknownObject {
        LOGGER.log(Level.FINER, "Remove: {0}", toRemove.getClass().getName());
        // si no hereda de IObjectProxy, el objeto no pertenece a la base y no se debe hacer nada.
        if (toRemove instanceof IObjectProxy) {
            initInternalTx();
            
            String ridToRemove = ((IObjectProxy) toRemove).___getVertex().getIdentity().toString();
            LOGGER.log(Level.FINER, "RID to remove: {0}",ridToRemove);
            this.deleteTree(toRemove);
            
            // agregarlo a la lista de vertices a remover.
            this.dirtyDeleted.put(ridToRemove, toRemove);
            
            closeInternalTx();
        }
    }
    
    /**
     * método llamado por {@code delete} para marcar todo el árbol al que pertenece el objeto
     * pero sin agregar los nodos a la lista de dirtyDeleted.
     * 
     * @param toRemove
     * @throws UnknownObject 
     */ 
    private void deleteTree(Object toRemove) throws UnknownObject {
        initInternalTx();
        String ridToRemove = ((IObjectProxy) toRemove).___getVertex().getIdentity().toString();
        LOGGER.log(Level.FINER, "Remove: {0}:{1}", new String[]{toRemove.getClass().getName(),ridToRemove});

        // si no hereda de IObjectProxy, el objeto no pertenece a la base y no se debe hacer nada.
        if (toRemove instanceof IObjectProxy) {
            LOGGER.log(Level.FINER, "is a IObjectProxy. Proceed.");
            // verificar que la integridad referencial no se viole.
            OVertex ovToRemove = ((IObjectProxy) toRemove).___getVertex();
            // reacargar preventivamente el objeto.
//            ovToRemove.reload();
            
            // la IR se verificará en el internalDelete cuando se haga commit.
//            LOGGER.log(Level.FINER, "Referencias IN: " + ovToRemove.countEdges(Direction.IN));
//            if (ovToRemove.countEdges(Direction.IN) > 0) {
//                throw new ReferentialIntegrityViolation();
//            }

            // Activar todos los campos que estén marcados con CascadeDelete o RemoveOrphan para poder procesarlos.
            // obtener el classDef del objeto
            ClassDef classDef = this.objectMapper.getClassDef(toRemove);
            
            // analizar el objeto
            Field f;
            
            for (Map.Entry<String, Class<?>> entry : classDef.links.entrySet()) {
                try {
                    String field = entry.getKey();
                    f = ReflectionUtils.findField(toRemove.getClass(), field);

                    if (f.isAnnotationPresent(CascadeDelete.class) || f.isAnnotationPresent(RemoveOrphan.class)) {
                        LOGGER.log(Level.FINER, "CascadeDelete|RemoveOrphan presente. Activando el objeto...");
                        ((IObjectProxy) toRemove).___loadLazyLinks();
                    }
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(Transaction.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // procesar los linkslist
            for (Map.Entry<String, Class<?>> entry : classDef.linkLists.entrySet()) {
                try {
                    String field = entry.getKey();

                    f = classDef.fieldsObject.get(field);
                    f.setAccessible(true);

                    LOGGER.log(Level.FINER, "procesando campo: {0}", field);

                    // si hay una colección y corresponde hacer la cascada.
                    if (f.isAnnotationPresent(CascadeDelete.class) || f.isAnnotationPresent(RemoveOrphan.class)) {
                        LOGGER.log(Level.FINER, "CascadeDelete|RemoveOrphan presente. Activando el objeto...");
                        // activar el campo.
                        Collection oCol = (Collection) f.get(toRemove);
                        if (oCol != null) {
                            String garbage = oCol.toString();
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Transaction.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            // fin de la activación.
            
            // elimino el nodo de la base para que se actualicen los vértices a los que apuntaba.
            // de esta forma, los inner quedan libres y pueden ser borrados por un delete simple
            // ovToRemove.remove();
                    
            
            // procesar los links
            for (Map.Entry<String, Class<?>> entry : classDef.links.entrySet()) {
                try {
                    String field = entry.getKey();
                    f = ReflectionUtils.findField(toRemove.getClass(), field);
                    boolean acc = f.isAccessible();
                    f.setAccessible(true);

                    if (f.isAnnotationPresent(CascadeDelete.class)) {
                        // si se apunta a un objeto, removerlo
                        // CascadeDelete fuerza el borrado y falla si no puede. 
                        // Tiene precedencia sobre RemoveOrphan
                        Object value = f.get(toRemove);
                        if (value != null) {
                            this.deleteTree(value);
                        }
                    } else if (f.isAnnotationPresent(RemoveOrphan.class)) {
                        // si se apunta a un objeto, removerlo
                        Object value = f.get(toRemove);
                        if (value != null) {
                            try {
                                this.deleteTree(value);
                            } catch (ReferentialIntegrityViolation riv) {
                                LOGGER.log(Level.FINER, "RemoveOrphan: El objeto aún tiene vínculos.");
                                closeInternalTx();
                                throw new ReferentialIntegrityViolation(
                                        "RemoveOrphan: " + riv.getMessage(), this);
                            }
                        }
                    }
                    f.setAccessible(acc);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Transaction.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            // procesar los linkslist
            // la eliminación del Vertex actual elimina de la base los Edges correspondientes por lo que 
            // solo es necesario verificar si es necesario realizar una cascada en el borrado 
            // para eliminar vértices relacionads
            for (Map.Entry<String, Class<?>> entry : classDef.linkLists.entrySet()) {
                try {
                    String field = entry.getKey();
                    Class<? extends Object> fieldClass = entry.getValue();

//                    f = ReflectionUtils.findField(((IObjectProxy) toRemove).___getBaseObject().getClass(), field);
                    f = ReflectionUtils.findField(toRemove.getClass(), field);
                    boolean acc = f.isAccessible();
                    f.setAccessible(true);

                    LOGGER.log(Level.FINER, "procesando campo: " + field);

//                    Collection oCol = (Collection) f.get(((IObjectProxy) toRemove).___getBaseObject());
                    Object oCol = f.get(toRemove);

                    // si hay una colección y corresponde hacer la cascada.
                    if ((oCol != null) && (f.isAnnotationPresent(CascadeDelete.class))) {
                        if (oCol instanceof List) {
                            List oListCol = (List) oCol;
                            for (Object object : oListCol) {
                                this.deleteTree(object);
                            }

                        } else if (oCol instanceof Map) {
                            HashMap oMapCol = (HashMap) oCol;
                            oMapCol.forEach((k, v) -> {
                                this.deleteTree(v);
                            });

                        } else {
                            LOGGER.log(Level.FINER, "********************************************");
                            LOGGER.log(Level.FINER, "field: {0}", field);
                            LOGGER.log(Level.FINER, "********************************************");
                            closeInternalTx();
                            throw new CollectionNotSupported(oCol.getClass().getSimpleName());
                        }
                        f.setAccessible(acc);
                    } else if ((oCol != null) && (f.isAnnotationPresent(RemoveOrphan.class))) {

                        if (oCol instanceof List) {
                            List oListCol = (List) oCol;
                            for (Object object : oListCol) {
                                try {
                                    this.deleteTree(object);
                                } catch (ReferentialIntegrityViolation riv) {
                                    LOGGER.log(Level.FINER, "RemoveOrphan: El objeto aún tiene vínculos.");
                                    closeInternalTx();
                                    throw new ReferentialIntegrityViolation(
                                            "RemoveOrphan: " + riv.getMessage(), this);
                                }
                            }

                        } else if (oCol instanceof Map) {
                            HashMap oMapCol = (HashMap) oCol;
                            oMapCol.forEach((k, v) -> {
                                try {
                                    this.deleteTree(v);
                                } catch (ReferentialIntegrityViolation riv) {
                                    LOGGER.log(Level.FINER, "RemoveOrphan: El objeto aún tiene vínculos.");
                                    closeInternalTx();
                                    throw new ReferentialIntegrityViolation(
                                            "RemoveOrphan: " + riv.getMessage(), this);
                                }
                            });

                        } else {
                            LOGGER.log(Level.FINER, "********************************************");
                            LOGGER.log(Level.FINER, "field: {0}", field);
                            LOGGER.log(Level.FINER, "********************************************");
                            closeInternalTx();
                            throw new CollectionNotSupported(oCol.getClass().getSimpleName());
                        }
                        f.setAccessible(acc);
                    }

                } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            if (this.isAuditing()) {
                this.auditLog((IObjectProxy) toRemove, Audit.AuditType.DELETE, "DELETE", null);
            }

            //ovToRemove.remove(); movido arriba.
            // si tengo un RID, proceder a removerlo de las colecciones.
            this.dirty.remove(ridToRemove);
            this.removeFromCache(ridToRemove);
            
            // invalidar el objeto
            ((IObjectProxy) toRemove).___setDeletedMark();
            
            LOGGER.log(Level.FINER, "rid: "+ridToRemove+" removed succefully");
        } else {
            closeInternalTx();
            throw new UnknownObject(this);
        }
        
        closeInternalTx();
    }

    
    /**
     * Este es el método que efectívamente realiza el borrado. Es llamdo desde commit.
     * el método {@code delete} registra los objetos que luego serán enviados a este proceso 
     * para su efectiva eliminación.
     * 
     * @param toRemove
     * @throws ReferentialIntegrityViolation
     * @throws UnknownObject 
     */
    private void internalDelete(Object toRemove) throws ReferentialIntegrityViolation, UnknownObject {
        initInternalTx();
        
        LOGGER.log(Level.FINER, "Remove: {0}", toRemove.getClass().getName());

        // si no hereda de IObjectProxy, el objeto no pertenece a la base y no se debe hacer nada.
        if (toRemove instanceof IObjectProxy) {
            // verificar que la integridad referencial no se viole.
            OVertex ovToRemove = ((IObjectProxy) toRemove).___getVertex();
            String logRidToRemove = ovToRemove.getIdentity().toString();
            
            // reacargar preventivamente el objeto.
            //LOGGER.log(Level.FINEST, "pre-reload:  --(in: " + ovToRemove.countEdges(ODirection.IN) + ")-->( "+logRidToRemove + ")--(out: "+ ovToRemove.countEdges(ODirection.OUT)+"  )---->   ");            
            ovToRemove.reload();
            //LOGGER.log(Level.FINEST, "post-reload:  --(in: " + ovToRemove.countEdges(ODirection.IN) + ")-->( "+logRidToRemove + ")--(out: "+ ovToRemove.countEdges(Direction.OUT)+"  )---->   ");            
            
            //LOGGER.log(Level.FINER, "Referencias {0} IN: {1}", new String[]{ovToRemove.getIdentity().toString(),(""+ovToRemove.countEdges(ODirection.IN))});
            if (ovToRemove.getEdges(ODirection.IN).iterator().hasNext()) {
                closeInternalTx();
                throw new ReferentialIntegrityViolation(ovToRemove, this);
            }
            // Activar todos los campos que estén marcados con CascadeDelete o RemoveOrphan para poder procesarlos.
            // obtener el classDef del objeto
            ClassDef classDef = this.objectMapper.getClassDef(toRemove);
            
            // analizar el objeto
            Field f;
            
            // elimino el nodo de la base para que se actualicen los vértices a los que apuntaba.
            // de esta forma, los inner quedan libres y pueden ser borrados por un delete simple
            
            //LOGGER.log(Level.FINEST, "pre-remove:  --(in: " + ovToRemove.countEdges(Direction.IN) + ")-->( "+logRidToRemove+" )--(out: "+ ovToRemove.countEdges(Direction.OUT)+"  )---->   ");            
            
            // borrar todo los edges
            // eliminaod en la conversión a MMAPI. Se había agregado para solucionar el problema que se presentaba a borrar directamente
            // el vertex y que no actualzaba el los edges. Remover si todo da ok. 15/08/2020.
//            for (Iterator<Edge> it = ovToRemove.getEdges(Direction.OUT).iterator(); it.hasNext();) {
//                Edge ed = it.next();
//                ed.remove();
//            }
            ovToRemove.delete();
            //LOGGER.log(Level.FINEST, "post-remove:  --(in: " + ovToRemove.countEdges(Direction.IN) + ")-->( "+logRidToRemove+" )--(out: "+ ovToRemove.countEdges(Direction.OUT)+"  )---->   ");            

            //Lista de vértices a remover
            List<OVertex> vertexToRemove = new ArrayList<>();

            // procesar los links
            LOGGER.log(Level.FINEST, "procesar los links de {0}...",logRidToRemove);
            for (Map.Entry<String, Class<?>> entry : classDef.links.entrySet()) {
                try {
                    String field = entry.getKey();
//                    f = ReflectionUtils.findField(((IObjectProxy) toRemove).___getBaseObject().getClass(), field);
                    f = ReflectionUtils.findField(toRemove.getClass(), field);
                    boolean acc = f.isAccessible();
                    f.setAccessible(true);

                    if (f.isAnnotationPresent(CascadeDelete.class)) {
                        // si se apunta a un objeto, removerlo
                        // CascadeDelete fuerza el borrado y falla si no puede. 
                        // Tiene precedencia sobre RemoveOrphan
                        LOGGER.log(Level.FINEST, "links: cascade delete detected");
                        Object value = f.get(toRemove);
                        if (value != null) {
                            this.internalDelete(value);
                        }
                    } else if (f.isAnnotationPresent(RemoveOrphan.class)) {
                        // si se apunta a un objeto, removerlo
                        Object value = f.get(toRemove);
                        if (value != null) {
                            try {
                                this.internalDelete(value);
                            } catch (ReferentialIntegrityViolation riv) {
                                LOGGER.log(Level.FINER, "RemoveOrphan: El objeto aún tiene vínculos.");
                                closeInternalTx();
                                throw new ReferentialIntegrityViolation(
                                        "RemoveOrphan: " + riv.getMessage(), this);
                            }
                        }
                    }
                    f.setAccessible(acc);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Transaction.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            LOGGER.log(Level.FINEST, "<-------------- fin links de {0}...",logRidToRemove);
            // procesar los linkslist
            // la eliminación del Vertex actual elimina de la base los Edges correspondientes por lo que 
            // solo es necesario verificar si es necesario realizar una cascada en el borrado 
            // para eliminar vértices relacionads
            LOGGER.log(Level.FINEST, "procesar los linkslist de {0}...",logRidToRemove);
            for (Map.Entry<String, Class<?>> entry : classDef.linkLists.entrySet()) {
                try {
                    String field = entry.getKey();
                    Class<? extends Object> fieldClass = entry.getValue();

//                    f = ReflectionUtils.findField(((IObjectProxy) toRemove).___getBaseObject().getClass(), field);
                    f = ReflectionUtils.findField(toRemove.getClass(), field);
                    boolean acc = f.isAccessible();
                    f.setAccessible(true);

                    LOGGER.log(Level.FINER, "procesando campo: " + field);

//                    Collection oCol = (Collection) f.get(((IObjectProxy) toRemove).___getBaseObject());
                    Object oCol = f.get(toRemove);

                    // si hay una colección y corresponde hacer la cascada.
                    if ((oCol != null) && (f.isAnnotationPresent(CascadeDelete.class))) {
                        if (oCol instanceof List) {
                            List oListCol = (List) oCol;
                            for (Object object : oListCol) {
                                LOGGER.log(Level.FINEST, "-----> invocar a InternaDelete desde {0}",logRidToRemove);
                                OVertex ovSendToRemove = ((IObjectProxy) object).___getVertex();
                                //LOGGER.log(Level.FINEST, "-----> {0} in: {1}", new Object[]{ovSendToRemove.getIdentity().toString(), ovSendToRemove.countEdges(Direction.IN)});
                                this.internalDelete(object);
                                LOGGER.log(Level.FINEST, "<----- volviendo a InternaDelete de {0}",logRidToRemove);
                            }

                        } else if (oCol instanceof Map) {
                            HashMap oMapCol = (HashMap) oCol;
                            oMapCol.forEach((k, v) -> {
                                this.internalDelete(v);
                            });

                        } else {
                            LOGGER.log(Level.FINER, "********************************************");
                            LOGGER.log(Level.FINER, "field: {0}", field);
                            LOGGER.log(Level.FINER, "********************************************");
                            closeInternalTx();
                            throw new CollectionNotSupported(oCol.getClass().getSimpleName());
                        }
                        f.setAccessible(acc);
                    } else if ((oCol != null) && (f.isAnnotationPresent(RemoveOrphan.class))) {

                        if (oCol instanceof List) {
                            List oListCol = (List) oCol;
                            for (Object object : oListCol) {
                                try {
                                    this.internalDelete(object);
                                } catch (ReferentialIntegrityViolation riv) {
                                    LOGGER.log(Level.FINER, "RemoveOrphan: El objeto aún tiene vínculos.");
                                    closeInternalTx();
                                    throw new ReferentialIntegrityViolation(
                                            "RemoveOrphan: " + riv.getMessage(), this);
                                }
                            }

                        } else if (oCol instanceof Map) {
                            HashMap oMapCol = (HashMap) oCol;
                            oMapCol.forEach((k, v) -> {
                                try {
                                    this.internalDelete(v);
                                } catch (ReferentialIntegrityViolation riv) {
                                    LOGGER.log(Level.FINER, "RemoveOrphan: El objeto aún tiene vínculos.");
                                    closeInternalTx();
                                    throw new ReferentialIntegrityViolation(
                                            "RemoveOrphan: " + riv.getMessage(), this);
                                }
                            });

                        } else {
                            LOGGER.log(Level.FINER, "********************************************");
                            LOGGER.log(Level.FINER, "field: {0}", field);
                            LOGGER.log(Level.FINER, "********************************************");
                            closeInternalTx();
                            throw new CollectionNotSupported(oCol.getClass().getSimpleName());
                        }
                        f.setAccessible(acc);
                    }

                } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
                    closeInternalTx();
                    throw new OdbogmException("Error eliminando vértice.", this);
                }
            }
            LOGGER.log(Level.FINEST, "<-------- fin linkslist de {0}...",logRidToRemove);

        } else {
            closeInternalTx();
            throw new UnknownObject(this);
        }
        
        closeInternalTx();
    }
    
    
    
    
    /**
     * Transfiere todos los cambios de los objetos a las estructuras subyacentes.
     */
    public synchronized void flush() {
        initInternalTx();
        
        for (Map.Entry<String, Object> e : dirty.entrySet()) {
            String rid = e.getKey();
            IObjectProxy o = (IObjectProxy) e.getValue();

            // actualizar todos los objetos a nivel de cache de la base sin proceder a 
            // bajarlos efectívamente.
            o.___commit();
        }
        
        closeInternalTx();
    }

    /**
     * retorna el SessionManager asociado a la transacción
     *
     * @return sm
     */
    public SessionManager getSessionManager() {
        return this.sm;
    }

    /**
     * Devuelve el ObjectMapper asociado a la transacción
     *
     * @return objectMapper
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    /**
     * Agrega si no existe un objeto al cache de la transacción acutal a fin de evitar los loops cuando se comletan los links dentro del ObjectProxy
     *
     * @param rid RID del objeto.
     * @param o objeto a controlar.
     */
    public void addToTransactionCache(String rid, Object o) {
        getTransactionCount++;
        if (this.transactionLoopCache.get(rid) == null) {
            LOGGER.log(Level.FINER, "Forzando el agregado al TransactionLoopCache de " + rid + "  hc:" + System.identityHashCode(o));
            this.transactionLoopCache.put(rid, o);
        }
    }

    /**
     * invocado desde ObjectProxy al completar los links.
     */
    public void decreseTransactionCache() {
        getTransactionCount--;
        if (getTransactionCount == 0) {
            transactionLoopCache.clear();
        }
    }
    
    /**
     * Enqueues a proxy object to be processed after database commit.
     * @param proxy 
     */
    public void processAfterDbCommit(IObjectProxy proxy) {
        this.afterCommit.add(proxy);
    }

//    /**
//     * Vincula el elemento a la transacción actual.
//     * @param e 
//     */
//    public void attach(OElement e) {
//        this.orientdbTransact.attach(e);
//    }
//    
    /**
     * Retorna la cantidad de objetos marcados como Dirty. Utilizado para los test
     *
     * @return retorna la cantidad de objetos marcados para el próximo commit
     */
    public int getDirtyCount() {
        return this.dirty.size();
    }
    
    /**
     * Retorna la cantidad de objetos marcados para ser eliminados cuando se cierre la transacción. Utilizado para los test
     *
     * @return retorna la cantidad de objetos marcados para ser eliminados en el próximo commit
     */
    public int getDirtyDeletedCount() {
        return this.dirtyDeleted.size();
    }

    /**
     * Returns the dirty objects in the transaction.
     * @return 
     */
    public Collection<Object> getDirty() {
        return this.dirty.values();
    }
    
    /**
     * Only for debug. Returns the dirty objects cache.
     *
     * @return Map of rids and the identity hash code of the dirty objects.
     */
    public Map<String, Object> getDirtyCache() {
        Map <String, Object> dc = new HashMap();
        for (Map.Entry<String, Object> entry : this.dirty.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            dc.put(key, System.identityHashCode(value));
        }
        return dc;
    }

    /**
     * Recupera un objecto desde la base a partir del RID del Vértice. El objeto se recupera a partir del cache de objetos de la transacción.
     *
     * @param rid: ID del vértice a recupear
     * @return Retorna un objeto de la clase javaClass del vértice.
     * @throws UnknownRID Si se pasa un RID nulo, o si no se encuentra ningún vértice
     * con ese RID.
     */
    @Override
    public Object get(String rid) throws UnknownRID, VertexJavaClassNotFound {
        if (rid == null) {
            throw new UnknownRID("The rid must not be null",this);
        }
        
        initInternalTx();
        Object ret = null;
        try {
            if (getFromCache(rid) != null) {
                ret = getFromCache(rid);
                
                // actualizar los indirects
                // ((IObjectProxy)ret).___updateIndirectLinks();
                
                // si fue recuperado del caché, determinar si se ha modificado.
                // si no fue modificado, hacer un reload para actualizar con la última 
                // versión de la base de datos.
                if (!((IObjectProxy) ret).___isDirty()) {
                    ((IObjectProxy) ret).___reload();
                }
            }

            // si ret == null, recuperar el objeto desde la base, en caso contrario devolver el objeto desde el caché
            if (ret == null) {
                OVertex v = null;
                try {
                    ORID toLoad = new ORecordId(rid);
                    v = this.orientdbTransact.load(toLoad);
                } catch (Exception e) {
                    v = null;
                }
                
                if (v == null) {
                    closeInternalTx();
                    throw new UnknownRID(rid, this);
                }
                String javaClass = v.getSchemaType().get().getCustom("javaClass");
                if (javaClass == null) {
                    closeInternalTx();
                    throw new VertexJavaClassNotFound("La clase del Vértice no tiene la propiedad javaClass");
                }
                javaClass = javaClass.replaceAll("[\'\"]", "");
                Class<?> c = Class.forName(javaClass);
                ret = this.get(c, rid);
            } else {
                LOGGER.log(Level.FINER, "Objeto Recupeardo del caché.");
                // validar contra el usuario actualmente logueado si corresponde.
                if ((this.sm.getLoggedInUser() != null) && (ret instanceof SObject)) {
                    ((SObject) ret).validate(this.sm.getLoggedInUser());
                }
            }

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        closeInternalTx();
        return ret;
    }

    /**
     * Recupera un objeto a partir de la clase y el RID correspondiente.
     * El objeto se recupera a partir del cache de objetos de la transacción.
     *
     * @param <T> clase a devolver
     * @param type clase a devolver
     * @param rid RID del vértice de la base
     * @return objeto de la clase T
     * @throws UnknownRID si no se encuentra un vértice con ese rid
     */
    @Override
    public synchronized <T> T get(Class<T> type, String rid) throws UnknownRID {
        return get(type, rid, false);
    }
    
    
    /**
     * Like get, but fetches fully all the links of the vertex, including collections.
     * 
     * @param <T>
     * @param type
     * @param rid
     * @return
     * @throws UnknownRID 
     */
    @Override
    public synchronized <T> T fetch(Class<T> type, String rid) {
        return get(type, rid, true);
    }
    
    
    private synchronized <T> T get(Class<T> type, String rid, boolean fullLoad) throws UnknownRID {
        if (rid == null) {
            throw new UnknownRID(this);
        }
        
        initInternalTx();
        T o = null;

        // si está en el caché, devolver la referencia desde ahí.
        if (getFromCache(rid) != null) {
            LOGGER.log(Level.FINER, "Objeto Recupeardo del caché: {0} {1}",
                    new Object[]{rid, type.getSimpleName()});
            o = (T) getFromCache(rid);

            // actualizar los indirects
            // ((IObjectProxy)o).___updateIndirectLinks();
            
            // si fue recuperado del caché, determinar si se ha modificado.
            // si no fue modificado, hacer un reload para actualizar con la última 
            // versión de la base de datos.
            if (!((IObjectProxy) o).___isDirty()) {
                ((IObjectProxy) o).___reload();
            }
        }

        if (o == null) {
            // iniciar el conteo de gets. Todos los gets se guardan en un mapa 
            // para impedir que un único get entre en un loop cuando un objeto
            // tiene referencias a su padre.
            getTransactionCount++;

            LOGGER.log(Level.FINER, "Obteniendo objeto type: {0} en RID: {1}",
                    new Object[]{type.getSimpleName(), rid});

            // verificar si ya no se ha cargado
            o = (T) this.transactionLoopCache.get(rid);

            if (o == null) {
                // recuperar el vértice solicitado
                ORID toLoad = new ORecordId(rid);
                OVertex v = this.orientdbTransact.load(toLoad);
                if (v == null) {
                    closeInternalTx();
                    throw new UnknownRID(rid, this);
                }
                
                // hidratar un objeto
                try {
                    o = objectMapper.hydrate(type, v, this);
                    
                    //@TODO: if can't create proxy throw exception and stop

                } catch (InstantiationException | IllegalAccessException | NoSuchFieldException ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                LOGGER.log(Level.FINER, "Objeto recuperado del loop cache! : {0}",
                        o.getClass().getSimpleName());
            }
            getTransactionCount--;
            if (getTransactionCount == 0) {
                LOGGER.log(Level.FINER, "Fin de la transacción. Reseteando el loop cache...");
                this.transactionLoopCache.clear();
            }

            // cuardar el objeto en el caché
            LOGGER.log(Level.FINER, "Agregando el objeto al cache de objetos de la transacción: {0}: ihc: {1}",
                    new Object[]{rid, System.identityHashCode(o)});
            addToCache(rid, o);
            
            if (fullLoad) {
                ((IObjectProxy) o).___fullLoad();
            } else {
                //trigger eager load of configured links
                ((IObjectProxy) o).___eagerLoad();
            }
        }

        // Aplicar los controles de seguridad.
        LOGGER.log(Level.FINER, "Verificar la seguridad: LoggedIn: {0} SObject: {1}",
                new Object[]{this.sm.getLoggedInUser() != null, o instanceof SObject});
        if ((this.sm.getLoggedInUser() != null) && (o instanceof SObject)) {
            LOGGER.log(Level.FINER, "SObject detectado. Aplicando seguridad de acuerdo al usuario logueado: {0}",
                    this.sm.getLoggedInUser().getName());
            ((SObject) o).validate(this.sm.getLoggedInUser());
        }

        LOGGER.log(Level.FINER, "Auditar?");
        if (this.isAuditing()) {
            LOGGER.log(Level.FINER, "loguear datos...");
            this.auditLog((IObjectProxy) o, Audit.AuditType.READ, "READ", null);
        }
        LOGGER.log(Level.FINER, "fin auditoría.");
        
        LOGGER.log(Level.FINER, "Fin get: {0} : {1} ihc: {2}^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n",
                new Object[]{rid, type.getSimpleName(), System.identityHashCode(o)});
        closeInternalTx();
        return o;
    }

    
    public <T> T getEdgeAsObject(Class<T> type, OEdge e) {
        initInternalTx();
        T o = null;
        try {
            o = this.objectMapper.hydrate(type, e, this);

            //apply security controls
            if ((this.sm.getLoggedInUser() != null) && (o instanceof SObject)) {
                ((SObject) o).validate(this.sm.getLoggedInUser());
            }
            
            //if we must update version field, enqueue in transaction
            if (this.objectMapper.getClassDef(type).versionField != null) {
                processAfterDbCommit((IObjectProxy)o);
            }

        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        closeInternalTx();
        return o;
    }

    
    /**
     * Realiza un query direto a la base de datos y devuelve el resultado directamente sin procesarlo.
     *
     * @param sql sentencia a ejecutar
     * @return resutado de la ejecución de la sentencia SQL
     */
    @Override
    public ODBResultSet query(String sql) {
        initInternalTx();
        // usar una transacción interna para que el iterable pueda seguir funcionando
        // por fuera del OGM
        ODatabaseSession localtx = this.orientdbTransact;
        localtx.activateOnCurrentThread();
        flush();

        //OCommandSQL osql = new OCommandSQL(sql);
        //ODBOrientDynaElementIterable ret = new ODBOrientDynaElementIterable(localtx,localtx.command(osql).execute());
        LOGGER.log(Level.FINEST, "sql: {0}",sql);
        ODBResultSet ret = new ODBResultSet(localtx, localtx.query(sql));
        
        closeInternalTx();
        return ret;
    }

    
    @Override
    public ODBResultSet query(String sql, Object... params) {
        initInternalTx();
        // usar una transacción interna para que el iterable pueda seguir funcionando por fuera del OGM
        ODatabaseSession localtx = this.sm.getDBTx();
        localtx.activateOnCurrentThread();
        flush();

        ODBResultSet ret = new ODBResultSet(localtx, localtx.query(sql, params));
        closeInternalTx();
        return ret; 
    }
    
    public ODBResultSet query(String sql, Map params) {
        initInternalTx();
        ODatabaseSession localtx = this.sm.getDBTx();
        localtx.activateOnCurrentThread();
        flush();
        ODBResultSet ret = new ODBResultSet(localtx, localtx.query(sql, params));
        closeInternalTx();
        return ret; 
    }
    

    /**
     * Ejecuta un comando que devuelve un número. El valor devuelto será el primero que se encuentre en la lista de resultado.
     *
     * @param sql comando a ejecutar
     * @param retVal nombre de la propiedad a devolver. Puede ser "". En ese caso se devolverá el primer valor encontrado.
     * @return retorna el valor de la propiedad indacada obtenida de la ejecución de la consulta
     *
     * ejemplo: int size = sm.query("select count(*) as size from TestData","size");
     */
    @Override
    public long query(String sql, String retVal) {
        initInternalTx();
        this.flush();
        
        //OCommandSQL osql = new OCommandSQL(sql);
        OResultSet ors = this.orientdbTransact.query(sql);
        OResult or = ors.next();
        if (retVal.isEmpty()) {
            retVal = or.getPropertyNames().iterator().next();
        }
        long ret = or.getProperty(retVal);
        ors.close();
        closeInternalTx();
        return ret;
    }

    /**
     * Return all record of the reference class with all subclasses. 
     * Devuelve todos los registros a partir de una clase base con todos los de las subclases.
     *
     * @param <T> Reference class
     * @param clazz reference class
     * @return return a list of object of the refecence class.
     */
    @Override
    public <T> List<T> query(Class<T> clazz) {
        initInternalTx();
        this.flush();

        long init = System.currentTimeMillis();

        ArrayList<T> ret = new ArrayList<>();

        OResultSet vertices = this.orientdbTransact.query("select from ?",ClassCache.getEntityName(clazz));
        LOGGER.log(Level.FINER, "Enlapsed ODB response: " + (System.currentTimeMillis() - init));
        vertices.stream().forEach(vertexOfClass->{
            ret.add(this.get(clazz, vertexOfClass.getIdentity().get().toString()));
        }); 
        LOGGER.log(Level.FINER, "Enlapsed time query to List: " + (System.currentTimeMillis() - init));
        vertices.close();
        closeInternalTx();
        return ret;
    }
    
    /**
     * Devuelve todos los registros a partir de una clase base en una lista, filtrando los datos por lo que se agregue en el body.
     *
     * @param <T> clase base que se utilizará para el armado de la lista
     * @param clase clase base.
     * @param body cuerpo a agregar a la sentencia select. Ej: "where ...."
     * @return Lista con todos los objetos recuperados.
     */
    @Override
    public <T> List<T> query(Class<T> clase, String body) {
        initInternalTx();
        this.flush();

        ArrayList<T> ret = new ArrayList<>();

        String cSQL = "SELECT FROM " + ClassCache.getEntityName(clase) + " " + body;
        LOGGER.log(Level.FINER, cSQL);
        OResultSet ors  = this.orientdbTransact.query(cSQL);
        ors.stream().forEach(v->{
            ret.add(this.get(clase, v.getIdentity().get().toString()));
        });
        ors.close();
        closeInternalTx();
        return ret;
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
    @Override
    public <T> List<T> query(Class<T> clase, String sql, Object... param) {
        initInternalTx();

        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(sql);
        ArrayList<T> ret = new ArrayList<>();
        
        LOGGER.log(Level.FINER, sql + " param: " + param);
        OResultSet ors = this.orientdbTransact.command(sql,param);
        ors.stream().forEach(v->{
            ret.add(this.get(clase, v.getIdentity().get().toString()));
        });
        ors.close();
        closeInternalTx();
        return ret;
    }

    /**
     * Ejecuta un prepared query y devuelve una lista de la clase indicada. Esta consulta acepta parámetros por nombre. Ej:
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
     *
     * @param <T> clase de referencia para crear la lista de resultados
     * @param clase clase de referencia
     * @param sql comando a ejecutar
     * @param params parámetros extras para el query parametrizado.
     * @return una lista de la clase solicitada con los objetos lazy inicializados.
     */
    @Override
    public <T> List<T> query(Class<T> clase, String sql, Map params) {
        initInternalTx();

        ArrayList<T> ret = new ArrayList<>();

        LOGGER.log(Level.FINER, sql);
        try (OResultSet ors = this.orientdbTransact.command(sql, params)) {
            ors.stream().forEach(v -> ret.add(this.get(clase, v.getIdentity().get().toString())));
        }
        closeInternalTx();
        return ret;
    }

    /**
     * Devuelve el objecto de definición de la clase en la base.
     *
     * @param clase nombre de la clase
     * @return OClass o null si la clase no existe
     */
    public OClass getDBClass(String clase) {
        LOGGER.log(Level.FINEST, "object class: {0}",clase);
        initInternalTx();
        OClass ret = orientdbTransact.getClass(clase);
        closeInternalTx();
        LOGGER.log(Level.FINEST, "^^^^^^^ end.");
        return ret;
    }

    // Procesos de auditoría
    
    /**
     * Comienza a auditar los objetos y los persiste con el nombre de usuario dado.
     * Si se pasa un null, se desactiva la auditoría.
     *
     * @param user UserSID String only.
     */
    public void setAuditOnUser(String user) {
        this.auditor = user != null ? new Auditor(this, user) : null;
    }

    /**
     * Comienza a auditar los objetos y los persiste con el nombre del usuario 
     * actualmente logueado.
     
     * @throws NoUserLoggedIn Si no hay ningún usuario logueado.
     */
    public void setAuditOnUser() throws NoUserLoggedIn {
        if (this.sm.getLoggedInUser() == null) {
            throw new NoUserLoggedIn();
        }
        this.auditor = new Auditor(this, this.sm.getLoggedInUser().getUUID());
    }

    /**
     * Realiza una auditoría a partir del objeto indicado.
     *
     * @param o IOBjectProxy a auditar
     * @param at AuditType
     * @param label Etiqueta de referencia
     * @param data Objeto a loguear con un toString
     */
    public synchronized void auditLog(IObjectProxy o, int at, String label, Object data) {
        if (this.isAuditing()) {
            auditor.auditLog(o, at, label, data);
        }
    }

    /**
     * Determina si se está guardando un log de auditoría.
     * @return true Si la auditoría está activa.
     */
    public boolean isAuditing() {
        return this.auditor != null;
    }

    Auditor getAuditor() {
        return this.auditor;
    }

    /**
     * Asegurarse que la base esté activa en el thread en el que se encuentra la transacción
     */
    private void activateOnCurrentThread() {
        if (!orientdbTransact.isActiveOnCurrentThread()) {
            LOGGER.log(Level.FINEST, "Activando en el Thread actual...");
            LOGGER.log(Level.FINEST, "current thread: " + Thread.currentThread().getName());
            orientdbTransact.activateOnCurrentThread();
        } else {
            LOGGER.log(Level.FINEST, "base activada previamente en el Thread actual");
        }
    }

    /**
     * Retorna el caché actual de objetos existentes en la transacción.Se utiliza para debug.
     *
     * @return una referencia al WeakHashMap
     */
    public Map getObjectCache() {
        return this.objectCache.getCachedObjects();
    }

    public Transaction setCacheCleanInterval(int seconds) {
        this.objectCache.setTimeInterval(seconds);
        return this;
    }
    
    
//    public void attach(final OElement element) {
//        
//        orientdbTransact.attach(element);
//    }
    
    
}
