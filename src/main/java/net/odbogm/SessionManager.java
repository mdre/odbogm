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
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
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
import net.odbogm.exceptions.ClassToVertexNotFound;
import net.odbogm.exceptions.VertexJavaClassNotFound;
import org.reflections.Reflections;

/**
 *
 * @author SShadow
 */
public class SessionManager implements Actions.Store, Actions.Get {

    private final static Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    static {
        LOGGER.setLevel(Level.FINER);
    }

    private OrientGraph graphdb;
    private OrientGraphFactory factory;

    private ObjectMapper objectMapper;
//    private String url;
//    private String user;
//    private String passwd;
//    
    // Working objects
//    private ConcurrentHashMap<String, OrientVertex> vertexs = new ConcurrentHashMap<>();
//    private ConcurrentHashMap<String, OrientEdge> edges = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Object> dirty = new ConcurrentHashMap<>();
//    private ConcurrentHashMap<Object, String> entitiesToRid = new ConcurrentHashMap<>();

    // se utiliza para guardar los objetos recuperados durante un get a fin de evitar los loops
    private int getTransactionCount = 0;
    ConcurrentHashMap<String, Object> getTransactionCache = new ConcurrentHashMap<>();

    // Los RIDs temporales deben ser convertidos a los permanentes en el proceso de commit
    List<String> newrids = new ArrayList<>();

    // determina si se está en el proceso de commit.
    private boolean commiting = false;
    private ConcurrentHashMap<Object, Object> commitedObject = new ConcurrentHashMap<>();

    int newObjectCount = 0;

    private Reflections declaredClasses;

    public SessionManager(String url, String user, String passwd) {
//        this.url = url;
//        this.user = user;
//        this.passwd = passwd;

        this.factory = new OrientGraphFactory(url, user, passwd).setupPool(1, 10);
//        vertexs = new ConcurrentHashMap<>();
//        edges = new HashMap<>();
//        this.factory.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
        this.objectMapper = new ObjectMapper(this);
    }

    /**
     * Crea una mapa con todas las clases de referencia que se encuentran en los paquetes que se pasan como parámetro.
     *
     * @param ref
     */
    public void setDeclaredClasses(Object... ref) {
        declaredClasses = new Reflections(ref);
//        if (LOGGER.getLevel() == Level.FINER) {
//            LOGGER.log(Level.FINER, "**********************************************************************");
//            LOGGER.log(Level.FINER, "* Clases escaneadas");
//            LOGGER.log(Level.FINER, "**********************************************************************");
//            Set<String> dc = declaredClasses.getAllTypes();
//            LOGGER.log(Level.FINER, "Clases: "+dc.size());
//            dc.forEach(clazz->LOGGER.log(Level.FINER, ""+clazz));
//            LOGGER.log(Level.FINER, "------ FIN CLASES escaneadas------");
//        }
    }

    /**
     * Devuelve el listado de las clases que se cargaron como referecia.
     *
     * @return
     */
    public Reflections getDeclaredClasses() {
        return declaredClasses;
    }

    private void init() {
//        vertexs.clear();
//        edges.clear();

        dirty.clear();
//        entitiesToRid.clear();
    }

    /**
     * Inicia una transacción contra el servidor.
     */
    public void begin() {
        graphdb = factory.getTx();
        graphdb.getRawGraph().activateOnCurrentThread();
//        graphdb.setThreadMode(OrientConfigurableGraph.THREAD_MODE.ALWAYS_AUTOSET);
//        ODatabaseRecordThreadLocal.INSTANCE.set(graphdb.getRawGraph());
    }

    /**
     * Crea a un *NUEVO* vértice en la base de datos a partir del objeto. Retorna el RID del objeto que se agregó a la base.
     *
     * @param <T>
     * @param o
     */
    @Override
    public <T> T store(T o) throws IncorrectRIDField, NoOpenTx, ClassToVertexNotFound {
        graphdb.getRawGraph().activateOnCurrentThread();
        T proxied = null;
        try {
            // si no hay una tx abierta, disparar una excepción
            if (this.graphdb == null) {
                throw new NoOpenTx();
            }

            String classname;
            if (o instanceof IObjectProxy) {
                classname = o.getClass().getSuperclass().getSimpleName();
            } else {
                classname = o.getClass().getSimpleName();
            }

            // Recuperar la definición de clase del objeto.
            ClassDef oClassDef = this.objectMapper.getClassDef(o);

            // Obtener un map del objeto.
            ObjectStruct oStruct = objectMapper.objectStruct(o);
            Map<String, Object> omap = oStruct.fields;

            // verificar que la clase existe
            if (this.getDBClass(classname) == null) {
                // arrojar una excepción en caso contrario.
                throw new ClassToVertexNotFound("No se ha encontrado la definición de la clase "+classname+" en la base!");
                //graphdb.createVertexType(classname);
            }

            OrientVertex v = graphdb.addVertex("class:" + classname, omap);

            proxied = ObjectProxyFactory.create(o, v, this);
            // transferir todos los valores al proxy
            ReflectionUtils.copyObject(o, proxied);

            LOGGER.log(Level.FINER, "Marcando como dirty: " + proxied.getClass().getSimpleName());
            this.dirty.put(v.getId().toString(), proxied);

            // si se está en proceso de commit, registrar el objeto junto con el proxi para 
            // que no se genere un loop con objetos internos que lo referencien.
            this.commitedObject.put(o, proxied);

            /* 
            procesar los objetos internos. Primero se deber determinar
            si los objetos ya existían en el contexto actual. Si no existen
            deben ser creados.
             */
            LOGGER.log(Level.FINER, "Procesando los Links");
            for (Map.Entry<String, Object> link : oStruct.links.entrySet()) {
                String field = link.getKey();
                String graphRelationName = classname + "_" + field;

                // verificar si no formaba parte de los objetos que se están comiteando
                Object innerO = this.commitedObject.get(link.getValue());
                // si no es así, recuperar el valor del campo
                if (innerO == null) {
                    innerO = link.getValue();
                }

                // verificar si ya está en el contexto
                if (!(innerO instanceof IObjectProxy)) {
                    LOGGER.log(Level.FINER, "innerO nuevo. Crear un vértice y un link");
                    innerO = this.store(innerO);
                    // actualizar la referencia del objeto.
                    ObjectMapper.setFieldValue(proxied, field, innerO);

//                    innerRID = ((IObjectProxy)innerO).___getVertex().getId().toString();
                }
                // crear un link entre los dos objetos.
                this.graphdb.addEdge("", v, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
            }

            /* 
            procesar los LinkList. Primero se deber determinar
            si los objetos ya existían en el contexto actual. Si no existen
            deben ser creados.
             */
            LOGGER.log(Level.FINER, "Procesando los LinkList");
            for (Map.Entry<String, Object> link : oStruct.linkLists.entrySet()) {
                String field = link.getKey();
                Object value = link.getValue();
                final String graphRelationName = classname + "_" + field;

                if (value instanceof List) {
                    // crear un objeto de la colección correspondiente para poder trabajarlo
//                Class<?> oColection = oClassDef.linkLists.get(field);
                    Collection innerCol = (Collection) value;

                    // recorrer la colección verificando el estado de cada objeto.
                    for (Object llObject : innerCol) {
                        IObjectProxy ioproxied;
                        // verificar si ya está en el contexto
                        // verificar si no formaba parte de los objetos que se están comiteando
                        Object llO = this.commitedObject.get(llObject);
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
                        LOGGER.log(Level.FINE, "-----> agregando un LinkList!");
                        this.graphdb.addEdge("", v, ioproxied.___getVertex(), graphRelationName);
                    }
                } else if (value instanceof Map) {
                    HashMap innerMap = (HashMap) value;
//                    final String ffield = field;
//                    final String frid = rid;
//                    final ConcurrentHashMap<String, OrientVertex> fVertexs = vertexs;
                    innerMap.forEach(new BiConsumer() {
                        @Override
                        public void accept(Object imk, Object imV) {
                            // para cada entrada, verificar la existencia del objeto y crear un Edge.
                            IObjectProxy ioproxied;

                            // verificar si ya no se había guardardo
                            Object imO = commitedObject.get(imV);
                            // si no es así, recuperar el valor del campo
                            if (imO == null) {
                                imO = imV;
                            }
                            if (imO instanceof IObjectProxy) {
                                ioproxied = (IObjectProxy) imO;
                            } else {
                                LOGGER.log(Level.FINER, "Link Map Object nuevo. Crear un vértice y un link");
                                ioproxied = (IObjectProxy) SessionManager.this.store(imO);
                            }
                            // crear un link entre los dos objetos.
                            LOGGER.log(Level.FINER, "-----> agregando el edges de " + v.getId().toString() + " para " + ioproxied.___getVertex().toString() + " key: " + imk);
                            OrientEdge oe = SessionManager.this.graphdb.addEdge("", v, ioproxied.___getVertex(), graphRelationName);

                            // agragar la key como atributo.
                            if (Primitives.PRIMITIVE_MAP.get(imk.getClass()) != null) {
                                LOGGER.log(Level.FINER, "la prop del edge es primitiva");
                                // es una primitiva, agregar el valor como propiedad
                                oe.setProperty("key", imk);
                            } else {
                                LOGGER.log(Level.FINER, "la prop del edge es un Objeto. Se debe mapear!! ");
                                // mapear la key y asignarla como propiedades
                                oe.setProperties(objectMapper.simpleMap(imk));
                            }
                        }
                    });

                }
                // convertir la colección a Lazy para futuras referencias.
                this.objectMapper.colecctionToLazy(proxied, field, v);
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return proxied;
    }

    /**
     * Marca un objecto como dirty para ser procesado en el commit
     *
     * @param rid
     * @param o
     */
    public void setAsDirty(Object o) throws UnmanagedObject {
        graphdb.getRawGraph().activateOnCurrentThread();
        if (o instanceof IObjectProxy) {
            String rid = ((IObjectProxy) o).___getVertex().getId().toString();
            LOGGER.log(Level.FINER, "Marcando como dirty: " + o.getClass().getSimpleName() + " - " + o.toString());
            this.dirty.put(rid, o);
        } else {
            throw new UnmanagedObject();
        }
    }

    /**
     * Retorna el ObjectMapper asociado a la sesión
     *
     * @return
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    /**
     * Recupera el @RID asociado al objeto. Se debe tener en cuenta que si el objeto aún no se ha persistido la base devolverá un RID temporal con los
     * ids en negativo. Ej: #-10:-2
     *
     * @param o
     * @return
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
     * @throws NoOpenTx
     */
    public void commit() throws NoOpenTx, OConcurrentModificationException {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.graphdb.getRawGraph().activateOnCurrentThread();

        // bajar todos los objetos a los vértices
        // this.commitObjectChanges();
        // cambiar el estado a comiteando
        this.commiting = true;
        LOGGER.log(Level.FINER, "Objetos marcados como Dirty: " + dirty.size());
        for (Map.Entry<String, Object> e : dirty.entrySet()) {
            String rid = e.getKey();
            IObjectProxy o = (IObjectProxy) e.getValue();
            LOGGER.log(Level.FINER, "Commiting: " + rid + "   class: " + o.___getBaseClass());
            // actualizar todos los objetos antes de bajarlos.
            o.___commit();
        }
        LOGGER.log(Level.FINER, "Fin persistencia. <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        // comitear los vértices
        graphdb.commit();

        // vaciar el caché de elementos modificados.
        this.dirty.clear();
        this.commiting = false;
        this.commitedObject.clear();
    }

    /**
     * Transfiere todos los cambios de los objetos a las estructuras subyacentes.
     */
    public void flush() {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        for (Map.Entry<String, Object> e : dirty.entrySet()) {
            String rid = e.getKey();
            IObjectProxy o = (IObjectProxy) e.getValue();

            // actualizar todos los objetos antes de bajarlos.
            o.___commit();

        }
    }

    /**
     * realiza un rollback sobre la transacción activa.
     */
    public void rollback() {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        // primero revertir todos los vértices
        graphdb.rollback();

        // refrescar todos los objetos
        for (Map.Entry<String, Object> entry : dirty.entrySet()) {
            String key = entry.getKey();
            IObjectProxy value = (IObjectProxy) entry.getValue();
            value.___rollback();
        }

        // limpiar el caché de objetos modificados
        this.dirty.clear();
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
     * @return
     */
    public OrientGraph getGraphdb() {
        return graphdb;
    }

    /**
     * Retorna la cantidad de objetos marcados como Dirty.
     * Utilizado para los test
     */
    public int getDirtyCount() {
        return this.dirty.size();
    }
    
    
    /**
     * Recupera un objecto desde la base a partir del RID del Vértice.
     * 
     * @param rid: ID del vértice a recupear
     * @return: Retorna un objeto de la clase javaClass del vértice.
     */
    @Override
    public Object get(String rid) throws UnknownRID, VertexJavaClassNotFound {
        try {
            Object ret=null;
            if (this.graphdb == null) {
                throw new NoOpenTx();
            }
            if (rid == null) {
                throw new UnknownRID();
            }
            OrientVertex v = graphdb.getVertex(rid);
            if (v==null) {
                throw new UnknownRID(rid);
            }
            String javaClass = v.getType().getCustom("javaClass");
            if (javaClass==null) {
                throw new VertexJavaClassNotFound("La clase del Vértice no tiene la propiedad javaClass");
            }
            javaClass=javaClass.replaceAll("[\'\"]", "");
            Class<?> c = Class.forName(javaClass);
            return this.get(c, rid);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Recupera un objeto a partir de la clase y el RID correspondiente.
     *
     * @param <T>
     * @param type
     * @param rid
     * @return
     */
    @Override
    public <T> T get(Class<T> type, String rid) throws UnknownRID {
        if (this.graphdb == null) {
            throw new NoOpenTx();
        }
        if (rid == null) {
            throw new UnknownRID();
        }
        // iniciar el conteo de gets. Todos los gets se guardan en un mapa 
        // para impedir que un único get entre en un loop cuando un objeto
        // tiene referencias a su padre.
        getTransactionCount++;

        LOGGER.log(Level.FINER, "Obteniendo objeto type: " + type.getSimpleName() + " en RID: " + rid);
        T o = null;

        // verificar si ya no se ha cargado
        o = (T) this.getTransactionCache.get(rid);

        if (o == null) {
            // recuperar el vértice solicitado
            OrientVertex v = graphdb.getVertex(rid);

            // hidratar un objeto
            try {
                o = objectMapper.hydrate(type, v);
//                entities.put(rid, o);
            } catch (InstantiationException | IllegalAccessException | NoSuchFieldException ex) {
                Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.log(Level.FINER, "Objeto recuperado del dirty cache! : " + o.getClass().getSimpleName());
        }
        getTransactionCount--;
        if (getTransactionCount == 0) {
            this.getTransactionCache.clear();
        }
        return o;
    }

    @Override
    public <T> T getEdgeAsObject(Class<T> type, OrientEdge e) {
        if (this.graphdb == null) {
            throw new NoOpenTx();
        }

        T o = null;
        try {
            // verificar si ya no se ha cargado
            o = this.objectMapper.hydrate(type, e);
        } catch (InstantiationException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return o;
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
     * Remueve un vértice y todos los vértices apuntados por él y marcados con @RemoveOrphan
     *
     * @param toRemove
     */
    public void delete(Object toRemove) throws ReferentialIntegrityViolation, UnknownObject {
        LOGGER.log(Level.FINER, "Remove: " + toRemove.getClass().getName());

        // si no hereda de IObjectProxy, el objeto no pertenece a la base y no se debe hacer nada.
        if (toRemove instanceof IObjectProxy) {
            // verificar que la integridad referencial no se viole.
            OrientVertex ovToRemove = ((IObjectProxy) toRemove).___getVertex();

            if (ovToRemove.countEdges(Direction.IN) > 1) {
                throw new ReferentialIntegrityViolation();
            }

            // obtener el classDef del objeto
            ClassDef classDef = this.objectMapper.getClassDef(toRemove);

            //Lista de vértices a remover
            List<OrientVertex> vertexToRemove = new ArrayList<>();

            // analizar el objeto
            Field f;

            // procesar los links
            for (Map.Entry<String, Class<?>> entry : classDef.links.entrySet()) {
                try {
                    String field = entry.getKey();
//                    f = ReflectionUtils.findField(((IObjectProxy) toRemove).___getBaseObject().getClass(), field);
                    f = ReflectionUtils.findField(toRemove.getClass(), field);

                    if (f.isAnnotationPresent(CascadeDelete.class)) {
                        // si se apunta a un objeto, removerlo
                        /* FIXME: hay dos posibilidades. Una es remover si está huérfano
                            la otra es remover aún cuando al objeto haya relaciones desde otros
                            objetos.
                         */
                        Object value = f.get(toRemove);
                        if (value != null) {
                            this.delete(value);
                        }
                    }

                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            // procesar los linkslist
            // la eliminación del Vertex actual elimina de la base los Edges correspondientes por lo que 
            // solo es necesario verificar si es necesario realizar una cascada en el borrado 
            // para eliminar vértices relacionads
            for (Map.Entry<String, Class<?>> entry : classDef.linkLists.entrySet()) {
                try {
                    String field = entry.getKey();
                    final String graphRelationName = toRemove.getClass().getSimpleName() + "_" + field;
                    Class<? extends Object> fieldClass = entry.getValue();

//                    f = ReflectionUtils.findField(((IObjectProxy) toRemove).___getBaseObject().getClass(), field);
                    f = ReflectionUtils.findField(toRemove.getClass(), field);
                    boolean acc = f.isAccessible();
                    f.setAccessible(true);

                    LOGGER.log(Level.FINER, "procesando campo: " + field);

//                    Collection oCol = (Collection) f.get(((IObjectProxy) toRemove).___getBaseObject());
                    Collection oCol = (Collection) f.get(toRemove);

                    // si hay una colección y corresponde hacer la cascada.
                    if ((oCol != null) && (f.isAnnotationPresent(CascadeDelete.class))) {
                        if (oCol instanceof List) {
                            for (Object object : oCol) {
                                this.delete(object);
                            }

                        } else if (oCol instanceof Map) {
                            HashMap oMapCol = (HashMap) oCol;
                            oMapCol.forEach((k, v) -> {
                                this.delete(v);
                            });

                        } else {
                            LOGGER.log(Level.FINER, "********************************************");
                            LOGGER.log(Level.FINER, "field: {0}", field);
                            LOGGER.log(Level.FINER, "********************************************");
                            throw new CollectionNotSupported(oCol.getClass().getSimpleName());
                        }
                        f.setAccessible(acc);
                    }
                } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            ovToRemove.remove();
            // si tengo un RID, proceder a removerlo de las colecciones.
            this.dirty.remove(((IObjectProxy) toRemove).___getVertex().getId().toString());

        } else {
            throw new UnknownObject();
        }

    }

    /**
     * realiza un query a la base de datos
     *
     * @param <T>
     * @param sql
     * @return
     */
    public <T> T query(String sql) {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.flush();

        OCommandSQL osql = new OCommandSQL(sql);
        return graphdb.command(osql).execute();
    }

    /**
     * Devuelve todos los registros a partir de una clase base.
     *
     * @param <T>
     * @param sql
     * @return
     */
    public <T> List<T> query(Class<T> clase) {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.flush();

        ArrayList<T> ret = new ArrayList<>();

        for (Vertex verticesOfClas : graphdb.getVerticesOfClass(clase.getSimpleName())) {
            ret.add(this.get(clase, verticesOfClas.getId().toString()));
            LOGGER.log(Level.FINER, "vertex: " + verticesOfClas.getId().toString() + "  class: " + ((OrientVertex) verticesOfClas).getType().getName());
        }

        return ret;
    }

    /**
     * Devuelve todos los registros a partir de una clase base, filtrando los datos por lo que se agregue en el body.
     *
     * @param <T>
     * @param sql
     * @return
     */
    public <T> List<T> query(Class<T> clase, String body) {
        if (graphdb == null) {
            throw new NoOpenTx();
        }
        this.flush();

        ArrayList<T> ret = new ArrayList<>();

        String cSQL = "SELECT FROM " + clase.getSimpleName() + " " + body;
        LOGGER.log(Level.FINER, cSQL);
        for (Vertex v : (Iterable<Vertex>) graphdb.command(
                new OCommandSQL(cSQL)).execute()) {
            ret.add(this.get(clase, v.getId().toString()));
        }

        return ret;
    }

    /**
     * Ejecuta un prepared query y devuelve una lista de la clase indicada.
     *
     * @param <T>
     * @param clase
     * @param sql
     * @param param
     * @return
     */
    public <T> List<T> query(Class<T> clase, String sql, Object... param) {
        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(sql);
        ArrayList<T> ret = new ArrayList<>();

        LOGGER.log(Level.FINER, sql);
        for (Vertex v : (Iterable<Vertex>) graphdb.command(query).execute(param)) {
            ret.add(this.get(clase, v.getId().toString()));
        }
        return ret;
    }

    /**
     * Devuelve el objecto de definición de la clase en la base.
     *
     * @param clase
     * @return OClass o null si la clase no existe
     */
    public OClass getDBClass(String clase) {
        return graphdb.getRawGraph().getMetadata().getSchema().getClass(clase);
    }
}
