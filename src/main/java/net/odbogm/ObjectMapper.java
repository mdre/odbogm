/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

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
import net.odbogm.proxy.ILazyCollectionCalls;
import net.odbogm.proxy.ILazyMapCalls;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.proxy.ObjectProxyFactory;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public class ObjectMapper {

    private final static Logger LOGGER = Logger.getLogger(ObjectMapper.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.ObjectMapper);
    }
//    private static int newObjectCounter = 0;

    private SessionManager sessionManager;
    private ClassCache classCache;

    public ObjectMapper(SessionManager sm) {
//        LOGGER.setLevel(Level.INFO);

        // inicializar le caché de clases
        classCache = new ClassCache();
        this.sessionManager = sm;
    }

    /**
     * Devuelve la definición de la clase para el objeto pasado por parámetro
     *
     * @param o
     * @return
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
     * @param o
     * @return
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
     * @param o
     * @return
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
     * @param o
     * @param oStruct
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
     * @param <T>
     * @param c
     * @param v
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public <T> T hydrate(Class<T> c, OrientVertex v) throws DuplicateClassDefinition, InstantiationException, IllegalAccessException, NoSuchFieldException, CollectionNotSupported {
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
        Object oproxied = ObjectProxyFactory.create(toHydrate, v, sessionManager);

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
            
            LOGGER.log(Level.FINER, "Buscando campo {0} ....", new String[]{prop});
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
                } else {
//                    f.set(oproxied, value);
                    this.setFieldValue(oproxied, prop, value);
                }
                LOGGER.log(Level.FINER, "hidratado campo: " + prop + "=" + value);
                f.setAccessible(acc);
            }
        }
        // insertar el objeto en el transactionCache
        this.sessionManager.transactionCache.put(v.getId().toString(), oproxied);

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
                    this.colecctionToLazy(oproxied, field, fc, v);
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

    public void colecctionToLazy(Object o, String field, OrientVertex v) {
        ClassDef classdef;
        if (o instanceof IObjectProxy) {
            classdef = classCache.get(o.getClass().getSuperclass());
        } else {
            classdef = classCache.get(o.getClass());
        }

        Class<?> fc = classdef.linkLists.get(field);
        colecctionToLazy(o, field, fc, v);
    }

    /**
     * Convierte una colección común en una Lazy para futuras operaciones.
     *
     *
     * @param o objeto base sobre el que se trabaja
     * @param field campo a modificar
     * @param fc clase original del campo
     * @param v vértice con el cual se conecta.
     *
     */
    public void colecctionToLazy(Object o, String field, Class<?> fc, OrientVertex v) {
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
                ((ILazyCollectionCalls) col).init(sessionManager, v, (IObjectProxy) o, graphRelationName, listClass);

//                LOGGER.log(Level.FINER, "col: "+col.getDBClass());
            } else if (col instanceof Map) {
                ParameterizedType listType = (ParameterizedType) fLink.getGenericType();
                Class<?> keyClass = (Class<?>) listType.getActualTypeArguments()[0];
                Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[1];
                // inicializar la colección
                ((ILazyMapCalls) col).init(sessionManager, v, (IObjectProxy) o, graphRelationName, keyClass, valClass);
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
     * Hidrata un objeto a partir de los atributos guardados en un Edge
     *
     * @param <T> clase del objeto a devolver
     * @param c : clase del objeto a devolver
     * @param e : Edge desde el que recuperar los datos
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public <T> T hydrate(Class<T> c, OrientEdge e) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        T oproxied = ObjectProxyFactory.create(c, e, sessionManager);
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
