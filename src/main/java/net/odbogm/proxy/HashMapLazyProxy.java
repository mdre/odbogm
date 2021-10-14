package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.Primitives;
import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class HashMapLazyProxy extends HashMap<Object, Object> implements ILazyMapCalls {

    private final static Logger LOGGER = Logger.getLogger(HashMapLazyProxy.class.getName());

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.HashMapLazyProxy);
        }
    }

    private boolean dirty = false;

    private boolean lazyLoad = true;
    private boolean lazyLoading = false;

    private Transaction transaction;
    private String field;
    private Class<?> keyClass;
    private Class<?> valueClass;
    private ODirection direction;

    // referencia debil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;

    /**
     * Crea un ArrayList lazy.
     *
     * @param t Vínculo a la transacción actual
     * @param parent: Vértice con el cual se relaciona la colección
     * @param field: atributo de relación
     * @param k: clase del key.
     * @param v: clase del value.
     * @param d
     */
    @Override
    public void init(Transaction t, IObjectProxy parent, String field, Class<?> k, Class<?> v, ODirection d) {
        this.transaction = t;
        this.parent = new WeakReference<>(parent);
        this.field = field;
        this.keyClass = k;
        this.valueClass = v;
        this.direction = d;
    }

    //********************* change control **************************************
    private final Map<Object, ObjectCollectionState> entitiesState = new ConcurrentHashMap<>();
    private final Map<Object, ObjectCollectionState> keyState = new ConcurrentHashMap<>();
    private Map<Object, OEdge> keyToEdge = new ConcurrentHashMap<>();
    private final Map<Object, Object> keyToDeleted = new ConcurrentHashMap<>();
    private final Map<Object, Set<OEdge>> valueToEdge = new ConcurrentHashMap<>();

    private synchronized void lazyLoad() {
        this.transaction.initInternalTx();
        
        LOGGER.log(Level.FINEST, "Lazy Load.....");
        this.lazyLoad = false;
        this.lazyLoading = true;

        IObjectProxy theParent = this.parent.get();
        if (theParent != null) {
            // recuperar todos los elementos desde el vértice y agregarlos a la colección
            boolean indirect = this.direction == ODirection.IN;
            for (OEdge edge : theParent.___getVertex().getEdges(this.direction, field)) {
                OVertex next = indirect ? edge.getFrom() : edge.getTo();
                LOGGER.log(Level.FINER, "loading edge: {0} to: {1}", new Object[]{edge.getIdentity().toString(),next.getIdentity()});
                // el Lazy simpre se hace recuperado los datos desde la base de datos.
                Object o = transaction.get(valueClass, next.getIdentity().toString());

                // para cada vértice conectado, es necesario mapear todos los Edges que los unen.
                Object k = edgeToObject(edge);

                // llamar a super para que no se marque como dirty el objeto padre dado que el loadLazy no debería 
                // registrar cambios en el padre porque se los datos son recuperados de la base
                super.put(k, o);
                if (!edge.getIdentity().isNew()) {
                    // if edge is new then we don't add it to keyState to consider it added,
                    // so it can be processed at commit time
                    this.keyState.put(k, ObjectCollectionState.REMOVED);
                    addValueToEdge(o, edge);
                }

                // como puede estar varias veces un objecto agregado al map con distintos keys
                // primero verificamos su existencia para no duplicarlos.
                if (this.entitiesState.get(o) == null) {
                    // se asume que todos fueron borrados
                    this.entitiesState.put(o, ObjectCollectionState.REMOVED);
                }
            }
        }
        this.lazyLoading = false;
        this.transaction.closeInternalTx();
        LOGGER.log(Level.FINEST, "final size: {0} ",super.size());
    }
    
    private void addValueToEdge(Object value, OEdge edge) {
        if (!this.valueToEdge.containsKey(value)) {
            this.valueToEdge.put(value, new HashSet());
        }
        this.valueToEdge.get(value).add(edge);
    }
    
    public void removeValueToEdge(Object value, OEdge edge) {
        if (this.valueToEdge.containsKey(value)) {
            var s = this.valueToEdge.get(value);
            
//            por alguna razón loca lo siguiente NO ANDA!!!!!!
//            System.out.println("Size: " + s.size());
//            System.out.println("rid: " + edge.getIdentity().toString());
//            var e1 = s.iterator().next();
//            System.out.println("rid: " + e1.getIdentity().toString());
//            System.out.println(e1.hashCode());
//            var e2 = edge;
//            System.out.println(e2.hashCode());
//            System.out.println(e1.equals(e2));
//                                                
//            System.out.println("Contiene: " + s.contains(e1));
//            System.out.println("Contiene: " + s.contains(e2));
//            boolean ok = s.remove(edge);
//            System.out.println("Eliminado: " + ok);
//            System.out.println("Size: " + s.size());

            //por lo tanto elimino así:
            var s2 = new HashSet<OEdge>(s);
            s.clear();
            s2.stream().filter(e -> !e.getIdentity().equals(edge.getIdentity())).forEach(s::add);
        }
    }

    /**
     * Initializes a newly stored map.
     */
    @Override
    public void initStored() {
        if (!isEmpty()) { // triggers lazy load
            // if not empty, newly temporary edges exist. Make dirty so it's processed
            this.setDirty();
        }
    }
    
    /**
     * Vuelve establecer el punto de verificación.
     */
    @Override
    public synchronized void clearState() {
        this.entitiesState.clear();
        this.keyState.clear();
        this.keyToDeleted.clear();
        Map<Object, OEdge> newOE = new ConcurrentHashMap<>();

        for (Entry<Object, Object> entry : this.entrySet()) {
            Object k = entry.getKey();
            Object o = entry.getValue();

            this.keyState.put(k, ObjectCollectionState.REMOVED);

            // verificar si existe una relación con en Edge
            if (this.keyToEdge.get(k) != null) {
                newOE.put(k, this.keyToEdge.get(k));
            }

            // como puede estar varias veces un objecto agregado al map con distintos keys
            // primero verificamos su existencia para no duplicarlos.
            if (this.entitiesState.get(o) == null) {
                // se asume que todos fueron borrados
                this.entitiesState.put(o, ObjectCollectionState.REMOVED);
            }

        }
        this.keyToEdge = newOE;
        this.dirty = false;
    }

    /**
     * Actualiza el estado de todo el MAP y devuelve la referencia al estado de los keys
     *
     * @return retorna un mapa con el estado de la colección
     */
    @Override
    public Map<Object, ObjectCollectionState> getKeyState() {
        var aux = new ConcurrentHashMap<>(this.keyState);
        for (Object key : this.keySet()) {
            // update the state of the key:
            this.keyToDeleted.remove(key); // the key exists, remove it from deleted map
            if (this.keyState.get(key) == null) {
                // se agregó un objeto
                aux.put(key, ObjectCollectionState.ADDED);
            } else {
                // el objeto existe. Marcarlo como sin cambio para la colección
                aux.replace(key, ObjectCollectionState.NOCHANGE);
            }
        }
        return aux;
    }

    @Override
    public Map<Object, ObjectCollectionState> getEntitiesState() {
        var aux = new ConcurrentHashMap<>(this.entitiesState);
        for (Object value : this.values()) {
            // actualizar el estado del valor
            if (this.entitiesState.get(value) == null) {
                // se agregó un objeto
                aux.put(value, ObjectCollectionState.ADDED);
            } else {
                // el objeto existe. Marcarlo como sin cambio para la colección
                aux.replace(value, ObjectCollectionState.NOCHANGE);
            }
        }
        return aux;
    }

    @Override
    public Map<Object, OEdge> getKeyToEdge() {
        return keyToEdge;
    }

    public Map<Object, Object> getKeyToDeleted() {
        return keyToDeleted;
    }

    public Map<Object, Set<OEdge>> getValueToEdge() {
        return valueToEdge;
    }

    private void setDirty() {
        if (this.direction == ODirection.OUT) {
            LOGGER.log(Level.FINER, "Colección marcada como Dirty. Avisar al padre.");
            this.dirty = true;
            LOGGER.log(Level.FINER, () -> "weak:" + this.parent.get());
            // si el padre no está marcado como garbage, notificarle el cambio de la colección.
            if (this.parent.get() != null) {
                this.parent.get().___setDirty();
            }
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public synchronized void rollback() {
        //FIXME: Analizar si se puede implementar una versión que no borre todos los elementos
        super.clear();
        this.entitiesState.clear();
        this.keyToEdge.clear();
        this.keyState.clear();
        this.keyToDeleted.clear();
        this.dirty = false;
        this.lazyLoad = true;
    }

    
    /**
     * Método interno usado por 
     * fuerza la recarga de todos los elementos del vector. La llamada a este método
     * produce que se invoque a clear y luego se recarguen todos los objetos.
     */
    @Override
    public void forceLoad() {
        super.clear();
        this.lazyLoad();
    }
    
    
    //====================================================================================
    /**
     * Crea un map utilizando los atributos del Edge como key. Si se utiliza un objeto para representar los atributos, se debe declarar en el
     * annotation.
     */
    public HashMapLazyProxy() {
        super();
    }

    public HashMapLazyProxy(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public HashMapLazyProxy(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public Object clone() {
        if (lazyLoad) {
            this.lazyLoad();
        }

        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        if (!this.lazyLoading) {
            this.setDirty();
        }
        super.replaceAll(function); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void forEach(BiConsumer<? super Object, ? super Object> action) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.forEach(action); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        if (!this.lazyLoading) {
            this.setDirty();
        }
        return super.merge(key, value, remappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object compute(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.compute(key, remappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.computeIfPresent(key, remappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.computeIfAbsent(key, mappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object replace(Object key, Object value) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        if (!this.lazyLoading) {
            this.setDirty();
        }
        return super.replace(key, value); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        if (!this.lazyLoading) {
            this.setDirty();
        }
        return super.replace(key, oldValue, newValue); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        boolean res = super.remove(key, value);
        if (res) {
            if (!this.lazyLoading) {
                this.setDirty();
            }
            this.keyToDeleted.put(key, value);
        }
        return res;
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        Object res = super.putIfAbsent(key, value); //To change body of generated methods, choose Tools | Templates.
        if (res != null) {
            this.setDirty();
        }
        return res;
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.getOrDefault(key, defaultValue); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.entrySet(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Object> values() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.values(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Object> keySet() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.keySet(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsValue(Object value) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.containsValue(value); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        this.keyToDeleted.putAll(this);
        super.clear();
    }

    @Override
    public Object remove(Object key) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        Object previous = super.remove(key);
        if (previous != null) {
            this.keyToDeleted.put(key, previous);
        }
        return previous;
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> m) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.putAll(m); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object put(Object key, Object value) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        Object previous = super.put(key, value);
        if (previous != null) {
            if (!Objects.equals(value, previous)) {
                //we consider the value as removed to remove the old edge
                this.entitiesState.put(previous, ObjectCollectionState.REMOVED);
            }
        }
        if (this.keyState.containsKey(key)) {
            //if it was previously marked as REMOVED, remove the mark
            this.keyState.remove(key);
        }
        return previous;
    }

    @Override
    public boolean containsKey(Object key) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.containsKey(key); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object get(Object key) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.get(key); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEmpty() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.isEmpty(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int size() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.size(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toString() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.toString(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean equals(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.equals(o); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateKey(Object originalKey, OEdge edge) {
        if (originalKey instanceof IObjectProxy) {
            // if already a IObjectProxy update the associated edge
            ((IObjectProxy)originalKey).___setEdge(edge);
            //update value to edge???
        } else {
            Object key = this.edgeToObject(edge);
            Object value = this.get(originalKey);
            //we must replace the original key object with the proxy
            super.remove(originalKey);
            super.put(key, value);
            //and the state
            ObjectCollectionState state = this.keyState.get(originalKey);
            if (state != null) {
                this.keyState.put(key, state);
            }
            addValueToEdge(value, edge);
        }
    }

    private Object edgeToObject(OEdge edge) {
        Object k;
        LOGGER.log(Level.FINER, "edge keyclass: {0}  OE RID:{1}",
                new Object[]{this.keyClass, edge.getIdentity().toString()});
        if (Primitives.PRIMITIVE_MAP.containsKey(this.keyClass)) {
            k = edge.getProperty("key");
            LOGGER.log(Level.INFO, "primitive edge key: {0}",k);
        } else {
            //if keyClass is not a native type, we must hydrate an object
            LOGGER.log(Level.FINER, "clase como key");
            k = transaction.getEdgeAsObject(keyClass, edge);
        }
        this.keyToEdge.put(k, edge);
        return k;
    }
    
}
