package net.odbogm.proxy;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.ObjectMapper;
import net.odbogm.ObjectStruct;
import net.odbogm.SessionManager;
import net.odbogm.Transaction;
import net.odbogm.agent.ITransparentDirtyDetector;
import net.odbogm.annotations.Audit.AuditType;
import net.odbogm.annotations.Indirect;
import net.odbogm.annotations.RemoveOrphan;
import net.odbogm.cache.ClassDef;
import net.odbogm.exceptions.CollectionNotSupported;
import net.odbogm.exceptions.DuplicateLink;
import net.odbogm.exceptions.InvalidObjectReference;
import net.odbogm.exceptions.ObjectMarkedAsDeleted;
import net.odbogm.utils.ReflectionUtils;
import net.odbogm.utils.ThreadHelper;
import net.odbogm.utils.VertexUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectProxy implements IObjectProxy, MethodInterceptor {

    private final static Logger LOGGER = Logger.getLogger(ObjectProxy.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ObjectProxy);
        }
    }

    // the real object      
    private Object ___proxiedObject;

    private Class<?> ___baseClass;

    // Vértice desde el que se obtiene el objeto.
    // private OrientVertex baseVertex;
    private OrientElement ___baseElement;

    // permite marcar el objeto como inválida en caso que se haga un rollback 
    // sobre un objeto que nunca se persistió.
    private boolean ___isValidObject = true;

    private Transaction ___transaction;

    private boolean ___dirty = false;

    // determina si ya se han cargado los links o no
    private boolean ___loadLazyLinks = true;

    // determina si el objeto ya ha sido completamente inicializado.
    // sirve para impedir que se invoquen a los métodos durante el setup inicial del construtor.
    private boolean ___objectReady = false;

    // si esta marca está activa indica que el objeto ha sido eliminado de la base de datos 
    // y toda comunicación con el mismo debe ser abortada
    private boolean ___deletedMark = false;


    // constructor - the supplied parameter is an
    // object whose proxy we would like to create     
    public ObjectProxy(Object obj, OrientElement e, Transaction t) {
        this.___baseClass = obj.getClass();
        this.___baseElement = e;
        this.___transaction = t;
    }


    public ObjectProxy(Class c, OrientElement e, Transaction t) {
        this.___baseClass = c;
        this.___baseElement = e;
        this.___transaction = t;
    }


    // GCLib interceptor 
    @Override
    public Object intercept(Object o,
            Method method,
            Object[] args,
            MethodProxy methodProxy) throws Throwable {
        // response object
        Object res = null;

        // el estado del objeto se debe poder consultar siempre
        if (method.getName().equals("___isValid")) {
            return this.___isValid();
        }
        if (method.getName().equals("___isDeleted")) {
            return this.___isDeleted();
        }

        if (method.getName().equals("___getVertex")) {
            if (this.___objectReady) {
                return this.___getVertex();
            }
        }

        if (method.getName().equals("___getBaseClass")) {
            if (this.___objectReady) {
                return this.___getBaseClass();
            }
        }

        if (!this.___isValidObject) {
            LOGGER.log(Level.FINER, "El objeto está marcado como inválido!!!");
            throw new InvalidObjectReference();
        }

        if (method.getName().equals("___rollback")) {
            if (this.___objectReady) {
                this.___rollback();
                return true;
            }
        }

        if (this.___baseElement.getIdentity().isNew()) {
            LOGGER.log(Level.FINER, "RID nuevo. No procesar porque el store preparó todo y no hay nada que recuperar de la base.");
            this.___loadLazyLinks = false;
        }
        // BEFORE
        // measure the current time         
//        long time1 = System.currentTimeMillis();
//        System.out.println("intercepted: " + method.getName());
        // modificar el llamado
        if (!this.___deletedMark) {
            switch (method.getName()) {
//                case "___getVertex":
//                    if (this.___objectReady) {
//                        res = this.___getVertex();
//                    }
//                    break;
                case "___getRid":
                    if (this.___objectReady) {
                        res = this.___getRid();
                    }
                    break;
                case "___getProxiedObject":
                    if (this.___objectReady) {
                        res = this.___getProxiedObject();
                    }
                    break;
//                case "___getBaseClass":
//                    if (this.___objectReady) {
//                        res = this.___getBaseClass();
//                    }
//                    break;
//                case "___isValid":
//                        se resuelve arriba.
//                    if (this.___isValidObject) {
//                        res = this.___isValid();
//                    }
//                    break;
                case "___loadLazyLinks":
                    if (this.___objectReady) {
                        this.___loadLazyLinks();
                    }
                    break;
                case "___updateIndirectLinks":
                    if (this.___objectReady) {
                        this.___updateIndirectLinks();
                    }
                    break;
                case "___isDirty":
                    if (this.___objectReady) {
                        res = this.___isDirty();
                    }
                    break;
                case "___setDirty":
                    if (this.___objectReady) {
                        this.___setDirty();
                    }
                    break;
                case "___removeDirtyMark":
                    if (this.___objectReady) {
                        this.___removeDirtyMark();
                    }
                    break;
                case "___commit":
                    /**
                     * FIXME: se podría evitar si se controlara si los links se
                     * han cargado o no al momento de hacer el commit para
                     * evitar realizar el
                     * load sin necesidad.
                     */
                    if (this.___objectReady) {
                        if (this.___loadLazyLinks) {
                            this.___loadLazyLinks();
                        }
                        this.___commit();
                    }
                    break;
                case "___reload":
                    if (this.___objectReady) {
                        this.___reload();
                    }
                    break;
//                case "___rollback":
//                    if (this.___objectReady) {
//                        this.___rollback();
//                    }
//                    break;
                case "___setDeletedMark":
                    this.___setDeletedMark();
                    break;

                case "___ogm___setDirty":
                    res = methodProxy.invokeSuper(o, args);
                    break;
                case "___ogm___isDirty":
//                    LOGGER.log(Level.INFO, "Method: sup.name: "+methodProxy.getSuperName()+
//                                                       " - sig: "+methodProxy.getSignature()+
//                                                       " - sup idx: "+methodProxy.getSuperIndex()
//                            );
                    res = methodProxy.invokeSuper(o, args);

                    break;
                default:

                    // invoke the method on the real object with the given params
//                res = methodProxy.invoke(realObj, args);
                    if (this.___objectReady) {
                        if (this.___loadLazyLinks) {
                            this.___loadLazyLinks();
                        }
                    }

                    if (method.getName().equals("toString")) {
                        try {
                            ReflectionUtils.findMethod(this.___baseClass, "toString", (Class<?>[]) null);
                            res = methodProxy.invokeSuper(o, args);
                        } catch (NoSuchMethodException nsme) {
                            res = this.___baseElement.getId().toString();
                        }
                    } else {
                        LOGGER.log(Level.FINEST, "invocando: " + method.getName());
                        res = methodProxy.invokeSuper(o, args);
                    }

                    // verificar si hay diferencias entre los objetos dependiendo de la estrategia seleccionada.
                    if (this.___objectReady) {
                        switch (this.___transaction.getSessionManager().getActivationStrategy()) {
                            case CLASS_INSTRUMENTATION:
                                // si se está usando la instrumentación de clase, directamente verificar en el objeto
                                // cual es su estado.
                                LOGGER.log(Level.FINEST, "o: " + o.getClass().getName() + " ITrans: " + (o instanceof ITransparentDirtyDetector));
                                if (((ITransparentDirtyDetector) o).___ogm___isDirty()) {
                                    LOGGER.log(Level.FINEST, "objeto {0} marcado como dirty por ASM. Agregarlo a la lista de pendientes.", o.getClass().getName());
                                    this.___setDirty();
                                }
                        }
                    }

                    break;
            }
        } else {
            throw new ObjectMarkedAsDeleted("The object " + this.___baseElement.getId().toString() + " was deleted from the database. Trying to call to " + method.getName());
        }
        // AFTER
        // print how long it took to execute the method on the proxified object
//        System.out.println("Took: " + (System.currentTimeMillis() - time1) + " ms");
        // return the result         
        return res;
    }


    /**
     * Establece el objeto base sobre el que trabaja el proxy.
     *
     * @param po objeto de referencia
     */
    public void ___setProxiedObject(Object po) {
        this.___proxiedObject = po;
        this.___objectReady = true;
    }


    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return referencia al OrientVertex
     */
    @Override
    public OrientVertex ___getVertex() {
        if (this.___baseElement.getElementType().equals("Vertex")) {
            return (OrientVertex) this.___baseElement;
        } else {
            return null;
        }
    }


    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return el RID del object en la base
     */
    @Override
    public String ___getRid() {
        if (this.___baseElement != null) {
            return this.___baseElement.getId().toString();
        } else {
            return null;
        }
    }


    /**
     *
     * establece el elemento base como un vértice.
     *
     * @param v vétice de referencia
     */
    @Override
    public void ___setVertex(OrientVertex v) {
        this.___baseElement = v;
    }


    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return la referencia al OrientVertex
     */
    @Override
    public OrientVertex ___getEdge() {
        if (this.___baseElement.getElementType().equals("Edge")) {
            return (OrientVertex) this.___baseElement;
        } else {
            return null;
        }
    }


    /**
     *
     * establece el elemento base como un vértice.
     *
     * @param e Edge de referencia
     */
    @Override
    public void ___setEdge(OrientEdge e) {
        this.___baseElement = e;
    }


    @Override
    public Object ___getProxiedObject() {
        return this.___proxiedObject;
    }


    @Override
    public Class<?> ___getBaseClass() {
        return this.___baseClass;
    }


    @Override
    public void ___setDeletedMark() {
        this.___deletedMark = true;
    }


    @Override
    public boolean ___isDeleted() {
        return this.___deletedMark;
    }


    /**
     * Carga todos los links del objeto
     */
    @Override
    public synchronized void ___loadLazyLinks() {

        if (this.___loadLazyLinks) {
            this.___transaction.initInternalTx();

            LOGGER.log(Level.FINER, "Base class: " + this.___baseClass.getSimpleName());
            LOGGER.log(Level.FINER, "iniciando loadLazyLinks...");
            boolean currentDirtyState = this.___isDirty();
            // marcar que ya se han incorporado todo los links
            this.___loadLazyLinks = false;

            if (this.___baseElement instanceof OrientVertex) {
                OrientVertex ov = (OrientVertex) this.___baseElement;
                ClassDef classdef = this.___transaction.getObjectMapper().getClassDef(this.___proxiedObject);

                // hidratar los atributos @links
                // procesar todos los links y los indirectLinks
//                Map<String, Class<?>> lnks = new HashMap<>();
//                lnks.putAll(classdef.links);
                LOGGER.log(Level.FINER, "procesando {0} links ", new Object[]{classdef.links.size()});
                for (Map.Entry<String, Class<?>> entry : classdef.links.entrySet()) {
                    //  classdef.links.entrySet().stream().forEach((entry) -> {
                    try {
                        String field = entry.getKey();
                        Class<?> fc = entry.getValue();

                        Field fLink = ReflectionUtils.findField(___baseClass, field);
                        boolean acc = fLink.isAccessible();
                        fLink.setAccessible(true);

                        String graphRelationName = ___baseClass.getSimpleName() + "_" + field;
                        Direction direction = Direction.OUT;
//                        if (fLink.isAnnotationPresent(Indirect.class)) {
//                            // si es un indirect se debe reemplazar el nombre de la relación por 
//                            // el propuesto por la anotation
//                            Indirect in = fLink.getAnnotation(Indirect.class);
//                            graphRelationName = in.linkName();
//                            direction = Direction.IN;
//                            LOGGER.log(Level.FINER, "Se ha detectado un indirect. Linkname = {0}", new Object[]{in.linkName()});
//                        }
                        LOGGER.log(Level.FINER, "Field: {0}.{1}   Class: {2}  RelationName: {3}", new String[]{this.___baseClass.getSimpleName(), field, fc.getSimpleName(), graphRelationName});

                        // recuperar de la base el vértice correspondiente
                        boolean duplicatedLinkGuard = false;
                        for (Vertex vertice : ov.getVertices(direction, graphRelationName)) {
                            LOGGER.log(Level.FINER, "hydrate innerO: " + vertice.getId());

                            if (!duplicatedLinkGuard) {
//                        Object innerO = this.hydrate(fc, vertice);
                                /*
                                 * FIXME: esto genera una dependencia cruzada.
                                 * Habría que revisar
                                 * como solucionarlo. Esta llamada se hace para
                                 * que quede el objeto
                                 * mapeado
                                 */
                                this.___transaction.addToTransactionCache(this.___getRid(), ___proxiedObject);

                                // si es una interface llamar a get solo con el RID.
                                Object innerO = null;

                                innerO = fc.isInterface() ? this.___transaction.get(vertice.getId().toString()) : this.___transaction.get(fc, vertice.getId().toString());

                                LOGGER.log(Level.FINER, "Inner object " + field + ": "
                                        + (innerO == null ? "NULL" : "" + innerO.toString())
                                        + "  FC: " + fc.getSimpleName()
                                        + "   innerO.class: " + innerO.getClass().getSimpleName()
                                        + " hashCode: " + System.identityHashCode(innerO));
                                fLink.set(this.___proxiedObject, fc.cast(innerO));
                                duplicatedLinkGuard = true;

                                ___transaction.decreseTransactionCache();
                            } else if (false) {
                                throw new DuplicateLink();
                            }
                            LOGGER.log(Level.FINER, "FIN hydrate innerO: " + vertice.getId() + "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                        }
                        fLink.setAccessible(acc);
                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                // actualizar los indirectLinks
                this.___updateIndirectLinks();
            }

            // resetear dirty si corresponde.
            this.___dirty = currentDirtyState;

            this.___transaction.closeInternalTx();
        }

    }


    @Override
    public void ___updateIndirectLinks() {
        if (this.___baseElement instanceof OrientVertex) {
            boolean preservDirtyState = this.___dirty;

            OrientVertex ov = (OrientVertex) this.___baseElement;
            ClassDef classdef = this.___transaction.getObjectMapper().getClassDef(this.___proxiedObject);

            // hidratar los atributos @indirectLinks
            Map<String, Class<?>> lnks = new HashMap<>();
            lnks.putAll(classdef.indirectLinks);
            LOGGER.log(Level.FINER, "procesando {0} indirected links", new Object[]{classdef.indirectLinks.size()});
            for (Map.Entry<String, Class<?>> entry : lnks.entrySet()) {
                //  classdef.links.entrySet().stream().forEach((entry) -> {
                try {
                    String field = entry.getKey();
                    Class<?> fc = entry.getValue();

                    Field fLink = ReflectionUtils.findField(___baseClass, field);
                    boolean acc = fLink.isAccessible();
                    fLink.setAccessible(true);

                    String graphRelationName = ___baseClass.getSimpleName() + "_" + field;
                    Direction direction = Direction.OUT;
                    if (fLink.isAnnotationPresent(Indirect.class)) {
                        // si es un indirect se debe reemplazar el nombre de la relación por 
                        // el propuesto por la anotation
                        Indirect in = fLink.getAnnotation(Indirect.class);
                        graphRelationName = in.linkName();
                        direction = Direction.IN;
                        LOGGER.log(Level.FINER, "Se ha detectado un indirect. Linkname = {0}", new Object[]{in.linkName()});
                    }
                    LOGGER.log(Level.FINER, "Field: {0}.{1}   Class: {2}  RelationName: {3}", new String[]{this.___baseClass.getSimpleName(), field, fc.getSimpleName(), graphRelationName});

                    // recuperar de la base el vértice correspondiente
                    boolean duplicatedLinkGuard = false;
                    for (Vertex vertice : ov.getVertices(direction, graphRelationName)) {
                        LOGGER.log(Level.FINER, "hydrate innerO: " + vertice.getId());

                        if (!duplicatedLinkGuard) {
//                        Object innerO = this.hydrate(fc, vertice);
                            /*
                             * FIXME: esto genera una dependencia cruzada.
                             * Habría que revisar
                             * como solucionarlo. Esta llamada se hace para que
                             * quede el objeto
                             * mapeado
                             */
                            this.___transaction.addToTransactionCache(this.___getRid(), ___proxiedObject);

                            // si es una interface llamar a get solo con el RID.
                            Object innerO = null;

                            innerO = fc.isInterface() ? this.___transaction.get(vertice.getId().toString()) : this.___transaction.get(fc, vertice.getId().toString());

                            LOGGER.log(Level.FINER, "Inner object " + field + ": "
                                    + (innerO == null ? "NULL" : "" + innerO.toString())
                                    + "  FC: " + fc.getSimpleName()
                                    + "   innerO.class: " + innerO.getClass().getSimpleName()
                                    + " hashCode: " + System.identityHashCode(innerO));
                            fLink.set(this.___proxiedObject, fc.cast(innerO));
                            duplicatedLinkGuard = true;

                            ___transaction.decreseTransactionCache();
                        } else if (false) {
                            throw new DuplicateLink();
                        }
                        LOGGER.log(Level.FINER, "FIN hydrate innerO: " + vertice.getId() + "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                    }
                    fLink.setAccessible(acc);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // forzar la recarga de las colecciones.
            LOGGER.log(Level.FINER, "Refrescando las colecciones indirectas...");
            // ********************************************************************************************
            // hidratar las colecciones indirectas
            // procesar todos los indirectLinkslist
            // ********************************************************************************************
            for (Map.Entry<String, Class<?>> entry : classdef.indirectLinkLists.entrySet()) {

                try {
                    // FIXME: se debería considerar agregar una annotation EAGER!
                    String field = entry.getKey();
                    Class<?> fc = entry.getValue();
                    LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
                    Field fLink = ReflectionUtils.findField(this.___baseClass, field);

                    boolean acc = fLink.isAccessible();
                    fLink.setAccessible(true);

                    String graphRelationName = null;
                    Direction RelationDirection = Direction.IN;

                    Indirect in = fLink.getAnnotation(Indirect.class);
                    graphRelationName = in.linkName();

                    // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
                    if ((ov.countEdges(RelationDirection, graphRelationName) > 0) || (fLink.get(___proxiedObject) != null)) {
                        this.___transaction.getObjectMapper().colecctionToLazy(___proxiedObject, field, fc, ov, ___transaction);
                    }

                    fLink.setAccessible(acc);

                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            // volver a establecer el estado de Dirty.
            this.___dirty = preservDirtyState;
        }

    }


    @Override
    public boolean ___isValid() {
        return ___isValidObject;
    }


    @Override
    public boolean ___isDirty() {
        return ___dirty;
    }


    /**
     * Marca el objeto como dirty para que sea considerado en el próximo commit
     *
     */
    @Override
    public void ___setDirty() {
        if (!this.___dirty) {
            this.___dirty = true;
            // agregarlo a la lista de dirty para procesarlo luego
            LOGGER.log(Level.FINER, "Dirty: " + this.___proxiedObject);
            this.___transaction.setAsDirty(this.___proxiedObject);
            LOGGER.log(Level.FINER, "Objeto marcado como dirty! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            LOGGER.log(Level.FINEST, ThreadHelper.getCurrentStackTrace());
        }
    }


    @Override
    public void ___removeDirtyMark() {
        this.___dirty = false;
        // verificar la estrategia de activación.
        // si la estrategia es ONCOMMIT se debe validar primero que existan cambios en los objetos
        // antes de proceder.
        if (this.___transaction.getSessionManager().getActivationStrategy() == SessionManager.ActivationStrategy.CLASS_INSTRUMENTATION) {
            LOGGER.log(Level.FINER, "CLASS_INSTRUMENTATION Strategy.");
            ((ITransparentDirtyDetector) this.___proxiedObject).___ogm___setDirty(false);
        }
    }


    @Override
    public synchronized void ___commit() {

//        ODatabaseRecordThreadLocal.INSTANCE.set(this.___sm.getGraphdb().getRawGraph());
        LOGGER.log(Level.FINER, "Iniciando ___commit() ....");
        LOGGER.log(Level.FINER, "valid: " + this.___isValidObject);
        LOGGER.log(Level.FINER, "dirty: " + this.___dirty);

        if (this.___dirty || this.___baseElement.getIdentity().isNew()) {
            this.___transaction.initInternalTx();

            // asegurarse que está activa la base.
            this.___transaction.activateOnCurrentThread();

            // asegurarse que está atachado
            if (this.___baseElement.getGraph() == null) {
                LOGGER.log(Level.FINER, "El objeto no está atachado!");
                this.___transaction.getGraphdb().attach(this.___baseElement);
            }

            // obtener la definición de la clase
            ClassDef cDef = this.___transaction.getObjectMapper().getClassDef(this.___proxiedObject);

            // obtener un mapa actualizado del objeto contenido
            ObjectStruct oStruct = this.___transaction.getObjectMapper().objectStruct(this.___proxiedObject);
            Map<String, Object> omap = oStruct.fields;

            // bajar todo al vértice
            this.___baseElement.setProperties(omap);

            // guardar log de auditoría si corresponde.
            if (this.___transaction.getSessionManager().isAuditing()) {
                this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "UPDATE", omap);
            }

            // si se trata de un Vértice
            if (this.___baseElement.getElementType().equals("Vertex")) {
                OrientVertex ov = (OrientVertex) this.___baseElement;
                // Analizar si cambiaron los vértices
                /*
                 * procesar los objetos internos. Primero se deber determinar
                 * si los objetos ya existían en el contexto actual. Si no
                 * existen
                 * deben ser creados.
                 */
                for (Map.Entry<String, Class<?>> link : cDef.links.entrySet()) {
                    String field = link.getKey();
                    String graphRelationName = this.___baseClass.getSimpleName() + "_" + field;
                    Class<?> fclass = link.getValue();
                    Field f;
                    // determinar el estado del campo
                    if (oStruct.links.get(field) == null) {
                        // si está en null, es posible que se haya eliminado el objeto
                        // por lo cual se debería eliminar el vértice correspondiente
                        // si es que existe
                        if (ov.countEdges(Direction.OUT, graphRelationName) > 0) {
                            // se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente
                            OrientEdge removeEdge = null;
                            for (Edge edge : ov.getEdges(Direction.OUT, graphRelationName)) {
                                removeEdge = (OrientEdge) edge;

                                if (this.___transaction.getSessionManager().isAuditing()) {
                                    this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "REMOVE LINK: " + graphRelationName, removeEdge);
                                }
                                this.removeEdge(removeEdge, field);
                            }

                        }
                    } else {
                        Object innerO = oStruct.links.get(field);
                        // verificar si ya está en el contexto. Si fue creado en forma 
                        // separada y asociado con el objeto principal, se puede dar el caso
                        // de que el objeto principal tiene RID y el agregado no.
                        if (innerO instanceof IObjectProxy) {
                            // el objeto existía.
                            // se debe verificar si el eje entre los dos objetos ya existía.
                            if (!VertexUtils.isConectedTo(ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName)) {
                                // No existe un eje. Se debe crear
                                LOGGER.log(Level.FINER, "Los objetos no están conectados. (" + ov.getId() + " |--|" + ((IObjectProxy) innerO).___getVertex().getId());

                                // primero verificar si no existía una relación previa con otro objeto para removerla.
                                if (ov.countEdges(Direction.OUT, graphRelationName) > 0) {
                                    LOGGER.log(Level.FINER, "Existía una relación previa. Se debe eliminar.");
                                    // existé una relación. Elimnarla antes de proceder a establecer la nueva.
                                    OrientEdge removeEdge = null;
                                    for (Edge edge : ov.getEdges(Direction.OUT, graphRelationName)) {
                                        removeEdge = (OrientEdge) edge;
                                        LOGGER.log(Level.FINER, "Eliminar relación previa a " + removeEdge.getInVertex());

                                        if (this.___transaction.getSessionManager().isAuditing()) {
                                            this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "REMOVE LINK: " + graphRelationName, removeEdge);
                                        }

                                        this.removeEdge(removeEdge, field);

                                    }

                                }
                                LOGGER.log(Level.FINER, "Agregar un link entre dos objetos existentes.");
                                OrientEdge oe = this.___transaction.getSessionManager().getGraphdb().addEdge("class:" + graphRelationName, ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                                if (this.___transaction.getSessionManager().isAuditing()) {
                                    this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "ADD LINK: " + graphRelationName, oe);
                                }
                            }
                        } else {
                            // el objeto es nuevo
                            // primero verificar si no existía una relación previa con otro objeto para removerla.
                            if (ov.countEdges(Direction.OUT, graphRelationName) > 0) {
                                LOGGER.log(Level.FINER, "Existía una relación previa. Se debe eliminar.");
                                // existé una relación. Elimnarla antes de proceder a establecer la nueva.
                                OrientEdge removeEdge = null;
                                for (Edge edge : ov.getEdges(Direction.OUT, graphRelationName)) {
                                    removeEdge = (OrientEdge) edge;
                                    LOGGER.log(Level.FINER, "Eliminar relación previa a " + removeEdge.getOutVertex());
                                    if (this.___transaction.getSessionManager().isAuditing()) {
                                        this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "REMOVE LINK: " + graphRelationName, removeEdge);
                                    }
                                    this.removeEdge(removeEdge, field);
                                }

                            }

                            // crear la nueva relación
                            LOGGER.log(Level.FINER, "innerO nuevo. Crear un vértice y un link");
                            innerO = this.___transaction.store(innerO);
//                            this.sm.getObjectMapper().setFieldValue(realObj, field, innerO);
                            this.___transaction.getObjectMapper().setFieldValue(this.___proxiedObject, field, innerO);

                            // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                            if (innerO instanceof ITransparentDirtyDetector) {
                                ((ITransparentDirtyDetector) innerO).___ogm___setDirty(false);
                            }

                            OrientEdge oe = this.___transaction.getGraphdb().addEdge("class:" + graphRelationName, ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                            if (this.___transaction.getSessionManager().isAuditing()) {
                                this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "ADD LINK: " + graphRelationName, oe);
                            }
                        }
                    }
                }

                /**
                 * Procesar los linklists.
                 */
                Field f;
                for (Map.Entry<String, Class<?>> entry : cDef.linkLists.entrySet()) {
                    try {
                        String field = entry.getKey();
                        Class<? extends Object> fieldClass = entry.getValue();

                        // f = ReflectionUtils.findField(this.realObj.getClass(), field);
                        LOGGER.log(Level.FINER, "procesando campo: " + field + " clase: " + this.___proxiedObject.getClass());

                        f = ReflectionUtils.findField(this.___proxiedObject.getClass(), field);
                        boolean acc = f.isAccessible();
                        f.setAccessible(true);
                        final String graphRelationName;

                        // preprarar el nombre de la relación
                        graphRelationName = this.___baseClass.getSimpleName() + "_" + field;

                        // Object oCol = f.get(this.realObj);
                        Object oCol = f.get(this.___proxiedObject);

                        // verificar si existe algún cambio en la colecciones
                        // ingresa si la colección es distinta de null y
                        // oCol es instancia de ILazyCalls y está marcado como dirty
                        // o oCol no es instancia de ILazyCalls, lo que significa que es una colección nueva
                        // y debe ser procesada completamente.
                        if ((oCol != null)
                                && ((ILazyCalls.class.isAssignableFrom(oCol.getClass()) && ((ILazyCalls) oCol).isDirty())
                                || (!ILazyCalls.class.isAssignableFrom(oCol.getClass())))) {
                            LOGGER.log(Level.FINER, (!ILazyCalls.class.isAssignableFrom(oCol.getClass()))
                                    ? "No es instancia de ILazyCalls"
                                    : "Es instancia de Lazy y está marcado como DIRTY");

                            if (oCol instanceof List) {
                                ILazyCollectionCalls col;
                                // procesar la colección

                                if (ILazyCollectionCalls.class
                                        .isAssignableFrom(oCol.getClass())) {
                                    col = (ILazyCollectionCalls) oCol;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    // this.sm.getObjectMapper().colecctionToLazy(this.realObj, field, ov);
                                    this.___transaction.getObjectMapper().colecctionToLazy(this.___proxiedObject, field, ov, this.___transaction);

                                    //recuperar la nueva colección
                                    // Collection inter = (Collection) f.get(this.realObj);
                                    Collection inter = (Collection) f.get(this.___proxiedObject);

                                    //agregar todos los valores que existían
                                    inter.addAll((Collection) oCol);
                                    //preparar la interface para que se continúe con el acceso.
                                    col = (ILazyCollectionCalls) inter;
                                    // reasignar el objeto oCol
                                    oCol = f.get(this.___proxiedObject);
                                }

                                List lCol = (List) oCol;
                                Map<Object, ObjectCollectionState> colState = col.collectionState();

                                // procesar los elementos presentes en la colección
                                for (int i = 0; i < lCol.size(); i++) {
                                    Object colObject = lCol.get(i);
                                    // verificar el estado del objeto en la colección
                                    if (colState.get(colObject) == ObjectCollectionState.ADDED) {
                                        // si se agregó uno, determinar si era o no manejado por el SM
                                        if (!(colObject instanceof IObjectProxy)) {
                                            LOGGER.log(Level.FINER, "Objeto nuevo. Insertando en la base y reemplazando el original...");
                                            // no es un objeto que se haya almacenado.
                                            colObject = this.___transaction.store(colObject);
                                            // reemplazar en la colección el objeto por uno administrado
                                            lCol.set(i, colObject);

                                            // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                                            if (colObject instanceof ITransparentDirtyDetector) {
                                                ((ITransparentDirtyDetector) colObject).___ogm___setDirty(false);
                                            }

                                        }

                                        // vincular el nodo
                                        OrientEdge oe = this.___transaction.getGraphdb().addEdge("class:" + graphRelationName, this.___getVertex(), ((IObjectProxy) colObject).___getVertex(), graphRelationName);

                                        if (this.___transaction.getSessionManager().isAuditing()) {
                                            this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "LINKLIST ADD: " + graphRelationName, oe);
                                        }
                                    }
                                }
                                // procesar los removidos solo si está el anotation en el campo

                                for (Map.Entry<Object, ObjectCollectionState> entry1 : colState.entrySet()) {
                                    Object colObject = entry1.getKey();
                                    ObjectCollectionState colObjState = entry1.getValue();

                                    if (colObjState == ObjectCollectionState.REMOVED) {
                                        // remover el link
                                        for (Edge edge : ((OrientVertex) this.___baseElement)
                                                .getEdges(((IObjectProxy) colObject).___getVertex(),
                                                        Direction.OUT,
                                                        graphRelationName)) {
                                            if (this.___transaction.getSessionManager().isAuditing()) {
                                                this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "LINKLIST REMOVE: " + graphRelationName, edge);
                                            }
                                            edge.remove();
                                        }
                                        // si existe la anotación, remover tambien el vertex
                                        if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                            if (this.___transaction.getSessionManager().isAuditing()) {
                                                this.___transaction.getSessionManager().auditLog(this, AuditType.DELETE, "LINKLIST DELETE: " + graphRelationName, colObject);
                                            }
                                            this.___transaction.delete(colObject);
                                        }
                                    }
                                }

                                // resetear el estado
                                col.clearState();

                            } else if (oCol instanceof Map) {

                                Map innerMap;
                                // procesar la colección

                                if (ILazyMapCalls.class.isAssignableFrom(oCol.getClass())) {
                                    innerMap = (Map) oCol;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    // this.sm.getObjectMapper().colecctionToLazy(this.realObj, field, ov);
                                    this.___transaction.getObjectMapper().colecctionToLazy(this.___proxiedObject, field, ov, this.___transaction);
                                    //recuperar la nueva colección
                                    // Collection inter = (Collection) f.get(this.realObj);
                                    Map inter = (Map) f.get(this.___proxiedObject);
                                    //agregar todos los valores que existían
                                    inter.putAll((Map) oCol);
                                    //preparar la interface para que se continúe con el acceso.
                                    innerMap = (Map) inter;
                                }

                                // final String ffield = field;
                                // refrescar los estados
                                final Map<Object, ObjectCollectionState> keyState = ((ILazyMapCalls) innerMap).collectionState();
                                final Map<Object, OrientEdge> keyToEdge = ((ILazyMapCalls) innerMap).getKeyToEdge();
                                final Map<Object, ObjectCollectionState> entitiesState = ((ILazyMapCalls) innerMap).getEntitiesState();

                                // recorrer todas las claves del mapa
                                for (Map.Entry<Object, ObjectCollectionState> entry1 : keyState.entrySet()) {
                                    Object imk = entry1.getKey();
                                    ObjectCollectionState imV = entry1.getValue();

                                    LOGGER.log(Level.FINER, "imk: " + imk + " state: " + imV);
                                    // para cada entrada, verificar la existencia del objeto y crear un Edge.
                                    OrientEdge oe = null;
                                    Object linkedO = innerMap.get(imk);

                                    if (!(linkedO instanceof IObjectProxy)) {
                                        LOGGER.log(Level.FINER, "Link Map Object nuevo. Crear un vértice y un link");
                                        linkedO = this.___transaction.store(linkedO);
                                        innerMap.replace(imk, linkedO);

                                        // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                                        if (linkedO instanceof ITransparentDirtyDetector) {
                                            ((ITransparentDirtyDetector) linkedO).___ogm___setDirty(false);
                                        }
                                    }

                                    // verificar el estado del objeto en la colección.
                                    switch (imV) {
                                        case ADDED:
                                            // crear un link entre los dos objetos.
                                            LOGGER.log(Level.FINER, "-----> agregando un LinkList al Map!");
                                            //                                        oe = SessionManager.this.graphdb.addEdge("", fVertexs.get(frid), fVertexs.get(llRID), ffield);
                                            oe = this.___transaction.getGraphdb().addEdge("class:" + graphRelationName, (OrientVertex) this.___baseElement, ((IObjectProxy) linkedO).___getVertex(), graphRelationName);
                                            // actualizar el edge con los datos de la key.
                                            oe.setProperties(this.___transaction.getObjectMapper().simpleMap(imk));

                                            if (this.___transaction.getSessionManager().isAuditing()) {
                                                this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "LINKLIST ADD: " + graphRelationName, oe);
                                            }
                                            break;

                                        case NOCHANGE:
                                            // el link no se ha modificado. 
                                            break;

                                        case REMOVED:
                                            // quitar el Edge
                                            OrientEdge oeRemove = keyToEdge.get(imk);
                                            if (this.___transaction.getSessionManager().isAuditing()) {
                                                this.___transaction.getSessionManager().auditLog(this, AuditType.WRITE, "LINKLIST REMOVE: " + graphRelationName, oeRemove);
                                            }
                                            oeRemove.remove();
                                            // el link se ha removido. Se debe eliminar y verificar si corresponde borrar 
                                            // el vértice en caso de estar marcado con @RemoveOrphan.
                                            if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                                if (entitiesState.get(imk) == ObjectCollectionState.REMOVED) {
                                                    this.___transaction.delete(entitiesState.get(imk));
                                                    if (this.___transaction.getSessionManager().isAuditing()) {
                                                        this.___transaction.getSessionManager().auditLog(this, AuditType.DELETE, "LINKLIST REMOVE: " + graphRelationName, imk);
                                                    }
                                                }
                                            }
                                            break;
                                    }
                                }
                                ((ILazyMapCalls) innerMap).clearState();
                            } else {
                                LOGGER.log(Level.FINER, "********************************************");
                                LOGGER.log(Level.FINER, "field: {0}", field);
                                LOGGER.log(Level.FINER, "********************************************");
                                throw new CollectionNotSupported(oCol.getClass().getSimpleName());
                            }
                            f.setAccessible(acc);

                        }
                    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(SessionManager.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            // quitar la marca de dirty
            this.___removeDirtyMark();
//            this.___dirty = false;
            this.___transaction.closeInternalTx();
        }
        LOGGER.log(Level.FINER, "fin commit ----");
    }


    /**
     * Refresca el objeto base recuperándolo nuevamente desde la base de datos.
     */
    @Override
    public void ___reload() {
        this.___transaction.initInternalTx();

        this.___transaction.activateOnCurrentThread();
        this.___baseElement.reload();

        this.___transaction.closeInternalTx();
    }


    /**
     * Función de uso interno para remover un eje
     *
     * @param edgeToRemove
     * @param field
     */
    private synchronized void removeEdge(OrientEdge edgeToRemove, String field) {

        try {
            // f = ReflectionUtils.findField(this.realObj.getClass(), field);
            Field f = ReflectionUtils.findField(this.___baseClass, field);
            boolean acc = f.isAccessible();
            f.setAccessible(true);

            // En el Edge, IN proviene del objeto apuntado. Raro pero es así :(
            String outRid = edgeToRemove.getInVertex().getIdentity().toString();
            LOGGER.log(Level.FINER, "El edge " + edgeToRemove
                    + " apunta IN: " + edgeToRemove.getInVertex().getIdentity().toString()
                    + " apunta OUT: " + edgeToRemove.getOutVertex().getIdentity().toString());
            // remover primero el eje
            edgeToRemove.remove();

            // si corresponde
            if (f.isAnnotationPresent(RemoveOrphan.class)) {
                LOGGER.log(Level.FINER, "Remove orphan presente");
                //auditar
                if (this.___transaction.getSessionManager().isAuditing()) {
                    this.___transaction.getSessionManager().auditLog(this, AuditType.DELETE, "LINKLIST DELETE: ", edgeToRemove + " : " + field + " : " + f.get(this.___proxiedObject));
                }
                // eliminar el objecto
                // this.sm.delete(f.get(realObj));
                if (f.get(this.___proxiedObject) != null) {
                    LOGGER.log(Level.FINER, "La referencia aún existe. Eliminar el objeto directamente");
                    this.___transaction.delete(f.get(this.___proxiedObject));
                } else {
                    LOGGER.log(Level.FINER, "la referencia estaba en null, recupear y eliminar el objeto.");
                    this.___transaction.delete(this.___transaction.get(outRid));
                }
            }

            f.setAccessible(acc);

        } catch (SecurityException | IllegalArgumentException | NoSuchFieldException ex) {
            Logger.getLogger(SessionManager.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            Logger.getLogger(ObjectProxy.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }


    /**
     * Revierte el objeto al estado que tiene el Vertex original.
     */
    @Override
    public synchronized void ___rollback() {
        LOGGER.log(Level.FINER, "\n\n******************* ROLLBACK *******************\n\n");
        LOGGER.log(Level.FINER, ThreadHelper.getCurrentStackTrace());

        this.___transaction.initInternalTx();

        this.___transaction.activateOnCurrentThread();
        // si es un objeto nuevo
        LOGGER.log(Level.FINER, "RID: " + this.___baseElement.getIdentity().toString() + " Nueva?: " + this.___baseElement.getIdentity().isNew());
        if (this.___baseElement.getIdentity().isNew()) {
            // invalidar el objeto
            LOGGER.log(Level.FINER, "El objeto aún no se ha persistido en la base. Invalidar");
            this.___isValidObject = false;
            return;
        }
        // asegurarse que la marca de borrado sea eliminada.
        this.___deletedMark = false;

        // recargar todo.
        this.___baseElement.reload();

        LOGGER.log(Level.FINER, "vmap: " + this.___baseElement.getProperties());
        // restaurar los atributos al estado original.
        ClassDef classdef = this.___transaction.getObjectMapper().getClassDef(___proxiedObject);
        Map<String, Class<?>> fieldmap = classdef.fields;

        Field f;
        for (Map.Entry<String, Class<?>> entry : fieldmap.entrySet()) {
            String prop = entry.getKey();
            Class<? extends Object> fieldClazz = entry.getValue();

            LOGGER.log(Level.FINER, "Rollingback field {0} ....", new String[]{prop});
            Object value = this.___baseElement.getProperty(prop);
            try {
                // obtener la clase a la que pertenece el campo
                Class<?> fc = fieldmap.get(prop);

                f = ReflectionUtils.findField(this.___baseClass, prop);

                boolean acc = f.isAccessible();
                f.setAccessible(true);

//                if (f.getType().isEnum()) {
//                    LOGGER.log(Level.FINER, "Enum field: " + f.getName() + " type: " + f.getType() + "  value: " + value + "   Enum val: " + Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
////                    f.set(oproxied, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
//                    this.setFieldValue(oproxied, prop, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
//                    f.set(___proxyObject, value);
//                } else 
                if (f.getType().isAssignableFrom(List.class)) {
                    // se debe hacer una copia del la lista para no quede referenciando al objeto original
                    // dado que en la asignación solo se pasa la referencia del objeto.
                    LOGGER.log(Level.FINER, "Lista detectada: realizando una copia del contenido...");
                    f.set(___proxiedObject, new ArrayListEmbeddedProxy((IObjectProxy) ___proxiedObject, (List) value));
                } else if (f.getType().isAssignableFrom(Map.class)) {
                    // se debe hacer una copia del la lista para no quede referenciando al objeto original
                    // dado que en la asignación solo se pasa la referencia del objeto.
                    LOGGER.log(Level.FINER, "Map detectado: realizando una copia del contenido...");
                    // FIXME: Ojo que se hace solo un shalow copy!! no se está conando la clave y el value
                    f.set(___proxiedObject, new HashMapEmbeddedProxy((IObjectProxy) ___proxiedObject, (Map) value));
                } else {
                    f.set(___proxiedObject, value);
                }
                LOGGER.log(Level.FINER, "hidratado campo: " + prop + "=" + value);
                f.setAccessible(acc);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // procesar los enum
        LOGGER.log(Level.FINER, "Procesando los enums...");
        for (Map.Entry<String, Class<?>> entry : classdef.enumFields.entrySet()) {
            String prop = entry.getKey();
            Class<? extends Object> fieldClazz = entry.getValue();

            LOGGER.log(Level.FINER, "Buscando campo {0} ....", new String[]{prop});
            Object value = this.___baseElement.getProperty(prop);
            try {
                // obtener la clase a la que pertenece el campo
                Class<?> fc = fieldmap.get(prop);
                // FIXME: este código se puede mejorar. Tratar de usar solo setFieldValue()
                f = ReflectionUtils.findField(this.___baseClass, prop);

                boolean acc = f.isAccessible();
                f.setAccessible(true);

                if (value != null) {
                    f.set(this.___proxiedObject, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
                } else {
                    f.set(this.___proxiedObject, null);
                }

                LOGGER.log(Level.FINER, "hidratado campo: " + prop + "=" + value);
                f.setAccessible(acc);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        LOGGER.log(Level.FINER, "Revirtiendo los Links......... ");
        // hidratar los atributos @links
        // procesar todos los links
        for (Map.Entry<String, Class<?>> entry : classdef.links.entrySet()) {
//        classdef.links.entrySet().stream().forEach((entry) -> {
            try {
                String field = entry.getKey();
                Class<?> fc = entry.getValue();

                Field fLink = ReflectionUtils.findField(this.___baseClass, field);
                boolean acc = fLink.isAccessible();
                fLink.setAccessible(true);

                fLink.set(this.___proxiedObject, null);
                fLink.setAccessible(acc);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // volver a activar la carga de los links
        this.___loadLazyLinks = true;

        // revertir las colecciones
        // procesar todos los linkslist
        LOGGER.log(Level.FINER, "Revirtiendo las colecciones...");
        for (Map.Entry<String, Class<?>> entry : classdef.linkLists.entrySet()) {

            try {
                // FIXME: se debería considerar agregar una annotation EAGER!
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
                Field fLink = ReflectionUtils.findField(this.___baseClass, field);
                boolean acc = fLink.isAccessible();
                fLink.setAccessible(true);

                ILazyCalls lc = (ILazyCalls) fLink.get(___proxiedObject);
                if (lc != null) {
                    lc.rollback();
                }

                fLink.setAccessible(acc);

            } catch (NoSuchFieldException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        this.___transaction.closeInternalTx();
    }

}
