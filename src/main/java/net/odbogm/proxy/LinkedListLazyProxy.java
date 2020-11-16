package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OVertex;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Spliterator;
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
public class LinkedListLazyProxy extends LinkedList implements ILazyCollectionCalls {

    private final static Logger LOGGER = Logger.getLogger(LinkedListLazyProxy.class.getName());

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.LinkedListLazyProxy);
        }
    }

    private boolean dirty = false;

    private boolean lazyLoad = true;
    private boolean lazyLoading = false;

    private Transaction transaction;
    private OVertex relatedTo;
    private String field;
    private Class<?> fieldClass;
    private ODirection direction;

    // referencia debil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;

    /**
     * Crea un ArrayList lazy.
     *
     * @param t Vínculo a la transacción actual
     * @param relatedTo: Vértice con el cual se relaciona la colección
     * @param field: atributo de relación
     * @param c: clase genérica de la colección.
     */
    @Override
    public void init(Transaction t, OVertex relatedTo, IObjectProxy parent, String field, Class<?> c, ODirection d) {
        try {
            this.transaction = t;
            this.relatedTo = relatedTo;
            this.parent = new WeakReference<>(parent);
            this.field = field;
            this.fieldClass = c;
            this.direction = d;
            LOGGER.log(Level.FINER, "relatedTo: {0} - field: {1} - Class: {2}", new Object[]{relatedTo, field, c.getSimpleName()});
            //LOGGER.log(Level.FINER, "relatedTo.getGraph : " + relatedTo.getGraph());
        } catch (Exception ex) {
            Logger.getLogger(LinkedListLazyProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //********************* change control **************************************
    private Map<Object, ObjectCollectionState> listState = new ConcurrentHashMap<>();

    private void lazyLoad() {
        this.transaction.initInternalTx();
        
        //LOGGER.log(Level.FINER, "getGraph: " + relatedTo.getGraph());
//        if (relatedTo.getGraph() == null) {
//            this.transaction.getSessionManager().getGraphdb().attach(relatedTo);
//        }

//        LOGGER.log(Level.FINER, "getRawGraph: " + relatedTo.getGraph().getRawGraph());

//        relatedTo.getGraph().getRawGraph().activateOnCurrentThread();
//        ODatabaseDocument database = (ODatabaseDocument) ODatabaseRecordThreadLocal.INSTANCE.get();
//        database.activateOnCurrentThread();
//        LOGGER.log(Level.FINER, "ODatabase: "+database+" activated");

//        LOGGER.log(Level.INFO, "Lazy Load.....");
        this.lazyLoad = false;
        this.lazyLoading = true;
        LOGGER.log(Level.FINER, "relatedTo: {0} - field: {1} - Class: {2}", new Object[]{relatedTo, field, fieldClass.getSimpleName()});
        // recuperar todos los elementos desde el vértice y agregarlos a la colección
        Iterable<OVertex> rt = relatedTo.getVertices(this.direction, field);
//        for (Iterator<Vertex> iterator = relatedTo.getVertices(Direction.OUT, field).iterator(); iterator.hasNext();) {
        for (Iterator<OVertex> iterator = rt.iterator(); iterator.hasNext();) {
            OVertex next = (OVertex) iterator.next();
//            LOGGER.log(Level.INFO, "loading: " + next.getId().toString());
            
            Object o = null;
            o = transaction.get(fieldClass, next.getIdentity().toString());
            
            this.add(o);
            // se asume que todos fueron borrados
            this.listState.put(o, ObjectCollectionState.REMOVED);
        }
        this.lazyLoading = false;
        this.transaction.closeInternalTx();
    }

    public Map<Object, ObjectCollectionState> collectionState() {
        // si se ha hecho referencia al contenido de la colección, realizar la verificación
        if (!this.lazyLoad) {
            for (Object o : this) {
                // actualizar el estado
                if (this.listState.get(o) == null) {
                    // se agregó un objeto
                    this.listState.put(o, ObjectCollectionState.ADDED);
                } else {
                    // el objeto existe. Removerlo para que solo queden los que se agregaron o eliminaron 
                    this.listState.remove(o);
                    // el objeto existe. Marcarlo como sin cambio para la colección
//                    this.listState.replace(o, ObjectCollectionState.NOCHANGE);
                }
            }
        }
        return this.listState;
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
        // Si es una colección sobre una dirección saliente proceder a marcar
        // en caso contrario se la considera como un Indirect no NO REPORTA 
        // las modificaciones
        if (this.direction == ODirection.OUT) {
            LOGGER.log(Level.FINER, "Colección marcada como Dirty. Avisar al padre.");
            this.dirty = true;
            LOGGER.log(Level.FINER, "weak:" + this.parent.get());
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

    public LinkedListLazyProxy() {
        super();
    }

    @Override
    public Spliterator spliterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.spliterator();
    }

    @Override
    public Object[] toArray(Object[] a) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.toArray(a);
    }

    @Override
    public Object[] toArray() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.toArray();
    }

    @Override
    public Object clone() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.clone();
    }

    @Override
    public Iterator descendingIterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.descendingIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.listIterator(index);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.removeLastOccurrence(o);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.removeFirstOccurrence(o);
    }

    @Override
    public Object pop() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.pop();
    }

    @Override
    public void push(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.push(e);
    }

    @Override
    public Object pollLast() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.pollLast();
    }

    @Override
    public Object pollFirst() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.pollFirst();
    }

    @Override
    public Object peekLast() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.peekLast();
    }

    @Override
    public Object peekFirst() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.peekFirst();
    }

    @Override
    public boolean offerLast(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.offerLast(e);
    }

    @Override
    public boolean offerFirst(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.offerFirst(e);
    }

    @Override
    public boolean offer(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.offer(e);
    }

    @Override
    public Object remove() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.remove();
    }

    @Override
    public Object poll() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.poll();
    }

    @Override
    public Object element() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.element();
    }

    @Override
    public Object peek() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.peek();
    }

    @Override
    public int lastIndexOf(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.lastIndexOf(o);
    }

    @Override
    public int indexOf(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.indexOf(o);
    }

    @Override
    public Object remove(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.remove(index);
    }

    @Override
    public void add(int index, Object element) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.add(index, element);
    }

    @Override
    public Object set(int index, Object element) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.set(index, element);
    }

    @Override
    public Object get(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.get(index);
    }

    @Override
    public void clear() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.clear();
    }

    @Override
    public boolean addAll(int index, Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.remove(o);
    }

    @Override
    public boolean add(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        if (!this.lazyLoading) {
            this.setDirty();
        }
        return super.add(e);
    }

    @Override
    public int size() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.size();
    }

    @Override
    public boolean contains(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.contains(o);
    }

    @Override
    public void addLast(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.addLast(e);
    }

    @Override
    public void addFirst(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.addFirst(e);
    }

    @Override
    public Object removeLast() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.removeLast();
    }

    @Override
    public Object removeFirst() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.removeFirst();
    }

    @Override
    public Object getLast() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.getLast();
    }

    @Override
    public Object getFirst() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.getFirst();
    }

    @Override
    public Iterator iterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.iterator();
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public int hashCode() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.equals(o);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public ListIterator listIterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.listIterator();
    }

    @Override
    public void sort(Comparator c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.sort(c);
    }

    @Override
    public void replaceAll(UnaryOperator operator) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.replaceAll(operator);
    }

    @Override
    public String toString() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.toString();
    }

    @Override
    public boolean retainAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.removeAll(c);
    }

    @Override
    public boolean containsAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.containsAll(c);
    }

    @Override
    public boolean isEmpty() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.isEmpty();
    }

    @Override
    public Stream parallelStream() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.parallelStream();
    }

    @Override
    public Stream stream() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.stream();
    }

    @Override
    public boolean removeIf(Predicate filter) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.removeIf(filter);
    }

    @Override
    public void forEach(Consumer action) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.forEach(action);
    }

    @Override
    protected void finalize() throws Throwable {
//        if (lazyLoad) {
//            this.lazyLoad();
//        }
        super.finalize();
    }

}
