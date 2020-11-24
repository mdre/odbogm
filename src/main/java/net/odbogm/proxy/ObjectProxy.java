package net.odbogm.proxy;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.record.ORecordElement;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import net.odbogm.exceptions.ConcurrentModification;
import net.odbogm.exceptions.DuplicateLink;
import net.odbogm.exceptions.InvalidObjectReference;
import net.odbogm.exceptions.ObjectMarkedAsDeleted;
import net.odbogm.exceptions.OdbogmException;
import net.odbogm.utils.ReflectionUtils;
import net.odbogm.utils.ThreadHelper;
import net.odbogm.utils.VertexUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.odbogm.annotations.DontLoadLinks;
import net.odbogm.annotations.Eager;

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

    private final Class<?> ___baseClass;

    // Vértice desde el que se obtiene el objeto.
    // private OVertex baseVertex;
    private OElement ___baseElement;

    // permite marcar el objeto como inválida en caso que se haga un rollback 
    // sobre un objeto que nunca se persistió.
    private boolean ___isValidObject = true;

    private final Transaction ___transaction;

    private boolean ___dirty = false;

    // determina si ya se han cargado los links o no
    private boolean ___loadLazyLinks = true;

    // determina si el objeto ya ha sido completamente inicializado.
    // sirve para impedir que se invoquen a los métodos durante el setup inicial del construtor.
    private boolean ___objectReady = false;

    // si esta marca está activa indica que el objeto ha sido eliminado de la base de datos 
    // y toda comunicación con el mismo debe ser abortada
    private boolean ___deletedMark = false;


    public ObjectProxy(Class c, OElement e, Transaction t) {
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
            return this.___getVertex();
        }
        if (method.getName().equals("___getEdge")) {
            return this.___getEdge();
        }
        if (method.getName().equals("___geElement")) {
            return this.___getElement();
        }

        if (method.getName().equals("___getRid")) {
            return this.___getRid();
        }
        
        if (method.getName().equals("___getBaseClass")) {
            return this.___getBaseClass();
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
        
        //if object was deleted:
        if (this.___deletedMark) {
            switch (method.getName()) {
                case "equals":
                case "hashCode":
                    if (!this.___transaction.getSessionManager().getConfig().
                            isEqualsAndHashCodeOnDeletedThrowsException()) {
                        return methodProxy.invokeSuper(o, args);
                    }
                default:
                    throw new ObjectMarkedAsDeleted("The object " + this.___baseElement.getIdentity().toString() + " was deleted from the database. Trying to call to " + method.getName());
            }
        }
        
        // modificar el llamado
        switch (method.getName()) {
            case "___uptadeVersion":
                if (this.___objectReady) {
                    this.___uptadeVersion();
                }
                break;
            case "___injectRid":
                if (this.___objectReady) {
                    this.___injectRid();
                }
                break;
            case "___getProxiedObject":
                if (this.___objectReady) {
                    res = this.___getProxiedObject();
                }
                break;
            case "___loadLazyLinks":
                if (this.___objectReady) {
                    this.___loadLazyLinks();
                }
                break;
            case "___eagerLoad":
                if (this.___objectReady) {
                    this.___eagerLoad();
                }
                break;
            case "___fullLoad":
                if (this.___objectReady) {
                    this.___fullLoad();
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
                 * evitar realizar el load sin necesidad.
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
            case "___setDeletedMark":
                this.___setDeletedMark();
                break;

            case "___ogm___setDirty":
                res = methodProxy.invokeSuper(o, args);
                break;
            case "___ogm___isDirty":
                res = methodProxy.invokeSuper(o, args);
                break;

            default:
                // invoke the method on the real object with the given params:

                if (method.getName().equals("toString")) {
                    try {
                        //if object doesn't have toString defined, we implement one on the fly
                        ReflectionUtils.findMethod(this.___baseClass, "toString", (Class<?>[]) null);
                    } catch (NoSuchMethodException nsme) {
                        res = this.___baseElement.getIdentity().toString(); //returns rid
                        break;
                    }
                }

                if (this.___loadLazyLinks) {
                    boolean methodTriggersLoadLazyLink = true;
                    if (method.getName().equals("equals") || method.getName().equals("hashCode")) {
                        methodTriggersLoadLazyLink &= this.___transaction.getSessionManager().
                                getConfig().isEqualsAndHashCodeTriggerLoadLazyLinks();
                    }
                    methodTriggersLoadLazyLink &= !method.isAnnotationPresent(DontLoadLinks.class);
                    if (this.___objectReady && methodTriggersLoadLazyLink) {
                        this.___loadLazyLinks();
                    }
                }

                LOGGER.log(Level.FINEST, "Invoking: {0}", method.getName());
                res = methodProxy.invokeSuper(o, args);

                // verificar si hay diferencias entre los objetos dependiendo de la estrategia seleccionada.
                if (this.___objectReady) {
                    switch (this.___transaction.getSessionManager().getActivationStrategy()) {
                        case CLASS_INSTRUMENTATION:
                            // si se está usando la instrumentación de clase, directamente verificar en el objeto
                            // cual es su estado.
                            LOGGER.log(Level.FINEST, "o: {0} ITrans: {1}", new Object[]{o.getClass().getName(), o instanceof ITransparentDirtyDetector});
                            if (((ITransparentDirtyDetector) o).___ogm___isDirty()) {
                                LOGGER.log(Level.FINEST, "objeto {0} marcado como dirty por ASM. Agregarlo a la lista de pendientes.", o.getClass().getName());
                                this.___setDirty();
                            }
                    }
                }
                break;
        }
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
        this.___injectRid();
        this.___objectReady = true;
    }
    
    
    @Override
    public void ___injectRid() {
        //inject RID if the field is defined
        ClassDef classdef = getClassDef();
        if (classdef.ridField != null) {
            objectMapper().setFieldValue(___proxiedObject, classdef.ridField, ___baseElement.getIdentity().toString());
        }
    }
    
    
    @Override
    public void ___uptadeVersion() {
        ClassDef classdef = getClassDef();
        if (classdef.versionField != null) {
            objectMapper().setFieldValue(___proxiedObject, classdef.versionField, ___baseElement.getProperty("@version"));
        }
    }


    /**
     * retorna el OElement asociado a este proxi.
     *
     * @return referencia al OElement
     */
    @Override
    public OElement ___getElement() {
        return this.___baseElement;
    }

    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return referencia al OVertex
     */
    @Override
    public OVertex ___getVertex() {
        if (this.___baseElement.isVertex()) {
            return (OVertex) this.___baseElement;
        } else {
            return null;
        }
    }


    /**
     * Returns the Record ID of the associated element.
     *
     * @return RID of element.
     */
    @Override
    public String ___getRid() {
        if (this.___baseElement != null) {
            return this.___baseElement.getIdentity().toString();
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
    public void ___setVertex(OVertex v) {
        this.___baseElement = v;
    }


    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return la referencia al OVertex
     */
    @Override
    public OEdge ___getEdge() {
        if (this.___baseElement.isEdge()) {
            return (OEdge) this.___baseElement;
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
    public void ___setEdge(OEdge e) {
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
     * Load all links of object.
     */
    @Override
    public synchronized void ___loadLazyLinks() {
        if (this.___loadLazyLinks) {
            this.___transaction.initInternalTx();
            
            LOGGER.log(Level.FINER, "Base class: {0}", this.___baseClass.getSimpleName());
            LOGGER.log(Level.FINER, "iniciando loadLazyLinks...");
            boolean currentDirtyState = this.___isDirty();
            // marcar que ya se han incorporado todo los links
            this.___loadLazyLinks = false;

            if (this.___baseElement instanceof OVertex) {
                // hydrate links attributes: process all links and indirectLinks
                this.updateLinks();
                this.updateIndirectLinks();
            }

            // resetear dirty si corresponde.
            this.___dirty = currentDirtyState;

            this.___transaction.closeInternalTx();
        }
    }
    
    
    private void updateLinks() {
        ClassDef classdef = getClassDef();
        OVertex ov = (OVertex) this.___baseElement;
        LOGGER.log(Level.FINER, "Processing {0} links ", classdef.links.size());
        this.loadLinks(ov, classdef, classdef.links, false, false);
    }
    
    
    /**
     * Load the links of the given vertex.
     * 
     * @param ov The vertex.
     * @param linksFields Fields to hydrate.
     * @param onlyEager If true, only fields marked as Eager, else all fields.
     * @param indirect If must load indirect links instead of direct.
     */
    private void loadLinks(OVertex ov, ClassDef classdef, HashMap<String, Class<?>> linksFields, boolean onlyEager, boolean indirect) {
        for (Map.Entry<String, Class<?>> entry : linksFields.entrySet()) {
            try {
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                Field fLink = classdef.fieldsObject.get(field);
                
                if (onlyEager && !fLink.isAnnotationPresent(Eager.class)) {
                    continue; //only eager, discard field. Go to next
                }
                
                String graphRelationName;
                ODirection direction;
                
                if (indirect) {
                    //the name configured in annotation must be used
                    Indirect in = fLink.getAnnotation(Indirect.class);
                    graphRelationName = in.linkName();
                    direction = ODirection.IN;
                    LOGGER.log(Level.FINER, "Se ha detectado un indirect. Linkname = {0}", new Object[]{in.linkName()});
                } else {
                    graphRelationName = classdef.entityName + "_" + field;
                    direction = ODirection.OUT;
                }
                LOGGER.log(Level.FINER, "Field: {0}.{1}   Class: {2}  RelationName: {3}",
                        new String[]{this.___baseClass.getSimpleName(), field,
                            fc.getSimpleName(), graphRelationName});

                // recuperar de la base el vértice correspondiente
                boolean duplicatedLinkGuard = false;
                for (OVertex vertice : ov.getVertices(direction, graphRelationName)) {
                    LOGGER.log(Level.FINER, "hydrate innerO: {0}", vertice.getIdentity());

                    if (!duplicatedLinkGuard) {
                        /*
                         * FIXME: esto genera una dependencia cruzada.
                         * Habría que revisar
                         * como solucionarlo. Esta llamada se hace para
                         * que quede el objeto
                         * mapeado
                         */
                        this.___transaction.addToTransactionCache(this.___getRid(), ___proxiedObject);

                        // si es una interface llamar a get solo con el RID.
                        Object innerO = fc.isInterface() ? this.___transaction.get(vertice.getIdentity().toString()) :
                                this.___transaction.get(fc, vertice.getIdentity().toString());

                        LOGGER.log(Level.FINER, "Inner object {0}: {1}  FC: {2}   innerO.class: {3} hashCode: {4}", new Object[]{
                            field, vertice, fc.getSimpleName(), innerO.getClass().getSimpleName(), System.identityHashCode(innerO)});
                        fLink.set(this.___proxiedObject, fc.cast(innerO));
                        duplicatedLinkGuard = true;

                        ___transaction.decreseTransactionCache();
                    } else if (false) {
                        throw new DuplicateLink();
                    }
                    LOGGER.log(Level.FINER, "FIN hydrate innerO: {0}^^^^^^^^^^^^^^^^^^^^^^^^^^^^^", vertice.getIdentity());
                }
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }


    private void updateIndirectLinks() {
        boolean preservDirtyState = this.___dirty;

        OVertex ov = (OVertex) this.___baseElement;
        ClassDef classdef = getClassDef();

        LOGGER.log(Level.FINER, "procesando {0} indirected links", new Object[]{classdef.indirectLinks.size()});
        this.loadLinks(ov, classdef, classdef.indirectLinks, false, true);

        
        //ANALYZE WELL IF THIS CODE IS VALID OR UNNECESSARY AND REMOVE IT
        
//        // forzar la recarga de las colecciones.
//        LOGGER.log(Level.FINER, "Refrescando las colecciones indirectas...");
        // ********************************************************************************************
        // hidratar las colecciones indirectas
        // procesar todos los indirectLinkslist
        // ********************************************************************************************
//            for (Map.Entry<String, Class<?>> entry : classdef.indirectLinkLists.entrySet()) {
//                try {
//                    // FIXME: se debería considerar agregar una annotation EAGER!
//                    String field = entry.getKey();
//                    Class<?> fc = entry.getValue();
//                    LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
//
//                    Field fLink = classdef.fieldsObject.get(field);
//                    Direction relationDirection = Direction.IN;
//
//                    Indirect in = fLink.getAnnotation(Indirect.class);
//                    String graphRelationName = in.linkName();
//
//                    // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
//                    if ((ov.countEdges(relationDirection, graphRelationName) > 0) || (fLink.get(___proxiedObject) != null)) {
//                        this.___transaction.getObjectMapper().collectionToLazy(___proxiedObject, field, fc, ov, ___transaction);
//                    }
//
//                } catch (IllegalAccessException | IllegalArgumentException ex) {
//                    Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }


        //----------------------------------------------------------------------
        //restart indirect collections to reload them
//        classdef.indirectLinks.keySet().forEach(field -> {
//            Field f = classdef.fieldsObject.get(field);
//                try {
//                    ILazyCalls coll = (ILazyCalls)f.get(this.___proxiedObject);
//                    coll.rollback();
//                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    LOGGER.log(Level.SEVERE, "Error eager loading link list! " + f, ex);
//            }
//        });
        //----------------------------------------------------------------------

        // volver a establecer el estado de Dirty.
        this.___dirty = preservDirtyState;
    }
    
    
    /**
     * Loads the vertex links marked as eager load.
     */
    @Override
    public void ___eagerLoad() {
        ClassDef classdef = getClassDef();
        if (classdef.isEager && this.___baseElement instanceof OVertex) {
            this.___transaction.initInternalTx();
            LOGGER.log(Level.FINER, "Eager loading of {0}", this.___baseElement);
            
            if (this.___baseClass.isAnnotationPresent(Eager.class)) {
                //eager load of whole class
                this.fullLoad();
            } else {
                OVertex ov = (OVertex) this.___baseElement;
                this.loadLinks(ov, classdef, classdef.links, true, false); //eager load of links
                this.eagerLoadLinkLists(classdef, classdef.linkLists, true); //eager load of link lists
                this.loadLinks(ov, classdef, classdef.indirectLinks, true, true); //eager load of indirect links
                this.eagerLoadLinkLists(classdef, classdef.indirectLinkLists, true); //eager load of indirect link lists
            }
            this.___transaction.closeInternalTx();
        }
    }
    
    
    /**
     * Loads all the vertex links.
     */
    @Override
    public void ___fullLoad() {
        this.fullLoad();
    }
    
    
    private void fullLoad() {
        ClassDef classdef = getClassDef();
        this.___loadLazyLinks();
        this.eagerLoadLinkLists(classdef, classdef.linkLists, false); //force load of link lists
        this.eagerLoadLinkLists(classdef, classdef.indirectLinkLists, false); //force load of indirect link lists
    }
    
    
    /**
     * Forces the load of collections of links.
     */
    private void eagerLoadLinkLists(ClassDef classdef, HashMap<String, Class<?>> linksFields, boolean onlyEager) {
        linksFields.keySet().forEach(field -> {
            Field f = classdef.fieldsObject.get(field);
            if (!onlyEager || f.isAnnotationPresent(Eager.class)) {
                ILazyCalls coll = (ILazyCalls)objectMapper().getFieldValue(this.___proxiedObject, f);
                if (coll != null) coll.forceLoad();
            }
        });
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
        //ante ciertas condiciones los métodos de Orient pueden lanzar una excepción
        //que hay que atraparlas en algún momento
        try {
            doCommit();
            
        } catch (OException ex) {
            throw new OdbogmException(ex, ___transaction);
        }
    }
    
    
    private void doCommit() throws ConcurrentModification {
        LOGGER.log(Level.FINER, "Iniciando ___commit() ....");
        LOGGER.log(Level.FINER, "valid: {0}", this.___isValidObject);
        LOGGER.log(Level.FINER, "dirty: {0}", this.___dirty);
        LOGGER.log(Level.FINER, "new: {0}", this.___baseElement.getIdentity().isNew());
        LOGGER.log(Level.FINER, "rid: {0}", this.___baseElement.getIdentity());
        LOGGER.log(Level.FINER, "database: {0}", this.___baseElement.getDatabase());

        
        if (this.___dirty || this.___baseElement.getIdentity().isNew()) {
            this.___transaction.initInternalTx();
            LOGGER.log(Level.FINER, "database after open: {0}", this.___baseElement.getDatabase());
            // verifciar que si el objeto es nuevo esté correctamente confirgurado.
            // puede cambiar en el caso de que un commit falle y lo deja inconsistente.
            if (this.___baseElement.getIdentity().isNew()) {
                LOGGER.log(Level.FINEST, "*********************************");
                LOGGER.log(Level.FINEST, "Forzar el status a LOADED y Dirty");
                LOGGER.log(Level.FINEST, "*********************************");
                this.___baseElement.setInternalStatus(ORecordElement.STATUS.LOADED);
                this.___baseElement.setDirty();
            } else {
                //FIXME: workaround para la grabación inconsistente de los edges en los vertex.
                OElement oeTmp = this.___transaction.getCurrentGraphDb().load(this.___baseElement.getIdentity());
                
                //FIXME: si se hubiera eliminado el Vertex el resultado es null. Para que siga con los errores de la base
                // no modifico las referencias.
                if (oeTmp!=null) {
                    if (this.___baseElement.getVersion() != oeTmp.getVersion()) {
                        throw new ConcurrentModification(new OConcurrentModificationException(this.___baseElement.getIdentity(),
                                                                                              oeTmp.getVersion(), 
                                                                                              this.___baseElement.getVersion(),
                                                                                              0
                                                                                              ), ___transaction);
                    }
                    // si todo está ok
                    // reemplazar la referencia y continuar
                    LOGGER.log(Level.FINEST, "\n\nInstance replace with fresh database load\n\n");
                    this.___baseElement = oeTmp;
                }
            }
            
            
            // asegurarse que está atachado
//            if (this.___baseElement.getDatabase() == null) {
//                LOGGER.log(Level.FINER, "El objeto no está atachado!");
//                this.___transaction.attach(this.___baseElement);
//            }

            // obtener la definición de la clase
            ClassDef cDef = this.___transaction.getObjectMapper().getClassDef(this.___proxiedObject);

            // obtener un mapa actualizado del objeto contenido
            ObjectStruct oStruct = this.___transaction.getObjectMapper().objectStruct(this.___proxiedObject);
            Map<String, Object> omap = oStruct.fields;

            // bajar todo al vértice
            
            VertexUtils.fillElement(this.___baseElement,omap);
            
            oStruct.removedProperties.forEach(prop -> this.___baseElement.removeProperty(prop));

            // guardar log de auditoría si corresponde.
            if (this.___transaction.isAuditing() && !this.___baseElement.getIdentity().isNew()) {
                this.___transaction.auditLog(this, AuditType.WRITE, "UPDATE", omap);
            }
            
            // si se trata de un Vértice
            if (this.___baseElement.isVertex()) {
                OVertex ov = this.___baseElement.asVertex().get();
                // Analizar si cambiaron los vértices
                /*
                 * procesar los objetos internos. Primero se deber determinar
                 * si los objetos ya existían en el contexto actual. Si no
                 * existen deben ser creados.
                 */
                for (Map.Entry<String, Class<?>> link : cDef.links.entrySet()) {
                    String field = link.getKey();
                    String graphRelationName = cDef.entityName + "_" + field;
                    // determinar el estado del campo
                    if (oStruct.links.get(field) == null) {
                        // si está en null, es posible que se haya eliminado el objeto
                        // por lo cual se debería eliminar el vértice correspondiente
                        // si es que existe
                        if (ov.getEdges(ODirection.OUT, graphRelationName).iterator().hasNext()) {
                            // se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente
                            LOGGER.log(Level.FINEST, "se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente");
                            OEdge removeEdge = null;
                            for (OEdge edge : ov.getEdges(ODirection.OUT, graphRelationName)) {
                                removeEdge = (OEdge) edge;
                                if (this.___transaction.isAuditing()) {
                                    this.___transaction.auditLog(this, AuditType.WRITE, "REMOVE LINK: " + graphRelationName, removeEdge);
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
                            LOGGER.log(Level.FINEST, "Se encontró una relación a un objeto ADMINISTRADO");
                            // el objeto existía.
                            // se debe verificar si el eje entre los dos objetos ya existía.
                            if (!VertexUtils.isConectedTo(ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName)) {
                                // No existe un eje. Se debe crear
                                LOGGER.log(Level.FINER, "Los objetos no están conectados. ({0} |--|{1}",
                                        new Object[]{ov.getIdentity(), ((IObjectProxy) innerO).___getVertex().getIdentity()});

                                // primero verificar si no existía una relación previa con otro objeto para removerla.
                                Iterator<OEdge> toRemove = ov.getEdges(ODirection.OUT, graphRelationName).iterator();
                                if (toRemove.hasNext()) {
                                    LOGGER.log(Level.FINER, "Existía una relación previa. Se debe eliminar.");
                                    // existé una relación. Elimnarla antes de proceder a establecer la nueva.
                                    //OEdge removeEdge = null;
                                    while (toRemove.hasNext()) {
                                        OEdge removeEdge = toRemove.next();
                                        //removeEdge = (OEdge) edge;
                                        LOGGER.log(Level.FINER, "Eliminar relación previa a " + removeEdge.getTo());

                                        if (this.___transaction.isAuditing()) {
                                            this.___transaction.auditLog(this, AuditType.WRITE, "REMOVE LINK: " + graphRelationName, removeEdge);
                                        }
                                        
                                        this.removeEdge(removeEdge, field);
                                        
                                    }
                                }
                                LOGGER.log(Level.FINEST, "vertex out({0}: {1}",new Object[]{graphRelationName,ov.getEdges(ODirection.OUT, graphRelationName)});
                                
                                
                                LOGGER.log(Level.FINER, "Agregar un link entre dos objetos existentes.");
                                //OEdge oe = this.___transaction.getSessionManager().getGraphdb().addEdge("class:" + graphRelationName, ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                                OEdge oe = ov.addEdge(((IObjectProxy) innerO).___getVertex(), graphRelationName);
                                
//                                // FIXME: eliminar este bloque
//                                LOGGER.log(Level.FINEST, "\npost agregar el vertex");
//                                LOGGER.log(Level.FINEST, "edges: "+ov.getEdges(ODirection.OUT, graphRelationName));
//                                toRemove = ov.getEdges(ODirection.OUT, graphRelationName).iterator();
//                                while (toRemove.hasNext()) {
//                                        OEdge removeEdge = toRemove.next();
//                                        LOGGER.log(Level.FINEST, "edge: "+removeEdge);
//                                }
//                                //-------------------------------
                                
                                if (this.___transaction.isAuditing()) {
                                    this.___transaction.auditLog(this, AuditType.WRITE, "ADD LINK: " + graphRelationName, oe);
                                }
                            }
                        } else {
                            // el objeto es nuevo
                            LOGGER.log(Level.FINEST, "Se encontró una relación a un objeto NUEVO");
                            // primero verificar si no existía una relación previa con otro objeto para removerla.
                            if (ov.getEdges(ODirection.OUT, graphRelationName).iterator().hasNext()) {
                                LOGGER.log(Level.FINER, "Existía una relación previa. Se debe eliminar.");
                                // existé una relación. Elimnarla antes de proceder a establecer la nueva.
                                //OEdge removeEdge = null;
                                while (ov.getEdges(ODirection.OUT, graphRelationName).iterator().hasNext()) {
                                    OEdge removeEdge = ov.getEdges(ODirection.OUT, graphRelationName).iterator().next();
                                    
                                    LOGGER.log(Level.FINER, "Eliminar relación previa a " + removeEdge.getTo());
                                    if (this.___transaction.isAuditing()) {
                                        this.___transaction.auditLog(this, AuditType.WRITE, "REMOVE LINK: " + graphRelationName, removeEdge);
                                    }
                                    this.removeEdge(removeEdge, field);
                                }
                            }

                            // crear la nueva relación
                            LOGGER.log(Level.FINER, "innerO nuevo. Crear un vértice y un link");
                            innerO = this.___transaction.store(innerO);
                            this.___transaction.getObjectMapper().setFieldValue(this.___proxiedObject, field, innerO);

                            // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                            if (innerO instanceof ITransparentDirtyDetector) {
                                ((ITransparentDirtyDetector) innerO).___ogm___setDirty(false);
                            }

                            //OEdge oe = this.___transaction.getCurrentGraphDb().addEdge("class:" + graphRelationName, ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                            OEdge oe = ov.addEdge(((IObjectProxy) innerO).___getVertex(), graphRelationName);
                            if (this.___transaction.isAuditing()) {
                                this.___transaction.auditLog(this, AuditType.WRITE, "ADD LINK: " + graphRelationName, oe);
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
                        LOGGER.log(Level.FINER, "procesando campo: {0} clase: {1}",
                                new Object[]{field, this.___proxiedObject.getClass()});

                        f = cDef.fieldsObject.get(field);

                        // preparar el nombre de la relación
                        final String graphRelationName = cDef.entityName + "_" + field;

                        Object collectionFieldValue = f.get(this.___proxiedObject);

                        // verificar si existe algún cambio en la colecciones
                        // ingresa si la colección es distinta de null y
                        // collectionFieldValue es instancia de ILazyCalls y está marcado como dirty
                        // o collectionFieldValue no es instancia de ILazyCalls, lo que 
                        // significa que es una colección nueva y debe ser procesada completamente.
                        if ((collectionFieldValue != null)
                                && ((ILazyCalls.class.isAssignableFrom(collectionFieldValue.getClass()) && ((ILazyCalls) collectionFieldValue).isDirty())
                                || (!ILazyCalls.class.isAssignableFrom(collectionFieldValue.getClass())))) {
                            LOGGER.log(Level.FINER, (!ILazyCalls.class.isAssignableFrom(collectionFieldValue.getClass()))
                                    ? "No es instancia de ILazyCalls"
                                    : "Es instancia de Lazy y está marcado como DIRTY");

                            if (collectionFieldValue instanceof List) {
                                ILazyCollectionCalls lazyCollectionCalls;
                                // procesar la colección

                                if (ILazyCollectionCalls.class.isAssignableFrom(
                                        collectionFieldValue.getClass())) {
                                    lazyCollectionCalls = (ILazyCollectionCalls) collectionFieldValue;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    this.___transaction.getObjectMapper().collectionToLazy(
                                            this.___proxiedObject, field, ov, this.___transaction);

                                    //recuperar la nueva colección
                                    Collection inter = (Collection) f.get(this.___proxiedObject);

                                    //agregar todos los valores que existían
                                    inter.addAll((Collection) collectionFieldValue);
                                    //preparar la interface para que se continúe con el acceso.
                                    lazyCollectionCalls = (ILazyCollectionCalls) inter;
                                    // reasignar el objeto oCol
                                    collectionFieldValue = f.get(this.___proxiedObject);
                                }

                                List listFieldValue = (List) collectionFieldValue;
                                Map<Object, ObjectCollectionState> colState = lazyCollectionCalls.collectionState();

                                // procesar los elementos presentes en la colección
                                for (int i = 0; i < listFieldValue.size(); i++) {
                                    Object colObject = listFieldValue.get(i);
                                    // verificar el estado del objeto en la colección
                                    if (colState.get(colObject) == ObjectCollectionState.ADDED) {
                                        // si se agregó uno, determinar si era o no manejado por el SM
                                        if (!(colObject instanceof IObjectProxy)) {
                                            LOGGER.log(Level.FINER, "Objeto nuevo. Insertando en la base y reemplazando el original...");
                                            // no es un objeto que se haya almacenado.
                                            colObject = this.___transaction.store(colObject);
                                            // reemplazar en la colección el objeto por uno administrado
                                            listFieldValue.set(i, colObject);

                                            // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                                            if (colObject instanceof ITransparentDirtyDetector) {
                                                ((ITransparentDirtyDetector) colObject).___ogm___setDirty(false);
                                            }

                                        }

                                        // vincular el nodo
                                        //OEdge oe = this.___transaction.getCurrentGraphDb().addEdge("class:" + graphRelationName, this.___getVertex(), ((IObjectProxy) colObject).___getVertex(), graphRelationName);
                                        OEdge oe = this.___getVertex().addEdge(((IObjectProxy) colObject).___getVertex(), graphRelationName);

                                        if (this.___transaction.isAuditing()) {
                                            this.___transaction.auditLog(this, AuditType.WRITE, "LINKLIST ADD: " + graphRelationName, oe);
                                        }
                                    }
                                }

                                // procesar los removidos solo si está el anotation en el campo
                                for (Map.Entry<Object, ObjectCollectionState> entry1 : colState.entrySet()) {
                                    Object colObject = entry1.getKey();
                                    ObjectCollectionState colObjState = entry1.getValue();

                                    if (colObjState == ObjectCollectionState.REMOVED) {
                                        // remover el link
                                        //for (OEdge edge : ((OVertex) this.___baseElement)
                                        //        .getEdges(((IObjectProxy) colObject).___getVertex(),
                                        //                ODirection.OUT,
                                        //                graphRelationName))
                                        Iterator<OEdge> edges = ((OVertex) this.___baseElement).getEdges(ODirection.OUT, graphRelationName).iterator(); 
                                        while (edges.hasNext()){
                                            OEdge edge = edges.next();
                                            if (edge.getTo().getIdentity().equals(((IObjectProxy) colObject).___getVertex().getIdentity())) {
                                                if (this.___transaction.isAuditing()) {
                                                    this.___transaction.auditLog(this, AuditType.WRITE, "LINKLIST REMOVE: " + graphRelationName, edge);
                                                }
                                                edge.delete();
                                            }
                                        }
                                        // si existe la anotación, remover tambien el vertex
                                        if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                            if (this.___transaction.isAuditing()) {
                                                this.___transaction.auditLog(this, AuditType.DELETE, "LINKLIST DELETE: " + graphRelationName, colObject);
                                            }
                                            this.___transaction.delete(colObject);
                                        }
                                    }
                                }

                                // resetear el estado
                                lazyCollectionCalls.clearState();

                            } else if (collectionFieldValue instanceof Map) {

                                Map mapFieldValue;
                                // procesar la colección

                                if (ILazyMapCalls.class.isAssignableFrom(collectionFieldValue.getClass())) {
                                    mapFieldValue = (Map) collectionFieldValue;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    // this.sm.getObjectMapper().colecctionToLazy(this.realObj, field, ov);
                                    this.___transaction.getObjectMapper().collectionToLazy(this.___proxiedObject, field, ov, this.___transaction);
                                    //recuperar la nueva colección
                                    // Collection inter = (Collection) f.get(this.realObj);
                                    Map inter = (Map) f.get(this.___proxiedObject);
                                    //agregar todos los valores que existían
                                    inter.putAll((Map) collectionFieldValue);
                                    //preparar la interface para que se continúe con el acceso.
                                    mapFieldValue = (Map) inter;
                                }

                                // refrescar los estados
                                ILazyMapCalls lazyMap = (ILazyMapCalls) mapFieldValue;
                                final Map<Object, ObjectCollectionState> keysState = lazyMap.collectionState();
                                final Map<Object, OEdge> keysToEdges = lazyMap.getKeyToEdge();
                                final Map<Object, ObjectCollectionState> entitiesState = lazyMap.getEntitiesState();

                                // recorrer todas las claves del mapa
                                for (Map.Entry<Object, ObjectCollectionState> entry1 : keysState.entrySet()) {
                                    Object key = entry1.getKey();
                                    ObjectCollectionState keyState = entry1.getValue();

                                    LOGGER.log(Level.FINER, "imk: {0} state: {1}", new Object[]{key, keyState});
                                    // para cada entrada, verificar la existencia del objeto y crear un Edge.
                                    Object linkedO = mapFieldValue.get(key);

                                    if (keyState != ObjectCollectionState.REMOVED &&
                                            !(linkedO instanceof IObjectProxy)) {
                                        LOGGER.log(Level.FINER, "Link Map Object nuevo. Crear un vértice y un link");
                                        linkedO = this.___transaction.store(linkedO);
                                        mapFieldValue.replace(key, linkedO);
                                        if (linkedO instanceof ITransparentDirtyDetector) {
                                            ((ITransparentDirtyDetector) linkedO).___ogm___setDirty(false);
                                        }
                                    }

                                    // verificar el estado del objeto en la colección.
                                    switch (keyState) {
                                        case ADDED:
                                            // crear un link entre los dos objetos.
                                            LOGGER.log(Level.FINER, "-----> agregando un LinkList al Map!");
                                            OEdge oe = ((OVertex) this.___baseElement).addEdge(((IObjectProxy) linkedO).___getVertex(), graphRelationName);
                                            // actualizar el edge con los datos de la key.
                                            this.___transaction.getObjectMapper().fillSequenceFields(key, this.___transaction, null);
                                            VertexUtils.fillElement(oe,this.___transaction.getObjectMapper().simpleMap(key));

                                            if (this.___transaction.isAuditing()) {
                                                this.___transaction.auditLog(this, AuditType.WRITE, "LINKLIST ADD: " + graphRelationName, oe);
                                            }
                                            
                                            //update the map with the managed key
                                            lazyMap.updateKey(key, oe);
                                            break;

                                        case NOCHANGE:
                                            // el link no se ha modificado. 
                                            break;

                                        case REMOVED:
                                            // quitar el Edge
                                            OEdge oeRemove = keysToEdges.get(key);
                                            if (oeRemove == null) {
                                                throw new IllegalStateException("The edge object couldn't be found. "
                                                        + "Make sure its hashCode is change-proof.");
                                            }
                                            
                                            // verificar si corresponde borrar el vértice en caso de estar marcado con @RemoveOrphan.
                                            boolean removeOrphan = false;
                                            if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                                if (entitiesState.get(linkedO) == ObjectCollectionState.REMOVED) {
                                                    removeOrphan = true;
                                                }
                                            }
                                            removeEdge(graphRelationName, oeRemove, removeOrphan ?
                                                    (IObjectProxy)linkedO : null);
                                            break;
                                    }
                                }
                                
                                for (Map.Entry<Object, ObjectCollectionState> e : entitiesState.entrySet()) {
                                    Object value = e.getKey();
                                    ObjectCollectionState valueState = e.getValue();
                                    if (value instanceof IObjectProxy && valueState == ObjectCollectionState.REMOVED) {
                                        //we must remove the old edge
                                        IObjectProxy valueop = (IObjectProxy)value;
                                        boolean removeOrphan = f.isAnnotationPresent(RemoveOrphan.class);
                                        
                                        valueop.___getVertex().getEdges(ODirection.IN, graphRelationName).forEach(edge -> {
                                            if (Objects.equals(this.___baseElement, edge.getFrom())) {
                                                removeEdge(graphRelationName, edge, removeOrphan ? valueop : null);
                                            }
                                        });
                                    }
                                }
                                
                                ((ILazyMapCalls) mapFieldValue).clearState();
                            } else {
                                LOGGER.log(Level.FINER, "********************************************");
                                LOGGER.log(Level.FINER, "field: {0}", field);
                                LOGGER.log(Level.FINER, "********************************************");
                                throw new CollectionNotSupported(collectionFieldValue.getClass().getSimpleName());
                            }
                        }
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(SessionManager.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            //if we must update version field, enqueue in transaction
            if (cDef.versionField != null) {
                this.___transaction.processAfterDbCommit(this);
            }
            
            // grabar los cambios
            this.___baseElement.save();
            
            this.___transaction.closeInternalTx();
        }
        LOGGER.log(Level.FINER, "fin commit ----");
    }
    
    private void removeEdge(String graphRelationName, OEdge edge, IObjectProxy vertexToRemove) {
        if (this.___transaction.isAuditing()) {
            this.___transaction.auditLog(this, AuditType.WRITE, "LINKLIST REMOVE: " + graphRelationName, edge);
        }
        // FIXME: ojo que esto puede haber cambiado.
        edge.reload();
        //--- 
        
        edge.delete();
        if (vertexToRemove != null) {
            this.___transaction.delete(vertexToRemove);
            if (this.___transaction.isAuditing()) {
                this.___transaction.auditLog(this, AuditType.DELETE, "LINKLIST REMOVE: " + graphRelationName, vertexToRemove);
            }
        }
    }
    

    /**
     * Refresca el objeto base recuperándolo nuevamente desde la base de datos.
     */
    @Override
    public void ___reload() {
        this.___transaction.initInternalTx();

        this.___baseElement.reload();
        this.___uptadeVersion();

        this.___transaction.closeInternalTx();
    }


    /**
     * Función de uso interno para remover un eje
     *
     * @param edgeToRemove
     * @param field
     */
    private synchronized void removeEdge(OEdge edgeToRemove, String field) {
        try {
            ClassDef classdef = this.___transaction.getObjectMapper().getClassDef(___baseClass);
            Field f = classdef.fieldsObject.get(field);
//            Field f = ReflectionUtils.findField(this.___baseClass, field);

            String outTo = edgeToRemove.getTo().getIdentity().toString();
            LOGGER.log(Level.FINER, "El edge {0} apunta from: {1} ---(to)--> {2}",
                    new Object[]{edgeToRemove,
                        edgeToRemove.getFrom().getIdentity().toString(),
                        edgeToRemove.getTo().getIdentity().toString()});
            // remover primero el eje
            edgeToRemove.delete();
            edgeToRemove.save();
            // si corresponde
            if (f.isAnnotationPresent(RemoveOrphan.class)) {
                LOGGER.log(Level.FINER, "Remove orphan presente");
                //auditar
                if (this.___transaction.isAuditing()) {
                    this.___transaction.auditLog(this, AuditType.DELETE, "LINKLIST DELETE: ", edgeToRemove + " : " + field + " : " + f.get(this.___proxiedObject));
                }
                // eliminar el objecto
                // this.sm.delete(f.get(realObj));
                if (f.get(this.___proxiedObject) != null) {
                    LOGGER.log(Level.FINER, "La referencia aún existe. Eliminar el objeto directamente");
                    this.___transaction.delete(f.get(this.___proxiedObject));
                } else {
                    LOGGER.log(Level.FINER, "la referencia estaba en null, recupear y eliminar el objeto.");
                    this.___transaction.delete(this.___transaction.get(outTo));
                }
            }

        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
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

        // si es un objeto nuevo
        boolean isNew = this.___baseElement.getIdentity().isNew();
        LOGGER.log(Level.FINER, "RID: {0} Nueva?: {1}", new Object[]{this.___baseElement.getIdentity().toString(), isNew});
        if (isNew) {
            // invalidar el objeto
            LOGGER.log(Level.FINER, "El objeto aún no se ha persistido en la base. Invalidar");
            this.___isValidObject = false;
            return;
        }
        // asegurarse que la marca de borrado sea eliminada.
        this.___deletedMark = false;

        // recargar todo.
        this.___reload();

        //LOGGER.log(Level.FINER, "vmap: {0}", this.___baseElement.getProperties());
        // restaurar los atributos al estado original.
        ObjectMapper objectMapper = objectMapper();
        ClassDef classdef = getClassDef();
        
        
        LOGGER.log(Level.FINER, "Reverting basic attributes.........");
        for (var entry : classdef.fields.entrySet()) {
            String prop = entry.getKey();
            if (!classdef.embeddedFields.containsKey(prop)) {
                LOGGER.log(Level.FINER, "Rollingback field {0} ....", new String[]{prop});
                Object value = this.___baseElement.getProperty(prop);
                objectMapper.setFieldValue(___proxiedObject, prop, value);
            }
        }
        

        LOGGER.log(Level.FINER, "Reverting embedded collections.........");
        this.___transaction.getObjectMapper().hydrateEmbeddedCollections(
                classdef, (IObjectProxy)this.___proxiedObject, this.___baseElement);
        

        // procesar los enum
        Field f;
        LOGGER.log(Level.FINER, "Reverting enums...");
        for (Map.Entry<String, Class<?>> entry : classdef.enumFields.entrySet()) {
            String prop = entry.getKey();
            LOGGER.log(Level.FINER, "Buscando campo {0} ....", new String[]{prop});
            Object value = this.___baseElement.getProperty(prop);
            try {
                f = classdef.fieldsObject.get(prop);
                if (value != null) {
                    f.set(this.___proxiedObject, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
                } else {
                    f.set(this.___proxiedObject, null);
                }
                LOGGER.log(Level.FINER, "hidratado campo: {0}={1}", new Object[]{prop, value});
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        
        LOGGER.log(Level.FINER, "Reverting enum collections.........");
        this.___transaction.getObjectMapper().hydrateEnumCollections(
                classdef, (IObjectProxy)this.___proxiedObject, this.___baseElement);
        

        
        LOGGER.log(Level.FINER, "Revirtiendo los Links......... ");
        // hidratar los atributos @links
        // procesar todos los links
        for (Map.Entry<String, Class<?>> entry : classdef.links.entrySet()) {
            try {
                String field = entry.getKey();
                Field fLink = classdef.fieldsObject.get(field);
                fLink.set(this.___proxiedObject, null);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // volver a activar la carga de los links
        this.___loadLazyLinks = true;

        // revertir las colecciones
        // procesar todos los linkslist
        LOGGER.log(Level.FINER, "Revirtiendo las colecciones...");
        for (Map.Entry<String, Class<?>> entry : classdef.linkLists.entrySet()) {
            String field = entry.getKey();
            Class<?> fc = entry.getValue();
            LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});

            Field fLink = classdef.fieldsObject.get(field);
            Object coll = objectMapper.getFieldValue(this.___proxiedObject, fLink);
            if (coll != null && coll instanceof ILazyCalls) {
                ((ILazyCalls)coll).rollback();
            } else {
                objectMapper.setFieldValue(this.___proxiedObject, fLink, null);
            }
        }
        
        this.___removeDirtyMark();
        this.___transaction.closeInternalTx();
    }

    
    private ClassDef getClassDef() {
        return this.___transaction.getObjectMapper().getClassDef(this.___baseClass);
    }
    
    private ObjectMapper objectMapper() {
        return this.___transaction.getObjectMapper();
    }
    
}
