/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

//import com.esotericsoftware.kryo.Kryo;
import net.odbogm.cache.ClassCache;
import net.odbogm.cache.ClassDef;
import net.odbogm.exceptions.CollectionNotSupported;
import net.odbogm.utils.ReflectionUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import java.util.HashMap;
import java.util.List;
import net.odbogm.exceptions.DuplicateClassDefinition;
import net.odbogm.proxy.ArrayListEmbeddedProxy;
import net.odbogm.proxy.HashMapEmbeddedProxy;
import net.odbogm.proxy.ILazyCollectionCalls;
import net.odbogm.proxy.ILazyMapCalls;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.proxy.ObjectProxyFactory;
import org.objenesis.ObjenesisStd;

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
//    private static int newObjectCounter = 0;
//    private Kryo kryo = new Kryo();
    
//    private SessionManager sessionManager;
    private ClassCache classCache;
    
    // usado para no llamar a Class.forName para clases ya cargadas.
    private HashMap<String, Class> classLoaded = new HashMap<>();
    
    // Clase encargada de la instanciación de los objetos.
    // http://objenesis.org/
    private ObjenesisStd objenesis = new ObjenesisStd();
    
    
    public ObjectMapper() {
//        LOGGER.setLevel(Level.INFO);

        // inicializar le caché de clases
        classCache = new ClassCache();
//        this.sessionManager = sm;
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
     * Devuelve un mapeo rápido del Objeto. No procesa los link o linklist. Simplemente devuelve todos los atributos del objeto en un map
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

    public void simpleFastMap(Object o, ClassDef classmap, HashMap<String, Object> data) {
        // procesar todos los campos
        classmap.fields.entrySet().stream().forEach((entry) -> {
            try {
                String field = entry.getKey();
                Class<?> c = entry.getValue();

                Field f = ReflectionUtils.findField(o.getClass(), field);
                boolean acc = f.isAccessible();
                f.setAccessible(true);

                // determinar si no es nulo
                if (f.get(o) != null) {
                    data.put(f.getName(), f.get(o));
                }
                f.setAccessible(acc);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        ObjectStruct oStruct = new ObjectStruct();

        // buscar la definición de la clase en el caché
        ClassDef classmap;
        if (o instanceof IObjectProxy) {
            classmap = classCache.get(o.getClass().getSuperclass());
        } else {
            classmap = classCache.get(o.getClass());
        }

//        this.map(o, o.getDBClass(),mappedObject);      
        this.fastmap(o, classmap, oStruct);
        return oStruct;
    }

    /**
     * Realiza un mapeo a partir de las definiciones existentes en el caché
     *
     * @param o objeto a analizar
     * @param oStruct objeto de referencia a completar
     */
    private void fastmap(Object o, ClassDef classmap, ObjectStruct oStruct) {

        // procesar todos los campos
        classmap.fields.entrySet().stream().forEach((entry) -> {
            try {
                String field = entry.getKey();
                Class<?> c = entry.getValue();
                Field f = ReflectionUtils.findField(o.getClass(), field);
                
                boolean acc = f.isAccessible();
                f.setAccessible(true);
                
                // determinar si no es nulo
                if (f.get(o) != null) {
                    LOGGER.log(Level.FINER, "Field: "+field+" Class: "+c.getSimpleName()+": "+f.get(o));
                    oStruct.fields.put(f.getName(), f.get(o));
                }
                f.setAccessible(acc);
                
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        // procesar todos los Enums
        classmap.enumFields.entrySet().stream().forEach((entry) -> {
            try {
                String field = entry.getKey();
                Class<?> c = entry.getValue();

                Field f = ReflectionUtils.findField(o.getClass(), field);
                boolean acc = f.isAccessible();
                f.setAccessible(true);
                // determinar si no es nulo
                if (f.get(o) != null) {
                    oStruct.fields.put(f.getName(), "" + f.get(o));
                }
                f.setAccessible(acc);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        // procesar todos los links
        classmap.links.entrySet().stream().forEach((entry) -> {
            try {
                String field = entry.getKey();
                Class<?> c = entry.getValue();

                Field f = ReflectionUtils.findField(o.getClass(), field);
                boolean acc = f.isAccessible();
                f.setAccessible(true);
                // determinar si no es nulo
                if (f.get(o) != null) {
                    oStruct.links.put(f.getName(), f.get(o));
                }
                f.setAccessible(acc);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        // procesar todos los linksList
        classmap.linkLists.entrySet().stream().forEach((entry) -> {
            try {
                String field = entry.getKey();
                Class<?> c = entry.getValue();

                Field f = ReflectionUtils.findField(o.getClass(), field);
                boolean acc = f.isAccessible();
                f.setAccessible(true);
                // determinar si no es nulo
                if (f.get(o) != null) {
                    oStruct.linkLists.put(f.getName(), f.get(o));
                }
                f.setAccessible(acc);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

    }

    /**
     * Crea y llena un objeto con los valores correspondintes obtenidos del Vertice asignado.
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
//        T o = c.newInstance();
        // activar la base de datos en el hilo actual.
        v.getGraph().getRawGraph().activateOnCurrentThread();

        Class<?> toHydrate = c;
        String vertexClass = (v.getType().getName() == "V" ? c.getSimpleName() : v.getType().getName());

        // validar que el Vertex sea instancia de la clase solicitada
        // o que la clase solicitada sea su superclass
        if (!c.getSimpleName().equals(vertexClass)) {
            LOGGER.log(Level.FINER, "Tipos distintos. {0} <> {1}", new Object[]{c.getSimpleName(), vertexClass});
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
        Map<String, Class<?>> fieldmap = classdef.fields;
        
        Field f;
        for (Map.Entry<String, Class<?>> entry : fieldmap.entrySet()) {
            String prop = entry.getKey();
            Class<? extends Object> fieldClazz = entry.getValue();
            
            LOGGER.log(Level.FINER, "Buscando campo {0} de tipo {1}....", new String[]{prop, fieldClazz.getSimpleName()});
            Object value = v.getProperty(prop);
            
            if (value != null) {
                // obtener la clase a la que pertenece el campo
                Class<?> fc = fieldmap.get(prop);
                
                f = ReflectionUtils.findField(toHydrate, prop);
                
                boolean acc = f.isAccessible();
                f.setAccessible(true);
                if (f.getType().isEnum()) {
                    LOGGER.log(Level.FINER, "Enum field: " + f.getName() + " type: " + f.getType() + "  value: " + value + "   Enum val: " + Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
//                    f.set(oproxied, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
                    this.setFieldValue(oproxied, prop, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
                } else if (f.getType().isAssignableFrom(List.class)) {
                    // si la lista fuera de Enum, es necesario realizar la conversión antes dado que se guarda como string.
                    ParameterizedType listType = (ParameterizedType) f.getGenericType();
                    Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                    if (listClass.isEnum()) {
                        // reemplazar todos los valores por el emum correspondiente
                        for (int i = 0; i < ((List)value).size(); i++) {
                            if (((List)value).get(i) instanceof String) {
                                // solo si el objeto contenido en la lista es un String.
                                String sVal = (String)((List)value).get(i);
                                ((List)value).set(i, Enum.valueOf(listClass.asSubclass(Enum.class), sVal));
                            }
                        }
                    }
                    // se debe hacer una copia del la lista para no quede referenciando al objeto original
                    // dado que en la asignación solo se pasa la referencia del objeto.
                    LOGGER.log(Level.FINER, "EmbeddedList detectada: realizando una copia del contenido...");
                    LOGGER.log(Level.FINER, "value: "+value.getClass());
                    this.setFieldValue(oproxied, prop, new ArrayListEmbeddedProxy((IObjectProxy)oproxied,(List)value));
                } else if (f.getType().isAssignableFrom(Map.class)) {
                    // se debe hacer una copia del la lista para no quede referenciando al objeto original
                    // dado que en la asignación solo se pasa la referencia del objeto.
                    LOGGER.log(Level.FINER, "EmbeddedMap detectado: realizando una copia del contenido...");
                    // FIXME: Ojo que se hace solo un shalow copy!! no se está conando la clave y el value
                    this.setFieldValue(oproxied, prop, new HashMapEmbeddedProxy((IObjectProxy)oproxied,(Map)value));
                } else{
                    LOGGER.log(Level.FINER, "hidratado campo: " + prop + "=" + value);
                    this.setFieldValue(oproxied, prop, value);
                }
                f.setAccessible(acc);
            } else {
                // si el valor es null verificar que no se trate de una Lista embebida 
                // que pueda haber sido inicializada en el constructor.
                f = ReflectionUtils.findField(toHydrate, prop);
                
                boolean acc = f.isAccessible();
                f.setAccessible(true);
                if ((f.get(oproxied)!=null) && (f.getType().isAssignableFrom(List.class))) {
                    // se trata de una lista embebida. Proceder a reemplazarlar con una que esté preparada.
                    LOGGER.log(Level.FINER, "Se ha detectado una lista embebida que no tiene valores. Se la reemplaza por una Embedded.");
                    this.setFieldValue(oproxied, prop, new ArrayListEmbeddedProxy((IObjectProxy)oproxied,(List)f.get(oproxied)));
                } else if ((f.get(oproxied)!=null) && (f.getType().isAssignableFrom(Map.class))) {
                    // se trata de un Map embebido. Proceder a reemplazarlo con uno que esté preparado.
                    LOGGER.log(Level.FINER, "Se ha detectado un Map embebido que no tiene valores. Se lo reemplaza por uno Embedded.");
                    this.setFieldValue(oproxied, prop, new HashMapEmbeddedProxy((IObjectProxy)oproxied,(Map)f.get(oproxied)));
                }
                f.setAccessible(acc);
            }
        }
        // insertar el objeto en el transactionCache
        t.transactionCache.put(v.getId().toString(), oproxied);

        // procesar los enum
        for (Map.Entry<String, Class<?>> entry : classdef.enumFields.entrySet()) {
            String prop = entry.getKey();
            Class<? extends Object> fieldClazz = entry.getValue();

            LOGGER.log(Level.FINER, "Buscando campo {0} ....", new String[]{prop});
            Object value = v.getProperty(prop);
            if (value != null) {
                // obtener la clase a la que pertenece el campo
                Class<?> fc = fieldmap.get(prop);
                // FIXME: este código se puede mejorar. Tratar de usar solo setFieldValue()
                f = ReflectionUtils.findField(toHydrate, prop);
//
//                boolean acc = f.isAccessible();
//                f.setAccessible(true);
                LOGGER.log(Level.FINER, "Enum field: " + f.getName() + " type: " + f.getType() + "  value: " + value + "   Enum val: " + Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
//                f.set(oproxied, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
                this.setFieldValue(oproxied, prop, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));

                LOGGER.log(Level.FINER, "hidratado campo: " + prop + "=" + value);
//                f.setAccessible(acc);
            }
        }

        // hidratar las colecciones
        // procesar todos los linkslist
        LOGGER.log(Level.FINER, "preparando las colecciones...");
        for (Map.Entry<String, Class<?>> entry : classdef.linkLists.entrySet()) {

            try {
                // FIXME: se debería considerar agregar una annotation EAGER!
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                LOGGER.log(Level.FINER, "Field: {0}   Class: {1}", new String[]{field, fc.getName()});
                Field fLink = ReflectionUtils.findField(toHydrate, field);
                String graphRelationName = toHydrate.getSimpleName() + "_" + field;
                boolean acc = fLink.isAccessible();
                fLink.setAccessible(true);

                // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
                if ((v.countEdges(Direction.OUT, graphRelationName) > 0) || (fLink.get(oproxied) != null)) {
                    this.colecctionToLazy(oproxied, field, fc, v, t);
                }

                fLink.setAccessible(acc);

            } catch (NoSuchFieldException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

//        LOGGER.log(Level.FINER, "Objeto hydratado: " + oproxied.toString());
        LOGGER.log(Level.FINER, "******************* FIN HYDRATE *******************");
        return (T) oproxied;
    }

    public void colecctionToLazy(Object o, String field, OrientVertex v, Transaction t) {
        LOGGER.log(Level.FINER, "convertir colection a Lazy: "+field);
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
        LOGGER.log(Level.FINER, "convertir colection a Lazy: "+field+" class: "+fc.getName());
        LOGGER.log(Level.FINER, "***************************************************************");
        try {
            Class<?> c;
            if (o instanceof IObjectProxy) {
                c = o.getClass().getSuperclass();
            } else {
                c = o.getClass();
            }

            Field fLink = ReflectionUtils.findField(c, field);
            String graphRelationName = c.getSimpleName() + "_" + field;
            boolean acc = fLink.isAccessible();
            fLink.setAccessible(true);

            Class<?> lazyClass = Primitives.LAZY_COLLECTION.get(fc);
            LOGGER.log(Level.FINER, "lazyClass: " + lazyClass.getName());
            Object col = lazyClass.newInstance();
            // dependiendo de si la clase hereda de Map o List, inicalizar
            if (col instanceof List) {
                ParameterizedType listType = (ParameterizedType) fLink.getGenericType();
                Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                // inicializar la colección
                ((ILazyCollectionCalls) col).init(t, v, (IObjectProxy) o, graphRelationName, listClass);

//                LOGGER.log(Level.FINER, "col: "+col.getDBClass());
            } else if (col instanceof Map) {
                ParameterizedType listType = (ParameterizedType) fLink.getGenericType();
                Class<?> keyClass = (Class<?>) listType.getActualTypeArguments()[0];
                Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[1];
                // inicializar la colección
                ((ILazyMapCalls) col).init(t, v, (IObjectProxy) o, graphRelationName, keyClass, valClass);
            } else {
                throw new CollectionNotSupported();
            }

            fLink.set(o, col);
            fLink.setAccessible(acc);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | InstantiationException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Convierte todas las colecciones identificadas como embedded en el ClassDef a sus correspondientes proxies
     * @param o the object to be analyzed
     * @param classDef  the class struct
     * @param t the current transaction
     */
    public void collectionsToEmbedded(Object o, ClassDef classDef, Transaction t) {
        boolean acc;
        Field f;
        for (Map.Entry<String, Class<?>> entry : classDef.embeddedFields.entrySet()) {
            try {
                String field = entry.getKey();
                Class<? extends Object> value = entry.getValue();
                
                Class<?> c;
                if (o instanceof IObjectProxy) {
                    c = o.getClass().getSuperclass();
                } else {
                    c = o.getClass();
                }
                LOGGER.log(Level.FINER, "Procesando campo: {0} type: {1}",new String[]{field,value.getName()});
                f = ReflectionUtils.findField(c, field);
                acc = f.isAccessible();
                f.setAccessible(true);
                // realizar la conversión solo si el campo tiene un valor.
                if (f.get(o)!=null) {
                    if (value.isAssignableFrom(List.class)) { 
                        LOGGER.log(Level.FINER, "convirtiendo en ArrayListEmbeddedProxy...");
                        f.set(o, new ArrayListEmbeddedProxy((IObjectProxy) o, (List) f.get(o)));
                    } else if (value.isAssignableFrom(Map.class)) { 
                        LOGGER.log(Level.FINER, "convirtiendo en HashMapEmbeddedProxy");
                        f.set(o, new HashMapEmbeddedProxy((IObjectProxy) o, (Map) f.get(o)));
                    }
                }
                f.setAccessible(acc);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
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
        // recuperar la definición de la clase desde el caché
        ClassDef classdef = classCache.get(c);
        Map<String, Class<?>> fieldmap = classdef.fields;

        Field f;
        for (String prop : e.getPropertyKeys()) {
            Object value = e.getProperty(prop);

            // obtener la clase a la que pertenece el campo
            Class<?> fc = fieldmap.get(prop);
//            LOGGER.log(Level.FINER, "hidratando campo: "+prop);
            // puede darse el caso que la base cree un atributo sobre los registros (ej: @rid) 
            // y la clave podría no corresponderse con un campo.
            if (fc != null) {
                f = ReflectionUtils.findField(c, prop);

                boolean acc = f.isAccessible();
                f.setAccessible(true);
                f.set(oproxied, value);
                f.setAccessible(acc);
            }
        }

        return oproxied;
    }

    public static void setFieldValue(Object o, String field, Object value) {
        try {
            Field f = ReflectionUtils.findField(o.getClass(), field);
            boolean acc = f.isAccessible();
            f.setAccessible(true);

            // determinar si no es nulo
            f.set(o, value);

            f.setAccessible(acc);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ObjectMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
