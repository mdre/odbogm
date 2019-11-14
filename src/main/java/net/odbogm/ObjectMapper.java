package net.odbogm;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
import net.odbogm.exceptions.DuplicateClassDefinition;
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

    // usado para no llamar a Class.forName para clases ya cargadas.
    private HashMap<String, Class> classLoaded = new HashMap<>();

    
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
            putValue(oStruct.fields, f, o, null);
        });

        // procesar todos los Enums
        classmap.enumFields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            putValue(oStruct.fields, f, o, v -> ((Enum)v).name());
        });
        
        // procesar todas las colecciones de Enums
        classmap.enumCollectionFields.entrySet().stream().forEach(entry -> {
            Field f = classmap.fieldsObject.get(entry.getKey());
            //se convierte la colección de enums a colección de strings con el name
            putValue(oStruct.fields, f, o, v -> ((Collection)v).stream().
                    map(e -> ((Enum)e).name()).collect(Collectors.toList()));
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
     * Guarda en el mapa de atributos 'valuesMap' el valor que tiene el objeto 'o'
     * en su atributo dado por el campo 'f', aplicando la transformación dada.
     */
    private void putValue(Map valuesMap, Field f, Object o, Function transform) {
        try {
            f.setAccessible(true);
            Object value = f.get(o);
            //guardar sólo si no es nulo
            if (value != null) {
                LOGGER.log(Level.FINER, "Field: {0}. Class: {1}. Value: {2}",
                        new Object[]{f.getName(), f.getType().getSimpleName(), value});
                valuesMap.put(f.getName(), transform != null ? transform.apply(value) : value);
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
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
    public <T> T hydrate(Class<T> c, OrientVertex v, Transaction t) throws DuplicateClassDefinition, InstantiationException, IllegalAccessException, NoSuchFieldException, CollectionNotSupported {
        t.initInternalTx();
        LOGGER.log(Level.FINER, "class: {0}  vertex: {1}", new Object[]{c, v});

        Class<?> toHydrate = c;
        String entityClass = ClassCache.getEntityName(toHydrate);
        String vertexClass = (v.getType().getName().equals("V") ? entityClass : v.getType().getName());

        // validar que el Vertex sea instancia de la clase solicitada
        // o que la clase solicitada sea su superclass
        if (!entityClass.equals(vertexClass)) {
            LOGGER.log(Level.FINER, "Tipos distintos. {0} <> {1}", new Object[]{entityClass, vertexClass});
            String javaClass = v.getType().getCustom("javaClass");
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
        Object oproxied = ObjectProxyFactory.create(toHydrate, v, t);

        LOGGER.log(Level.FINER, "**************************************************");
        LOGGER.log(Level.FINER, "Hydratando: {0} - Class: {1}", new Object[]{c.getName(), toHydrate});
        LOGGER.log(Level.FINER, "**************************************************");
        
        // recuperar la definición de la clase desde el caché
        ClassDef classdef = classCache.get(toHydrate);
        entityClass = classdef.entityName;
        
        // ********************************************************************************************
        // procesar los atributos básicos
        // ********************************************************************************************
        Field f;
        for (Map.Entry<String, Class<?>> entry : classdef.fields.entrySet()) {
            String prop = entry.getKey();
            Class<? extends Object> fieldClazz = entry.getValue();

            LOGGER.log(Level.FINER, "Buscando campo {0} de tipo {1}....", new String[]{prop, fieldClazz.getSimpleName()});
            Object value = v.getProperty(prop);

            if (value != null) {
                f = classdef.fieldsObject.get(prop);
                if (List.class.isAssignableFrom(f.getType())) {
                    // se debe hacer una copia del la lista para no quede referenciando al objeto original
                    // dado que en la asignación solo se pasa la referencia del objeto.
                    LOGGER.log(Level.FINER, "EmbeddedList detectada: realizando una copia del contenido...");
                    LOGGER.log(Level.FINER, "value: {0}", value.getClass());
                    this.setFieldValue(oproxied, prop, new ArrayListEmbeddedProxy((IObjectProxy) oproxied, (List) value));
                } else if (Map.class.isAssignableFrom(f.getType())) {
                    // se debe hacer una copia del la lista para no quede referenciando al objeto original
                    // dado que en la asignación solo se pasa la referencia del objeto.
                    LOGGER.log(Level.FINER, "EmbeddedMap detectado: realizando una copia del contenido...");
                    // FIXME: Ojo que se hace solo un shalow copy!! no se está conando la clave y el value
                    this.setFieldValue(oproxied, prop, new HashMapEmbeddedProxy((IObjectProxy) oproxied, (Map) value));
                } else {
                    LOGGER.log(Level.FINER, "hidratado campo: {0}={1}", new Object[]{prop, value});
                    this.setFieldValue(oproxied, prop, value);
                }
            } else {
                // si el valor es null verificar que no se trate de una Lista embebida 
                // que pueda haber sido inicializada en el constructor.
                f = classdef.fieldsObject.get(prop);
                Object fv = f.get(oproxied);
                if ((fv != null) && (List.class.isAssignableFrom(f.getType()))) {
                    // se trata de una lista embebida. Proceder a reemplazarlar con una que esté preparada.
                    LOGGER.log(Level.FINER, "Se ha detectado una lista embebida que no tiene valores. Se la reemplaza por una Embedded.");
                    this.setFieldValue(oproxied, prop, new ArrayListEmbeddedProxy((IObjectProxy) oproxied, (List) f.get(oproxied)));
                } else if ((fv != null) && (Map.class.isAssignableFrom(f.getType()))) {
                    // se trata de un Map embebido. Proceder a reemplazarlo con uno que esté preparado.
                    LOGGER.log(Level.FINER, "Se ha detectado un Map embebido que no tiene valores. Se lo reemplaza por uno Embedded.");
                    this.setFieldValue(oproxied, prop, new HashMapEmbeddedProxy((IObjectProxy) oproxied, (Map) f.get(oproxied)));
                }
            }
        }
        // insertar el objeto en el transactionLoopCache
        t.transactionLoopCache.put(v.getId().toString(), oproxied);

        // ********************************************************************************************
        // procesar los enum
        // ********************************************************************************************
        for (Map.Entry<String, Class<?>> entry : classdef.enumFields.entrySet()) {
            String prop = entry.getKey();
            LOGGER.log(Level.FINER, "Buscando campo {0} ....", new String[]{prop});
            Object value = v.getProperty(prop);
            if (value != null) {
                // FIXME: este código se puede mejorar. Tratar de usar solo setFieldValue()
                f = classdef.fieldsObject.get(prop);
                Object enumValue = Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString());
                LOGGER.log(Level.FINER, "Enum field: {0} type: {1} value: {2} Enum val: {3}",
                        new Object[]{f.getName(), f.getType(), value, enumValue});
                this.setFieldValue(oproxied, prop, enumValue);
                LOGGER.log(Level.FINER, "hidratado campo: {0}={1}", new Object[]{prop, value});
            }
        }

        // ********************************************************************************************
        // procesar colecciones de enums
        // ********************************************************************************************
        for (Map.Entry<String, Class<?>> entry : classdef.enumCollectionFields.entrySet()) {
            String prop = entry.getKey();
            Object value = v.getProperty(prop);
            if (value != null) {
                // reemplazar todos los valores por el emum correspondiente
                f = classdef.fieldsObject.get(prop);
                Class<?> listClass = getListType(f);
                for (int i = 0; i < ((List) value).size(); ++i) {
                    if (((List) value).get(i) instanceof String) {
                        // solo si el objeto contenido en la lista es un String.
                        String sVal = (String) ((List) value).get(i);
                        ((List) value).set(i, Enum.valueOf(listClass.asSubclass(Enum.class), sVal));
                    }
                }
                setFieldValue(oproxied, prop, value);
            }
        }

        // ********************************************************************************************
        // hidratar las colecciones
        // procesar todos los linkslist
        // ********************************************************************************************
        LOGGER.log(Level.FINER, "preparando las colecciones...");
        for (Map.Entry<String, Class<?>> entry : classdef.linkLists.entrySet()) {
            try {
                // FIXME: se debería considerar agregar una annotation EAGER!
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
                Field fLink = classdef.fieldsObject.get(field);
                fLink.setAccessible(true);
                
                String graphRelationName = entityClass + "_" + field;
                Direction RelationDirection = Direction.OUT;

                // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
                if ((v.countEdges(RelationDirection, graphRelationName) > 0) || (fLink.get(oproxied) != null)) {
                    this.colecctionToLazy(oproxied, field, fc, v, t);
                }
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // ********************************************************************************************
        // hidratar las colecciones indirectas
        // procesar todos los indirectLinkslist
        // ********************************************************************************************
        LOGGER.log(Level.FINER, "hidratar las colecciones indirectas...");
        for (Map.Entry<String, Class<?>> entry : classdef.indirectLinkLists.entrySet()) {
            try {
                // FIXME: se debería considerar agregar una annotation EAGER!
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
                Field fLink = classdef.fieldsObject.get(field);
                fLink.setAccessible(true);

                Direction RelationDirection = Direction.IN;
                Indirect in = fLink.getAnnotation(Indirect.class);
                String graphRelationName = in.linkName();
                // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
                if ((v.countEdges(RelationDirection, graphRelationName) > 0) || (fLink.get(oproxied) != null)) {
                    this.colecctionToLazy(oproxied, field, fc, v, t);
                }
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        LOGGER.log(Level.FINER, "******************* FIN HYDRATE *******************");
        t.closeInternalTx();
        return (T) oproxied;
    }

    
    public void colecctionToLazy(Object o, String field, OrientVertex v, Transaction t) {
        LOGGER.log(Level.FINER, "convertir colection a Lazy: {0}", field);
        ClassDef classdef;
        if (o instanceof IObjectProxy) {
            classdef = classCache.get(o.getClass().getSuperclass());
        } else {
            classdef = classCache.get(o.getClass());
        }

        Class<?> fc = classdef.linkLists.get(field);
        colecctionToLazy(o, field, fc, v, t);
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
    public void colecctionToLazy(Object o, String field, Class<?> fc, OrientVertex v, Transaction t) {
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
            Direction direction = Direction.OUT;

            if (fLink.isAnnotationPresent(Indirect.class)) {
                // si es un indirect se debe reemplazar el nombre de la relación por 
                // el propuesto por la anotation
                Indirect in = fLink.getAnnotation(Indirect.class);
                graphRelationName = in.linkName();
                direction = Direction.IN;
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
     * @param t the current transaction
     */
    public void collectionsToEmbedded(Object o, ClassDef classDef, Transaction t) {
        Field f;
        for (Map.Entry<String, Class<?>> entry : classDef.embeddedFields.entrySet()) {
            try {
                String field = entry.getKey();
                Class<? extends Object> value = entry.getValue();
                LOGGER.log(Level.FINER, "Procesando campo: {0} type: {1}", new String[]{field, value.getName()});
                
                f = classDef.fieldsObject.get(field);
                // realizar la conversión solo si el campo tiene un valor.
                if (f.get(o) != null) {
                    if (List.class.isAssignableFrom(value)) {
                        LOGGER.log(Level.FINER, "convirtiendo en ArrayListEmbeddedProxy...");
                        f.set(o, new ArrayListEmbeddedProxy((IObjectProxy) o, (List) f.get(o)));
                    } else if (Map.class.isAssignableFrom(value)) {
                        LOGGER.log(Level.FINER, "convirtiendo en HashMapEmbeddedProxy");
                        f.set(o, new HashMapEmbeddedProxy((IObjectProxy) o, (Map) f.get(o)));
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, "Error converting collections to embedded", ex);
            }
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
    public <T> T hydrate(Class<T> c, OrientEdge e, Transaction t) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        T oproxied = ObjectProxyFactory.create(c, e, t);
        ClassDef classdef = classCache.get(c);
        Field f;
        for (String prop : e.getPropertyKeys()) {
            Object value = e.getProperty(prop);

            // obtener la clase a la que pertenece el campo
            // (puede ser un enum en lugar de un atributo básico)
            // puede darse el caso que la base cree un atributo sobre los registros (ej: @rid) 
            // y la clave podría no corresponderse con un campo.
            Class<?> fc = classdef.fields.get(prop);
            fc = (fc == null) ? classdef.enumFields.get(prop) : fc;
            
            if (fc != null) {
                f = classdef.fieldsObject.get(prop);
                f.set(oproxied, fc.isEnum() ? Enum.valueOf(
                        f.getType().asSubclass(Enum.class), value.toString()) : value);
            }
        }
        return oproxied;
    }

    
    public void setFieldValue(Object o, String field, Object value) {
        try {
            Field f = this.classCache.get(o.getClass()).fieldsObject.get(field);
            f.set(o, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
