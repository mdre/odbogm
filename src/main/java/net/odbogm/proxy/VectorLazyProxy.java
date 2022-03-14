package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OVertex;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.odbogm.LogginProperties;
import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class VectorLazyProxy extends Vector implements ILazyCollectionCalls {

    private final static Logger LOGGER = Logger.getLogger(VectorLazyProxy.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.VectorLazyProxy);
        }
    }
    
    private boolean dirty = false;
    
    private boolean lazyLoad = true;
    private boolean lazyLoading = false;
    
    private Transaction transaction;
    private String field;
    private Class<?> fieldClass;
    private ODirection direction;
    
    // referencia debil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;
    
    @Override
    public void init(Transaction t, IObjectProxy parent, String field, Class<?> c, ODirection d) {
        this.transaction = t;
        this.parent = new WeakReference<>(parent);
        this.field = field;
        this.fieldClass = c;
        this.direction = d;
        LOGGER.log(Level.FINER, () -> String.format("relatedTo: %s - field: %s - Class: %s",
            parent.___getVertex().toString(), field, c.getSimpleName()));
    }

    //********************* change control **************************************
    private final Map<Object, ObjectCollectionState> listState = new ConcurrentHashMap<>();
    
    private void lazyLoad() {
        this.transaction.initInternalTx();
        this.lazyLoad = false;
        this.lazyLoading = true;
        IObjectProxy theParent = this.parent.get();
        if (theParent != null) {
            LOGGER.log(Level.FINER, () -> String.format("relatedTo: %s - field: %s - Class: %s",
                theParent.___getVertex().toString(), field, fieldClass.getSimpleName()));
            // recuperar todos los elementos desde el vértice y agregarlos a la colección
            Iterable<OVertex> rt = theParent.___getVertex().getVertices(direction, field);
            
            String auditLogLabel = theParent.___getAuditLogLabel();
            
            for (OVertex next : rt) {
                Object o = transaction.get(fieldClass, next.getIdentity().toString());
                this.add(o);
                // se asume que todos fueron borrados
                this.listState.put(o, ObjectCollectionState.REMOVED);
                
                // replicate the AuditLogLabel to inner objects
                ((IObjectProxy)o).___setAuditLogLabel(auditLogLabel);
            }
        }
        this.lazyLoading = false;
        this.transaction.closeInternalTx();
    }
    
    @Override
    public synchronized void updateAuditLogLabel(Set seen) {
        if (!this.lazyLoad) {
            IObjectProxy theParent = this.parent.get();
            if (theParent != null) {
                String auditLogLabel = theParent.___getAuditLogLabel();
                for (Object o : this) {
                    if (o instanceof IObjectProxy) {
                        ((IObjectProxy)o).___replicateAuditLogLabel(auditLogLabel, seen);
                    }
                }
            }
        }
    }
    
    @Override
    public Map<Object, ObjectCollectionState> collectionState() {
        // si se ha hecho referencia al contenido de la colección, realizar la verificación
        var aux = new ConcurrentHashMap<>(this.listState);
        if (!this.lazyLoad) {
            for (Object o : this) {
                // actualizar el estado
                if (this.listState.get(o) == null) {
                    // se agregó un objeto
                    aux.put(o, ObjectCollectionState.ADDED);
                } else {
                    // el objeto existe. Removerlo para que solo queden los que se agregaron o eliminaron 
                    aux.remove(o);
                    // el objeto existe. Marcarlo como sin cambio para la colección
//                    this.listState.replace(o, ObjectCollectionState.NOCHANGE);
                }
            }
        }
        return aux;
    }
    
    /**
     * Vuelve establecer el punto de verificación.
     */
    @Override
    public void clearState() {
        this.dirty = false;
        
        this.listState.clear();

        for (Object o : this) {
            if (this.listState.get(o) == null) {
                // se asume que todos fueron borrados
                this.listState.put(o, ObjectCollectionState.REMOVED);
            }
        }
    }
    
    private void setDirty() {
        LOGGER.log(Level.FINER, "Colección marcada como Dirty. Avisar al padre.");
        this.dirty = true;
        LOGGER.log(Level.FINER, "weak:"+this.parent.get());
        // si el padre no está marcado como garbage, notificarle el cambio de la colección.
        if (this.parent.get()!=null)
            this.parent.get().___setDirty();
    }
    
    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void rollback() {
        //FIXME: Analizar si se puede implementar una versión que no borre todos los elementos
        super.clear();
        this.listState.clear();
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

    public VectorLazyProxy(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    public VectorLazyProxy(int initialCapacity) {
        super(initialCapacity);
    }

    public VectorLazyProxy() {
        super();
    }

    @Override
    public Spliterator spliterator() {
        if (lazyLoad)
            this.lazyLoad();
        return super.spliterator(); 
    }

    @Override
    public synchronized void sort(Comparator c) {
        if (lazyLoad)
            this.lazyLoad();
        super.sort(c); 
    }

    @Override
    public synchronized void replaceAll(UnaryOperator operator) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.replaceAll(operator); 
    }

    @Override
    public synchronized boolean removeIf(Predicate filter) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.removeIf(filter); 
    }

    @Override
    public synchronized void forEach(Consumer action) {
        if (lazyLoad)
            this.lazyLoad();
        super.forEach(action); 
    }

    @Override
    public synchronized Iterator iterator() {
        if (lazyLoad)
            this.lazyLoad();
        return super.iterator(); 
    }

    @Override
    public synchronized ListIterator listIterator() {
        if (lazyLoad)
            this.lazyLoad();
        return super.listIterator(); 
    }

    @Override
    public synchronized ListIterator listIterator(int index) {
        if (lazyLoad)
            this.lazyLoad();
        return super.listIterator(index); 
    }

    @Override
    protected synchronized void removeRange(int fromIndex, int toIndex) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.removeRange(fromIndex, toIndex); 
    }

    @Override
    public synchronized List subList(int fromIndex, int toIndex) {
        if (lazyLoad)
            this.lazyLoad();
        return super.subList(fromIndex, toIndex); 
    }

    @Override
    public synchronized String toString() {
        if (lazyLoad)
            this.lazyLoad();
        return super.toString(); 
    }

    @Override
    public synchronized int hashCode() {
        if (lazyLoad)
            this.lazyLoad();
        return super.hashCode(); 
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (lazyLoad)
            this.lazyLoad();
        return super.equals(o); 
    }

    @Override
    public synchronized boolean addAll(int index, Collection c) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.addAll(index, c); 
    }

    @Override
    public synchronized boolean retainAll(Collection c) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.retainAll(c); 
    }

    @Override
    public synchronized boolean removeAll(Collection c) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.removeAll(c); 
    }

    @Override
    public synchronized boolean addAll(Collection c) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.addAll(c); 
    }

    @Override
    public synchronized boolean containsAll(Collection c) {
        if (lazyLoad)
            this.lazyLoad();
        return super.containsAll(c); 
    }

    @Override
    public void clear() {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.clear(); 
    }

    @Override
    public synchronized Object remove(int index) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.remove(index); 
    }

    @Override
    public void add(int index, Object element) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.add(index, element); 
    }

    @Override
    public boolean remove(Object o) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.remove(o); 
    }

    @Override
    public synchronized boolean add(Object e) {
        if (lazyLoad)
            this.lazyLoad();
        if(!this.lazyLoading) this.setDirty();
        return super.add(e); 
    }

    @Override
    public synchronized Object set(int index, Object element) {
        if (lazyLoad)
            this.lazyLoad();
        return super.set(index, element); 
    }

    @Override
    public synchronized Object get(int index) {
        if (lazyLoad)
            this.lazyLoad();
        return super.get(index); 
    }

    @Override
    public synchronized Object[] toArray(Object[] a) {
        if (lazyLoad)
            this.lazyLoad();
        return super.toArray(a); 
    }

    @Override
    public synchronized Object[] toArray() {
        if (lazyLoad)
            this.lazyLoad();
        return super.toArray(); 
    }

    @Override
    public synchronized Object clone() {
        if (lazyLoad)
            this.lazyLoad();
        return super.clone(); 
    }

    @Override
    public synchronized void removeAllElements() {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.removeAllElements(); 
    }

    @Override
    public synchronized boolean removeElement(Object obj) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        return super.removeElement(obj); 
    }

    @Override
    public synchronized void addElement(Object obj) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.addElement(obj); 
    }

    @Override
    public synchronized void insertElementAt(Object obj, int index) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.insertElementAt(obj, index); 
    }

    @Override
    public synchronized void removeElementAt(int index) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.removeElementAt(index); 
    }

    @Override
    public synchronized void setElementAt(Object obj, int index) {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.setElementAt(obj, index); 
    }

    @Override
    public synchronized Object lastElement() {
        if (lazyLoad)
            this.lazyLoad();
        return super.lastElement(); 
    }

    @Override
    public synchronized Object firstElement() {
        if (lazyLoad)
            this.lazyLoad();
        return super.firstElement(); 
    }

    @Override
    public synchronized Object elementAt(int index) {
        if (lazyLoad)
            this.lazyLoad();
        return super.elementAt(index); 
    }

    @Override
    public synchronized int lastIndexOf(Object o, int index) {
        if (lazyLoad)
            this.lazyLoad();
        return super.lastIndexOf(o, index); 
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        if (lazyLoad)
            this.lazyLoad();
        return super.lastIndexOf(o); 
    }

    @Override
    public synchronized int indexOf(Object o, int index) {
        if (lazyLoad)
            this.lazyLoad();
        return super.indexOf(o, index); 
    }

    @Override
    public int indexOf(Object o) {
        if (lazyLoad)
            this.lazyLoad();
        return super.indexOf(o); 
    }

    @Override
    public boolean contains(Object o) {
        return super.contains(o); 
    }

    @Override
    public Enumeration elements() {
        if (lazyLoad)
            this.lazyLoad();
        return super.elements(); 
    }

    @Override
    public synchronized boolean isEmpty() {
        if (lazyLoad)
            this.lazyLoad();
        return super.isEmpty(); 
    }

    @Override
    public synchronized int size() {
        if (lazyLoad)
            this.lazyLoad();
        return super.size(); 
    }

    @Override
    public synchronized int capacity() {
        if (lazyLoad)
            this.lazyLoad();
        return super.capacity(); 
    }

    @Override
    public synchronized void setSize(int newSize) {
        if (lazyLoad)
            this.lazyLoad();
        super.setSize(newSize); 
    }

    @Override
    public synchronized void ensureCapacity(int minCapacity) {
        if (lazyLoad)
            this.lazyLoad();
        super.ensureCapacity(minCapacity); 
    }

    @Override
    public synchronized void trimToSize() {
        if (lazyLoad)
            this.lazyLoad();
        this.setDirty();
        super.trimToSize(); 
    }

    @Override
    public synchronized void copyInto(Object[] anArray) {
        if (lazyLoad)
            this.lazyLoad();
        super.copyInto(anArray); 
    }

    @Override
    public Stream parallelStream() {
        if (lazyLoad)
            this.lazyLoad();
        return super.parallelStream(); 
    }

    @Override
    public Stream stream() {
        if (lazyLoad)
            this.lazyLoad();
        return super.stream(); 
    }
    
}
