/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import net.odbogm.annotations.RemoveOrphan;
import net.odbogm.ObjectStruct;
import net.odbogm.SessionManager;
import net.odbogm.cache.ClassDef;
import net.odbogm.exceptions.CollectionNotSupported;
import net.odbogm.utils.ReflectionUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.lang.reflect.Field;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 *
 * @author SShadow
 */
public class ObjectProxy implements IObjectProxy, MethodInterceptor {
    
    private final static Logger LOGGER = Logger.getLogger(ObjectProxy.class.getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    // the real object      
    private Object ___proxyObject;
    private Class<?> ___baseClass;
    // Vértice desde el que se obtiene el objeto.
    // private OrientVertex baseVertex;
    private OrientElement ___baseElement;
    private SessionManager ___sm;
    private boolean ___dirty = false;
    
    // constructor - the supplied parameter is an
    // object whose proxy we would like to create     
    public ObjectProxy(Object obj, OrientElement e, SessionManager sm) {
        this.___baseClass = obj.getClass();
        this.___baseElement = e;
        this.___sm = sm;
    }
    
    public ObjectProxy(Class c, OrientElement e, SessionManager sm) {
        this.___baseClass = c;
        this.___baseElement = e;
        this.___sm = sm;
    }
    
    // ByteBuddy inteceptor
    // this method will be called each time      
    // when the object proxy calls any of its methods
    @RuntimeType
    public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method) throws Exception {
    
        // response object
        Object res = null;
    
        // BEFORE
        // measure the current time         
        // long time1 = System.currentTimeMillis();
        
        // LOGGER.log(Level.FINER, "method intercepted: "+method.getName());
        // modificar el llamado
        switch (method.getName()) {
            case "___getVertex":
                res = this.___getVertex();
                break;
            case "___getRid":
                res = this.___getRid();
                break;
            case "___getProxiObject":
                res = this.___getProxiObject();
                break;
            case "___getBaseClass":
                res = this.___getBaseClass();
                break;
            case "___isDirty":
                res = this.___isDirty();
                break;
            case "___removeDirtyMark":
                this.___removeDirtyMark();
                break;
            case "___commit":
                this.___commit();
                break;
            default:
                // invoke the method on the real object with the given params
                res = zuper.call();
                // verificar si hay diferencias entre los objetos.
                this.commitObjectChange();
                
                break;
        }

        // AFTER
        // print how long it took to execute the method on the proxified object
        // System.out.println("Took: " + (System.currentTimeMillis() - time1) + " ms");
        // return the result         
        return res;
    }

    
    
    // GCLib interceptor 
    @Override
    public Object intercept(Object o,
            Method method,
            Object[] args,
            MethodProxy methodProxy) throws Throwable {
        // response object
        Object res = null;

        // BEFORE
        // measure the current time         
//        long time1 = System.currentTimeMillis();
//        System.out.println("intercepted: " + method.getName());
        // modificar el llamado
        switch (method.getName()) {
            case "___getVertex":
                res = this.___getVertex();
                break;
            case "___getRid":
                res = this.___getRid();
                break;
            case "___getProxiObject":
                res = this.___getProxiObject();
                break;
            case "___getBaseClass":
                res = this.___getBaseClass();
                break;
            case "___isDirty":
                res = this.___isDirty();
                break;
            case "___removeDirtyMark":
                this.___removeDirtyMark();
                break;
            case "___commit":
                this.___commit();
                break;
            default:
                // invoke the method on the real object with the given params
//                res = methodProxy.invoke(realObj, args);
                res = methodProxy.invokeSuper(o, args);

                // verificar si hay diferencias entre los objetos.
                this.commitObjectChange();

                break;
        }

        // AFTER
        // print how long it took to execute the method on the proxified object
//        System.out.println("Took: " + (System.currentTimeMillis() - time1) + " ms");
        // return the result         
        return res;
    }
    
    
    
    public void ___setProxyObject(Object po) {
        this.___proxyObject = po;
    }
    
    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista uno.
     *
     * @return
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
     * retorna el vértice asociado a este proxi o null en caso que no exista uno.
     *
     * @return
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
     * @param v
     */
    @Override
    public void ___setVertex(OrientVertex v) {
        this.___baseElement = v;
    }

    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista uno.
     *
     * @return
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
     * @param v
     */
    @Override
    public void ___setEdge(OrientEdge e) {
        this.___baseElement = e;
    }

    @Override
    public Object ___getProxiObject() {
        return this.___proxyObject;
    }
    
    @Override
    public Class<?> ___getBaseClass() {
        return this.___baseClass;
    }

    private void commitObjectChange() {
        this.___sm.getGraphdb().getRawGraph().activateOnCurrentThread();
        
        LOGGER.log(Level.FINER, "iniciando commit interno....");
        // si ya estaba marcado como dirty no volver a procesarlo.
        if (!___dirty) {
            // FIXME: debería pasar este map como propiedad para optimizar la velocidad?
            HashMap<String, Object> vmap = new HashMap<>();
            this.___baseElement.getPropertyKeys().stream().forEach((prop) -> {
                // LOGGER.log(Level.FINER, "VERTEX PROP: {0} <-----------------------------------------------",new String[]{prop});
                Object vvalue = this.___baseElement.getProperty(prop);
                vmap.put(prop, vvalue);
            });
            
            // obtener la definición de la clase
            LOGGER.log(Level.FINER, "**********************************");
            ClassDef cDef = this.___sm.getObjectMapper().getClassDef(this.___proxyObject);
            LOGGER.log(Level.FINER, "**********************************");

            // obtener un mapa actualizado del objeto contenido
            ObjectStruct oStruct = this.___sm.getObjectMapper().objectStruct(this.___proxyObject);
            Map<String, Object> omap = oStruct.fields;

            // si los mapas no son iguales, entonces eso implica que el objeto cambió
            if (!vmap.equals(omap)) {
                // transferir el bojeto al vértice en cuestión
                LOGGER.log(Level.FINER, "cambio detectado: " + this.___baseElement.getId());
                LOGGER.log(Level.FINER, "vmap:" + vmap);
                LOGGER.log(Level.FINER, "-------------------------------------------");
                LOGGER.log(Level.FINER, "omap:" + omap);
                
                // this.baseVertex.setProperties(omap);
                this.___dirty = true;

            } else // si no se trata de un Edge
             if (this.___baseElement.getElementType().equals("Vertex")) {

                    // si no hay diferencia a nivel de campo, puede existir diferencia 
                    // en los links. Analizarlos para ver si corresponde marcar el objeto 
                    // como dirty
                    // Analizar si cambiaron los vértices
                    /* 
                       procesar los objetos internos. Primero se deber determinar
                       si los objetos ya existían en el contexto actual. Si no existen
                       deben ser creados.
                     */
                    OrientVertex ov = (OrientVertex) this.___baseElement;

                    for (Map.Entry<String, Class<?>> link : cDef.links.entrySet()) {
                        String field = link.getKey();
                        String graphRelationName = this.___baseClass.getSimpleName() + "_" + field;
                        Class<?> fclass = link.getValue();

                        // determinar el estado del campo
                        if (oStruct.links.get(field) == null) {
                            // si está en null, es posible que se haya eliminado el objeto
                            // por lo cual se debería eliminar el vértice correspondiente
                            // si es que existe
                            if (ov.countEdges(Direction.OUT, graphRelationName) > 0) {
                                // se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente
                                // marcar el objeto como dirty
                                this.___dirty = true;
                                LOGGER.log(Level.FINER, "Dirty: se ha eliminado un link");
                            }
                        } else {
                            Object innerO = oStruct.links.get(field);
                            // verificar si ya está en el contexto. Si fue creado en forma 
                            // separada y asociado con el objeto principal, se puede dar el caso
                            // de que el objeto principal tiene RID y el agregado no.
                            if (((innerO instanceof IObjectProxy)
                                    && (ov.countEdges(Direction.OUT, graphRelationName) == 0))
                                    || (!(innerO instanceof IObjectProxy))) {
                                // si el objeto existía y no existía el eje
                                // o bien no existía el objeto
                                this.___dirty = true;
                                LOGGER.log(Level.FINER, "Dirty: se agregó un link");
                            }
                        }
                    }

                    // si no se han encontrado modificaciones aún, revisar los linklists
                    if (!this.___isDirty()) {

                        /**
                         * Procesar los linklists.
                         */
                        Field f;
                        for (Map.Entry<String, Class<?>> entry : cDef.linkLists.entrySet()) {
                            try {
                                String field = entry.getKey();
                                final String graphRelationName = this.___baseClass.getSimpleName() + "_" + field;
                                Class<? extends Object> fieldClass = entry.getValue();

                                f = ReflectionUtils.findField(this.___proxyObject.getClass(), field);
                                boolean acc = f.isAccessible();
                                f.setAccessible(true);

                                LOGGER.log(Level.FINER, "procesando campo: " + field);

                                // determinar el estado del campo
                                if (oStruct.linkLists.get(field) == null) {
                                    // si está en null, es posible que se haya eliminado el objeto
                                    // por lo cual se debería eliminar el vértice correspondiente
                                    // si es que existe
                                    if (ov.countEdges(Direction.OUT, graphRelationName) > 0) {
                                        // se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente
                                        // marcar el objeto como dirty
                                        this.___dirty = true;
                                        LOGGER.log(Level.FINER, "Dirty: se ha eliminado un linklist");
                                    }
                                } else {
                                    Object innerO = oStruct.linkLists.get(field);
                                    // verificar si ya está en el contexto. Si fue creado en forma 
                                    // separada y asociado con el objeto principal, se puede dar el caso
                                    // de que el objeto principal tiene RID y el agregado no.
                                    if (((innerO instanceof IObjectProxy)
                                            && (ov.countEdges(Direction.OUT, graphRelationName) == 0))
                                            || (!(innerO instanceof IObjectProxy))) {
                                        // si el objeto existía y no existía el eje
                                        // o bien no existía el objeto
                                        this.___dirty = true;
                                        LOGGER.log(Level.FINER, "Dirty: se ha agregado un linklist");
                                    }
                                }

                                f.setAccessible(acc);
                            } catch (NoSuchFieldException | IllegalArgumentException ex) {
                                Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }
                    }
                }
            if (this.___dirty) {
                // agregarlo a la lista de dirty para procesarlo luego
                LOGGER.log(Level.FINER, "Dirty: "+this.___proxyObject);
                this.___sm.setAsDirty(this.___proxyObject);
                LOGGER.log(Level.FINER, "Objeto marcado como dirty! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            }
        }
    }

    @Override
    public boolean ___isDirty() {
        return ___dirty;
    }

    @Override
    public void ___removeDirtyMark() {
        this.___dirty = false;
    }

    @Override
    public void ___commit() {
//        ODatabaseRecordThreadLocal.INSTANCE.set(this.___sm.getGraphdb().getRawGraph());
//        this.___sm.getGraphdb().getRawGraph().activateOnCurrentThread();
        
        if (this.___dirty) {
            // obtener la definición de la clase
            ClassDef cDef = this.___sm.getObjectMapper().getClassDef(this.___proxyObject);
            
            // obtener un mapa actualizado del objeto contenido
            ObjectStruct oStruct = this.___sm.getObjectMapper().objectStruct(this.___proxyObject);
            Map<String, Object> omap = oStruct.fields;

            // bajar todo al vértice
            this.___baseElement.setProperties(omap);

            // si se trata de un Vértice
            if (this.___baseElement.getElementType().equals("Vertex")) {
                OrientVertex ov = (OrientVertex) this.___baseElement;
                // Analizar si cambiaron los vértices
                /* 
                   procesar los objetos internos. Primero se deber determinar
                   si los objetos ya existían en el contexto actual. Si no existen
                   deben ser creados.
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
                            }

                            try {
                                // f = ReflectionUtils.findField(this.realObj.getClass(), field);
                                f = ReflectionUtils.findField(this.___baseClass, field);
                                boolean acc = f.isAccessible();
                                f.setAccessible(true);

                                if (f.isAnnotationPresent(RemoveOrphan.class
                                )) {

                                    // eliminar el objecto
                                    // this.sm.delete(f.get(realObj));
                                    this.___sm.delete(f.get(this));

                                } else {
                                    // solo eliminar el Edge y mantener el objeto
                                    removeEdge.remove();
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
                    } else {
                        Object innerO = oStruct.links.get(field);
                        // verificar si ya está en el contexto. Si fue creado en forma 
                        // separada y asociado con el objeto principal, se puede dar el caso
                        // de que el objeto principal tiene RID y el agregado no.
                        if (innerO instanceof IObjectProxy) {
                            // el objeto existía.
                            // se debe verificar si el eje entre los dos objetos ya existía.
                            if (ov.countEdges(Direction.OUT, graphRelationName) == 0) {
                                // No existe un eje. Se debe crear
                                LOGGER.log(Level.FINER, "Agregar un link entre dos objetos existentes.");
                                LOGGER.log(Level.FINER, ""+ov.getId().toString()+" --> "+((IObjectProxy) innerO).___getVertex().getId().toString());
                                this.___sm.getGraphdb().addEdge("", ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                            }
                        } else {
                            LOGGER.log(Level.FINER, "innerO nuevo. Crear un vértice y un link");
                            innerO = this.___sm.store(innerO);
//                            this.sm.getObjectMapper().setFieldValue(realObj, field, innerO);
                            this.___sm.getObjectMapper().setFieldValue(this.___proxyObject, field, innerO);
                            this.___sm.getGraphdb().addEdge("", ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
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
                        final String graphRelationName = this.___baseClass.getSimpleName() + "_" + field;
                        Class<? extends Object> fieldClass = entry.getValue();

                        // f = ReflectionUtils.findField(this.realObj.getClass(), field);
                        LOGGER.log(Level.FINER, "procesando campo: " + field + " clase: "+this.___proxyObject.getClass());

                        f = ReflectionUtils.findField(this.___proxyObject.getClass(), field);
                        boolean acc = f.isAccessible();
                        f.setAccessible(true);


                        // Object oCol = f.get(this.realObj);
                        Object oCol = f.get(this.___proxyObject);

                        // verificar si existe algún cambio en la coleccióne
                        // ingresa si la colección es distinta de null y
                        // oCol es instancia de ILazyCalls y está marcado como dirty
                        // o oCol no es instancia de ILazyCalls, lo que significa que es una colección nueva
                        // y debe ser procesada completamente.
                        if ((oCol != null)
                                && ((ILazyCalls.class.isAssignableFrom(oCol.getClass()) && ((ILazyCalls) oCol).isDirty())
                                    || (!ILazyCalls.class.isAssignableFrom(oCol.getClass())))) {
                            
                            if (oCol instanceof List) {
                                ILazyCollectionCalls col;
                                // procesar la colección

                                if (ILazyCollectionCalls.class
                                        .isAssignableFrom(oCol.getClass())) {
                                    col = (ILazyCollectionCalls) oCol;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    // this.sm.getObjectMapper().colecctionToLazy(this.realObj, field, ov);
                                    this.___sm.getObjectMapper().colecctionToLazy(this.___proxyObject, field, ov);
                                    
                                    //recuperar la nueva colección
                                    // Collection inter = (Collection) f.get(this.realObj);
                                    Collection inter = (Collection) f.get(this.___proxyObject);
                                    
                                    //agregar todos los valores que existían
                                    inter.addAll((Collection) oCol);
                                    //preparar la interface para que se continúe con el acceso.
                                    col = (ILazyCollectionCalls) inter;
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
                                            // no es un objeto que se haya almacenado.
                                            colObject = this.___sm.store(colObject);
                                            // reemplazar en la colección el objeto por uno administrado
                                            lCol.set(i, colObject);
                                        }

                                        // vincular el nodo
                                        this.___sm.getGraphdb().addEdge("", this.___getVertex(), ((IObjectProxy) colObject).___getVertex(), graphRelationName);
                                    }
                                }
                                // procesar los removidos solo si está el anotation en el campo

                                for (Map.Entry<Object, ObjectCollectionState> entry1 : colState.entrySet()) {
                                    Object colObject = entry1.getKey();
                                    ObjectCollectionState colObjState = entry1.getValue();

                                    if (colObjState == ObjectCollectionState.REMOVED) {
                                        if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                            this.___sm.delete(colObject);
                                        } else {
                                            // remover solo el link

                                            for (Edge edge : ((OrientVertex) this.___baseElement).getEdges(((IObjectProxy) colObject).___getVertex(), Direction.OUT, graphRelationName)) {
                                                edge.remove();
                                            }
                                        }
                                    }
                                }

                                // resetear el estado
                                col.clearState();

                            } else if (oCol instanceof Map) {

                                HashMap innerMap;
                                // procesar la colección

                                if (ILazyMapCalls.class
                                        .isAssignableFrom(oCol.getClass())) {
                                    innerMap = (HashMap) oCol;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    // this.sm.getObjectMapper().colecctionToLazy(this.realObj, field, ov);
                                    this.___sm.getObjectMapper().colecctionToLazy(this.___proxyObject, field, ov);
                                    //recuperar la nueva colección
                                    // Collection inter = (Collection) f.get(this.realObj);
                                    Collection inter = (Collection) f.get(this.___proxyObject);
                                    //agregar todos los valores que existían
                                    inter.addAll((Collection) oCol);
                                    //preparar la interface para que se continúe con el acceso.
                                    innerMap = (HashMap) inter;
                                }

                                //                            final String ffield = field;
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
                                        linkedO = this.___sm.store(linkedO);
                                        innerMap.replace(imk, linkedO);
                                    }

                                    // verificar el estado del objeto en la colección.
                                    switch (imV) {
                                        case ADDED:
                                            // crear un link entre los dos objetos.
                                            LOGGER.log(Level.FINER, "-----> agregando un LinkList al Map!");
                                            //                                        oe = SessionManager.this.graphdb.addEdge("", fVertexs.get(frid), fVertexs.get(llRID), ffield);
                                            oe = this.___sm.getGraphdb().addEdge("", (OrientVertex) this.___baseElement, ((IObjectProxy) linkedO).___getVertex(), graphRelationName);
                                            // actualizar el edge con los datos de la key.

                                            oe.setProperties(this.___sm.getObjectMapper().simpleMap(imk));
                                            break;

                                        case NOCHANGE:
                                            // el link no se ha modificado. 
                                            break;

                                        case REMOVED:
                                            // el link se ha removido. Se debe eliminar y verificar si corresponde borrar 
                                            // el vértice en caso de estar marcado con @RemoveOrphan.
                                            if (f.isAnnotationPresent(RemoveOrphan.class
                                            )) {
                                                if (entitiesState.get(imk) == ObjectCollectionState.REMOVED) {
                                                    this.___sm.delete(entitiesState.get(imk));
                                                }
                                            }
                                            // quitar el Edge
                                            OrientEdge oeRemove = keyToEdge.get(imk);
                                            oeRemove.remove();

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
            this.___dirty = false;
        }
    }

    @Override
    public void ___rollback() {

    }

}
