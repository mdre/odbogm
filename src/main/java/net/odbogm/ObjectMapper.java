package net.odbogm;

import com.orientechnologies.orient.core.metadata.sequence.OSequenceLibrary;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.odbogm.annotations.Indirect;
import net.odbogm.cache.ClassCache;
import net.odbogm.cache.ClassDef;
import net.odbogm.exceptions.CollectionNotSupported;
import net.odbogm.proxy.ArrayListEmbeddedProxy;
import net.odbogm.proxy.HashMapEmbeddedProxy;
import net.odbogm.proxy.ILazyCollectionCalls;
import net.odbogm.proxy.ILazyMapCalls;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.proxy.ObjectProxyFactory;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectMapper {

    private final static Logger LOGGER = Logger.getLogger(ObjectMapper.class.getName());

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ObjectMapper);
        }
    }
    private final ClassCache classCache;

    
    public ObjectMapper() {
        // inicializar el caché de clases
        classCache = new ClassCache();
    }

    /**
     * Devuelve la definición de la clase para el objeto pasado por parámetro
     *
     * @param o objeto de referencia
     * @return definición de la clase
     */
    public ClassDef getClassDef(Object o) {
        if (o instanceof IObjectProxy) {
            return classCache.get(((IObjectProxy) o).___getBaseClass());
        } else {
            return classCache.get(o.getClass());
        }
    }
    
    /**
     * Devuelve la definición de la clase dada.
     * 
     * @param cls Clase dada.
     * @return La definición de la clase, si todavía no existe la crea.
     */
    public ClassDef getClassDef(Class cls) {
        return classCache.get(cls);
    }
    
    /**
     * Devuelve un mapeo rápido del Objeto. No procesa los link o linklist.
     * Simplemente devuelve todos los atributos del objeto en un map (para
     * mapear propiedades de una arista).
     *
     * @param o objeto a analizar.
     * @return un mapa con los campos y las clases que representa.
     */
    public Map<String, Object> simpleMap(Object o) {
        HashMap<String, Object> data = new HashMap<>();
        if (Primitives.PRIMITIVE_MAP.containsKey(o.getClass())) {
            data.put("key", o);
        } else {
            ClassDef classmap = classCache.get(o.getClass());
            simpleFastMap(o, classmap, data);
        }
        return data;
    }

    private void simpleFastMap(Object o, ClassDef classmap, HashMap<String, Object> data) {
        //campos básicos y enums
        classmap.fields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            putValue(data, f, o, null);
        });
        classmap.enumFields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            putValue(data, f, o, v -> ((Enum)v).name());
        });
    }
    
    /**
     * Fills the sequence fields with the values of the corresponding sequences
     * from the DB.
     * 
     * @param o
     * @param t
     * @param v If not null, sets also its corresponding property.
     */
    public void fillSequenceFields(Object o, Transaction t, OVertex v) {
        ClassDef classdef = this.classCache.get(o.getClass());
        if (!classdef.sequenceFields.isEmpty()) {
            OSequenceLibrary seqLibrary = t.getCurrentGraphDb().getMetadata().getSequenceLibrary();
            classdef.sequenceFields.entrySet().forEach(e -> {
                Field f = classdef.fieldsObject.get(e.getKey());
                if (this.getFieldValue(o, f) == null) {
                    Long seqVal = seqLibrary.getSequence(e.getValue()).next();
                    this.setFieldValue(o, f, seqVal);
                    if (v != null) v.setProperty(e.getKey(), seqVal);
                }
            });
        }
    }
    
    //============================================================================================

    /**
     * Devuelve un Map con todos los K,V de cada campo del objeto.
     *
     * @param o objeto a analizar
     * @return un objeto con la estructura del objeto analizado.
     */
    public ObjectStruct objectStruct(Object o) {
        // buscar la definición de la clase en el caché
        ClassDef classmap;
        if (o instanceof IObjectProxy) {
            LOGGER.log(Level.FINEST, "Proxy instance. Seaching the orignal class... ({0})",
                    o.getClass().getSuperclass().getSimpleName());
            classmap = classCache.get(o.getClass().getSuperclass());
        } else {
            LOGGER.log(Level.FINEST, "Searching the class... ({0})",
                    o.getClass().getSimpleName());
            classmap = classCache.get(o.getClass());
        }
        
        ObjectStruct oStruct = new ObjectStruct();
        this.fastmap(o, classmap, oStruct);
        return oStruct;
    }

    /**
     * Realiza un mapeo a partir de las definiciones existentes en el caché.
     *
     * @param o objeto a analizar
     * @param oStruct objeto de referencia a completar
     */
    private void fastmap(Object o, ClassDef classmap, ObjectStruct oStruct) {
        // procesar todos los campos
        classmap.fields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            boolean put = putValue(oStruct.fields, f, o, null);
            if (!put) oStruct.removedProperties.add(entry.getKey());
        });

        // procesar todos los Enums
        classmap.enumFields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            boolean put = putValue(oStruct.fields, f, o, v -> ((Enum)v).name());
            if (!put) oStruct.removedProperties.add(entry.getKey());
        });
        
        // procesar todas las colecciones de Enums
        classmap.enumCollectionFields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            //se convierte la colección de enums a colección de strings con el name
            boolean put = putValue(oStruct.fields, f, o, v -> ((Collection)v).stream().
                    map(e -> ((Enum)e).name()).collect(Collectors.toList()));
            if (!put) oStruct.removedProperties.add(entry.getKey());
        });
        
        // procesar todos los links
        classmap.links.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            putValue(oStruct.links, f, o, null);
        });
        
        // procesar todos los linksList
        classmap.linkLists.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            putValue(oStruct.linkLists, f, o, null);
        });
    }
    
    /**
     * Saves in the attributes map 'valuesMap' the value that the object 'o'
     * has in its attribute given by the field 'f', applying the transform if
     * given. Returns false if the value is null.
     */
    private boolean putValue(Map valuesMap, Field f, Object o, Function transform) {
        try {
            Object value = f.get(o);
            LOGGER.log(Level.FINER, "Field: {0}. Class: {1}. Value: {2}",
                    new Object[]{f.getName(), f.getType().getSimpleName(), value});
            if (value != null) {
                valuesMap.put(f.getName(), transform != null ? transform.apply(value) : value);
            }
            return value != null;
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, "Couldn't get attribute value", ex);
            return true;
        }
    }
    
    /**
     * Crea y llena un objeto con los valores correspondintes obtenidos del Vértice asignado.
     *
     * @param <T> clase a devolver
     * @param c clase de referencia
     * @param v vértice de referencia
     * @param t Vínculo a la transacción actual
     * @return un objeto de la clase T
     * @throws InstantiationException cuando no se puede instanciar
     * @throws IllegalAccessException cuando no se puede acceder
     * @throws NoSuchFieldException no existe el campo.
     */
    public <T> T hydrate(Class<T> c, OVertex v, Transaction t) throws InstantiationException, IllegalAccessException, NoSuchFieldException, CollectionNotSupported {
        t.initInternalTx();
        LOGGER.log(Level.FINER, "class: {0}  vertex: {1}", new Object[]{c, v});
        
        Class<?> toHydrate = c;
        String entityClass = ClassCache.getEntityName(toHydrate);
//        String vertexClass = (v.getType().getName().equals("V") ? entityClass : v.getType().getName());
        String vertexClass = (v.getSchemaType().isPresent() & v.getSchemaType().get().getName().equals("V") ? 
                                entityClass : 
                                v.getSchemaType().get().getName());

        // validar que el Vertex sea instancia de la clase solicitada
        // o que la clase solicitada sea su superclass
        if (!entityClass.equals(vertexClass)) {
            LOGGER.log(Level.FINER, "Tipos distintos. {0} <> {1}", new Object[]{entityClass, vertexClass});
            String javaClass = v.getSchemaType().get().getCustom("javaClass");
            if (javaClass != null) {
                try {
                    // validar que sea un super de la clase del vértice
                    javaClass = javaClass.replaceAll("[\'\"]", "");
                    toHydrate = Class.forName(javaClass);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                throw new InstantiationException("ERROR de Instanciación! \n"
                        + "El vértice no coincide con la clase que se está intentando instanciar\n"
                        + "y no tiene definido la propiedad javaClass.");
            }
        }

        // crear un proxy sobre el objeto y devolverlo
        IObjectProxy proxy = (IObjectProxy)ObjectProxyFactory.create(toHydrate, v, t);

        LOGGER.log(Level.FINER, "**************************************************");
        LOGGER.log(Level.FINER, "Hydratando: {0} - Class: {1}", new Object[]{c.getName(), toHydrate});
        LOGGER.log(Level.FINER, "**************************************************");
        
        // recuperar la definición de la clase desde el caché
        ClassDef classdef = classCache.get(toHydrate);
        
        
        // ********************************************************************************************
        // procesar los atributos básicos
        // ********************************************************************************************
        Field f;
        for (var entry : classdef.fields.entrySet()) {
            String prop = entry.getKey();
            if (!classdef.embeddedFields.containsKey(prop)) {
                LOGGER.log(Level.FINER, "Buscando campo {0} de tipo {1}...",
                        new String[]{prop, entry.getValue().getSimpleName()});
                this.setFieldValue(proxy, prop, v.getProperty(prop));
            }
        }
        
        //version field
        proxy.___uptadeVersion();
        
        // insertar el objeto en el transactionLoopCache
        t.transactionLoopCache.put(v.getIdentity().toString(), proxy);
        
        
        // ********************************************************************************************
        // process embedded collections
        // ********************************************************************************************
        hydrateEmbeddedCollections(classdef, proxy, v);

        
        // ********************************************************************************************
        // procesar los enum
        // ********************************************************************************************
        for (Map.Entry<String, Class<?>> entry : classdef.enumFields.entrySet()) {
            String prop = entry.getKey();
            LOGGER.log(Level.FINER, "Buscando campo {0} ....", new String[]{prop});
            f = classdef.fieldsObject.get(prop);
            Object value = v.getProperty(prop);
            LOGGER.log(Level.FINER, "Enum field: {0} type: {1} value: {2}",
                    new Object[]{f.getName(), f.getType(), value});
            setEnumField(proxy, f, value);
            LOGGER.log(Level.FINER, "hidratado campo: {0}={1}", new Object[]{prop, value});
        }

        
        // ********************************************************************************************
        // procesar colecciones de enums
        // ********************************************************************************************
        hydrateEnumCollections(classdef, proxy, v);

        
        // ********************************************************************************************
        // hidratar las colecciones
        // procesar todos los linkslist
        // ********************************************************************************************
        LOGGER.log(Level.FINER, "preparando las colecciones...");
        hydrateLinkCollections(t, classdef, proxy, v, false);
        
        
        // ********************************************************************************************
        // hidratar las colecciones indirectas
        // procesar todos los indirectLinkslist
        // ********************************************************************************************
        LOGGER.log(Level.FINER, "hidratar las colecciones indirectas...");
        hydrateLinkCollections(t, classdef, proxy, v, true);
        
        
        LOGGER.log(Level.FINER, "******************* FIN HYDRATE *******************");
        t.closeInternalTx();
        return (T) proxy;
    }
    
    
    /**
     * Given a vertex from the base (or edge), hydrate the embedded collections of
     * the given proxy accordingly to the values of the vertex.
     * 
     * @param classdef Class definition of the object to hydrate.
     * @param proxy The proxy.
     * @param v An OrientDB element (vertex or edge).
     */
    public void hydrateEmbeddedCollections(ClassDef classdef, IObjectProxy proxy, OElement v) {
        Field f;
        for (Map.Entry<String, Class<?>> entry : classdef.embeddedFields.entrySet()) {
            String prop = entry.getKey();
            Object vertexValue = v.getProperty(prop);
            f = classdef.fieldsObject.get(prop);
            if (vertexValue != null) {
                collectionToEmbedded(proxy, f, vertexValue);
            } else {
                /* if the node doesn't have a value, respect the initial value of the attribute
                 * in the class:
                 * null -> null
                 * empty collection -> empty proxy collection
                 */
                if (getFieldValue(proxy, f) != null) {
                    collectionToEmbedded(proxy, f);
                } else {
                    setFieldValue(proxy, prop, null);
                }
            }
        }
    }
    
    /**
     * Given a vertex from the base (or edge), hydrate the enums collections of the given
     * proxy accordingly to the values of the vertex.
     * 
     * @param classdef Class definition of the object to hydrate.
     * @param proxy The proxy.
     * @param v An OrientDB element (vertex or edge).
     */
    public void hydrateEnumCollections(ClassDef classdef, IObjectProxy proxy, OElement v) {
        Field f;
        for (Map.Entry<String, Class<?>> entry : classdef.enumCollectionFields.entrySet()) {
            String prop = entry.getKey();
            List vertexValue = (List)v.getProperty(prop);
            f = classdef.fieldsObject.get(prop);
            if (vertexValue != null) {
                //replace all values by the correspoinding enum:
                Class<?> listClass = getListType(f);
                List enumList = new ArrayList(vertexValue.size());
                for (int i = 0; i < vertexValue.size(); ++i) {
                    if (vertexValue.get(i) instanceof String) {
                        //only convert to enum if the value in the list is a String
                        String sVal = (String)vertexValue.get(i);
                        enumList.add(Enum.valueOf(listClass.asSubclass(Enum.class), sVal));
                    }
                }
                setFieldValue(proxy, prop, new ArrayListEmbeddedProxy(proxy, enumList));
            } else {
                /* if the node doesn't have a value, respect the initial value of the attribute
                 * in the class:
                 * null -> null
                 * empty list -> empty proxy list
                 */
                if (getFieldValue(proxy, f) != null) {
                    setFieldValue(proxy, f, new ArrayListEmbeddedProxy(proxy, new ArrayList()));
                } else {
                    setFieldValue(proxy, prop, null);
                }
            }
        }
    }
    
    
    private void hydrateLinkCollections(Transaction t, ClassDef classdef, IObjectProxy proxy, OVertex v, boolean indirect) {
        Map<String, Class<?>> links;
        ODirection relationDirection;
        if (indirect) {
            links = classdef.indirectLinkLists;
            relationDirection = ODirection.IN;
        } else {
            links = classdef.linkLists;
            relationDirection = ODirection.OUT;
            
        }
        Field fLink;
        for (Map.Entry<String, Class<?>> entry : links.entrySet()) {
            try {
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
                fLink = classdef.fieldsObject.get(field);
                
                String graphRelationName;
                if (indirect) {
                    Indirect in = fLink.getAnnotation(Indirect.class);
                    graphRelationName = in.linkName();
                } else {
                    graphRelationName = classdef.entityName + "_" + field;
                }

                // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
                if ((v.getEdges(relationDirection, graphRelationName).iterator().hasNext()) || (fLink.get(proxy) != null)) {
                    this.collectionToLazy(proxy, field, fc, v, t);
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    
    public void collectionToLazy(Object o, String field, OVertex v, Transaction t) {
        LOGGER.log(Level.FINER, "convertir colection a Lazy: {0}", field);
        ClassDef classdef;
        if (o instanceof IObjectProxy) {
            classdef = classCache.get(o.getClass().getSuperclass());
        } else {
            classdef = classCache.get(o.getClass());
        }

        Class<?> fc = classdef.linkLists.get(field);
        collectionToLazy(o, field, fc, v, t);
    }
    
    
    /**
     * Dado un Field correspondiente a una lista, devuelve la clase de los
     * elementos que contiene la lista.
     */
    private Class<?> getListType(Field listField) {
        ParameterizedType listType = (ParameterizedType) listField.getGenericType();
        return (Class<?>) listType.getActualTypeArguments()[0];
    }
    
    /**
     * Convierte una colección común en una Lazy para futuras operaciones.
     *
     *
     * @param o objeto base sobre el que se trabaja
     * @param field campo a modificar
     * @param fc clase original del campo
     * @param v vértice con el cual se conecta.
     * @param t Vínculo a la transacción actual
     *
     */
    private void collectionToLazy(Object o, String field, Class<?> fc, OVertex v, Transaction t) {
        LOGGER.log(Level.FINER, "***************************************************************");
        LOGGER.log(Level.FINER, "convertir colection a Lazy: " + field + " class: " + fc.getName());
        LOGGER.log(Level.FINER, "***************************************************************");
        try {
            Class<?> c;
            if (o instanceof IObjectProxy) {
                c = o.getClass().getSuperclass();
            } else {
                c = o.getClass();
            }
            
            ClassDef classdef = classCache.get(c);
            Field fLink = classdef.fieldsObject.get(field);

            String graphRelationName = classdef.entityName + "_" + field;
            // Determinar la dirección
            ODirection direction = ODirection.OUT;

            if (fLink.isAnnotationPresent(Indirect.class)) {
                // si es un indirect se debe reemplazar el nombre de la relación por 
                // el propuesto por la anotation
                Indirect in = fLink.getAnnotation(Indirect.class);
                graphRelationName = in.linkName();
                direction = ODirection.IN;
            }

            Class<?> lazyClass = Primitives.LAZY_COLLECTION.get(fc);
            LOGGER.log(Level.FINER, "lazyClass: {0}", lazyClass.getName());
            Object col = lazyClass.newInstance();
            // dependiendo de si la clase hereda de Map o List, inicalizar
            if (col instanceof List) {
                Class<?> listClass = getListType(fLink);
                // inicializar la colección
                ((ILazyCollectionCalls) col).init(t, v, (IObjectProxy) o,
                        graphRelationName, listClass, direction);

            } else if (col instanceof Map) {
                ParameterizedType listType = (ParameterizedType) fLink.getGenericType();
                Class<?> keyClass = (Class<?>) listType.getActualTypeArguments()[0];
                Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[1];
                // inicializar la colección
                ((ILazyMapCalls) col).init(t, v, (IObjectProxy) o,
                        graphRelationName, keyClass, valClass, direction);
            } else {
                throw new CollectionNotSupported();
            }

            fLink.set(o, col);

        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InstantiationException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Convierte todas las colecciones identificadas como embedded en el ClassDef a sus correspondientes proxies
     *
     * @param o the object to be analyzed
     * @param classDef the class struct
     */
    public void collectionsToEmbedded(Object o, ClassDef classDef) {
        classDef.embeddedFields.entrySet().forEach(entry ->
                collectionToEmbedded(o, classDef, entry.getKey()));
        classDef.enumCollectionFields.entrySet().forEach(entry ->
                collectionToEmbedded(o, classDef, entry.getKey()));
    }
    
    /**
     * Converts the value of a field into an embedded proxy collection if of correct type.
     */
    private void collectionToEmbedded(Object o, ClassDef classDef, String field) {
        Field f = classDef.fieldsObject.get(field);
        try {
            collectionToEmbedded(o, f, f.get(o));
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, "Error converting collections to embedded", ex);
        }
    }
    
    /**
     * Converts the value of a field into an embedded proxy collection if of correct type.
     */
    private void collectionToEmbedded(Object o, Field f, Object value) {
        try {
            LOGGER.log(Level.FINER, "Procesando campo: {0} type: {1}", new String[]{f.getName(), f.getType().getName()});
            // realizar la conversión solo si el campo tiene un valor.
            if (value != null && List.class.isAssignableFrom(f.getType())) {
                LOGGER.log(Level.FINER, "convirtiendo en ArrayListEmbeddedProxy...");
                f.set(o, new ArrayListEmbeddedProxy((IObjectProxy)o, (List)value));
            } else if (value != null && Map.class.isAssignableFrom(f.getType())) {
                LOGGER.log(Level.FINER, "convirtiendo en HashMapEmbeddedProxy");
                f.set(o, new HashMapEmbeddedProxy((IObjectProxy)o, (Map)value));
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, "Error converting collections to embedded", ex);
        }
    }
    
    /**
     * Sets the value of a field into an empty embedded proxy collection.
     */
    private void collectionToEmbedded(Object o, Field f) {
        try {
            LOGGER.log(Level.FINER, "Processing field: {0} type: {1}", new String[]{f.getName(), f.getType().getName()});
            // realizar la conversión solo si el campo tiene un valor.
            if (List.class.isAssignableFrom(f.getType())) {
                LOGGER.log(Level.FINER, "converting into empty ArrayListEmbeddedProxy...");
                f.set(o, new ArrayListEmbeddedProxy((IObjectProxy)o, new ArrayList()));
            } else if (Map.class.isAssignableFrom(f.getType())) {
                LOGGER.log(Level.FINER, "converting into empty HashMapEmbeddedProxy...");
                f.set(o, new HashMapEmbeddedProxy((IObjectProxy)o, new HashMap()));
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, "Error converting collections to embedded", ex);
        }
    }

    /**
     * Hidrata un objeto a partir de los atributos guardados en un Edge
     *
     * @param <T> clase del objeto a devolver
     * @param c : clase del objeto a devolver
     * @param e : Edge desde el que recuperar los datos
     * @param t Vínculo a la transacción actual
     * @return objeto completado a partir de la base de datos
     * @throws InstantiationException si no se puede instanciar.
     * @throws IllegalAccessException si no se puede acceder
     * @throws NoSuchFieldException si no se encuentra alguno de los campos.
     */
    public <T> T hydrate(Class<T> c, OEdge e, Transaction t) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        T oproxied = ObjectProxyFactory.create(c, e, t);
        ClassDef classdef = classCache.get(c);
        Field f;
        for (String prop : e.getPropertyNames()) {
            Object value = e.getProperty(prop);
            if (value != null) {
                f = classdef.fieldsObject.get(prop);
                // obtener la clase a la que pertenece el campo
                // (puede ser un enum en lugar de un atributo básico)
                // puede darse el caso que la base cree un atributo sobre los registros (ej: @rid) 
                // y la clave podría no corresponderse con un campo.
                Class<?> fc = classdef.fields.get(prop);
                if (fc != null) {
                    setFieldValue(oproxied, f, value);
                } else {
                    fc = classdef.enumFields.get(prop);
                    if (fc != null) {
                        setEnumField(oproxied, f, value);
                    }
                }
            }
        }
        return oproxied;
    }

    
    public Object getFieldValue(Object o, Field field) {
        try {
            return field.get(o);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, "Error getting value for " + field, ex);
            return null;
        }
    }
    
    public void setFieldValue(Object o, Field field, Object value) {
        try {
            field.set(o, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, "Error setting value for " + field, ex);
        }
    }
    
    public void setFieldValue(Object o, String field, Object value) {
        Field f = this.classCache.get(o.getClass()).fieldsObject.get(field);
        try {
            f.set(o, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, "Error setting value for " + f, ex);
        }
    }

    public void setEnumField(Object o, Field field, Object value) {
        try {
            if (value != null && value.toString() != null && !value.toString().isBlank()) {
                field.set(o, Enum.valueOf(field.getType().asSubclass(Enum.class), value.toString()));
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, "Error setting enum value for " + field, ex);
        }
    }
    
}
