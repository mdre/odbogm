/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import net.odbogm.SessionManager;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
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
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author SShadow
 */
public class LinkedListLazyProxy extends LinkedList implements ILazyCollectionCalls {

    private final static Logger LOGGER = Logger.getLogger(LinkedListLazyProxy.class.getName());

    private boolean dirty = false;
    private boolean lazyLoad = true;
    private SessionManager sm;
    private OrientVertex relatedTo;
    private String field;
    private Class<?> fieldClass;

    /**
     * Crea un LinkedList lazy.
     *
     * @param sm Vínculo al SessionManager actual
     * @param relatedTo: Vértice con el cual se relaciona la colección
     * @param field: atributo de relación
     * @param c: clase genérica de la colección.
     */
    @Override
    public void init(SessionManager sm, OrientVertex relatedTo, String field, Class<?> c) {
        this.sm = sm;
        this.relatedTo = relatedTo;
        this.field = field;
        this.fieldClass = c;
    }

    //********************* change control **************************************
    private Map<Object, ObjectCollectionState> listState = new ConcurrentHashMap<>();

    private void lazyLoad() {
//        LOGGER.log(Level.INFO, "Lazy Load.....");
        this.lazyLoad = false;

        // recuperar todos los elementos desde el vértice y agregarlos a la colección
        for (Iterator<Vertex> iterator = relatedTo.getVertices(Direction.OUT, field).iterator(); iterator.hasNext();) {
            OrientVertex next = (OrientVertex) iterator.next();
//            LOGGER.log(Level.INFO, "loading: " + next.getId().toString());
            Object o = sm.get(fieldClass, next.getId().toString());
            this.add(o);
            // se asume que todos fueron borrados
            this.listState.put(o, ObjectCollectionState.REMOVED);
        }
    }

    public Map<Object, ObjectCollectionState> collectionState() {
        for (Object o : this) {
            // actualizar el estado
            if (this.listState.get(o) == null) {
                // se agregó un objeto
                this.listState.put(o, ObjectCollectionState.ADDED);
            } else {
                // el objeto existe. Marcarlo como sin cambio para la colección
                this.listState.remove(o);
            }
        }
        return this.listState;
    }
    
    /**
     * Vuelve  establecer el punto de verificación.
     */
    @Override
    public void clearState() {
        this.listState.clear();

        for (Object o : this) {
            if (this.listState.get(o) == null) {
                // se asume que todos fueron borrados
                this.listState.put(o, ObjectCollectionState.REMOVED);
            }
        }
    }
    
    
    @Override
    public boolean isDirty() {
        return this.dirty;
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
        return super.removeLastOccurrence(o); 
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.removeFirstOccurrence(o); 
    }

    @Override
    public Object pop() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.pop(); 
    }

    @Override
    public void push(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.push(e); 
    }

    @Override
    public Object pollLast() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.pollLast(); 
    }

    @Override
    public Object pollFirst() {
        if (lazyLoad) {
            this.lazyLoad();
        }
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
        return super.offerLast(e); 
    }

    @Override
    public boolean offerFirst(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.offerFirst(e); 
    }

    @Override
    public boolean offer(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.offer(e); 
    }

    @Override
    public Object remove() {
        if (lazyLoad) {
            this.lazyLoad();
        }
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
        return super.remove(index); 
    }

    @Override
    public void add(int index, Object element) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.add(index, element); 
    }

    @Override
    public Object set(int index, Object element) {
        if (lazyLoad) {
            this.lazyLoad();
        }
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
        super.clear(); 
    }

    @Override
    public boolean addAll(int index, Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.addAll(index, c); 
    }

    @Override
    public boolean addAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.addAll(c); 
    }

    @Override
    public boolean remove(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.remove(o); 
    }

    @Override
    public boolean add(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
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
        super.addLast(e); 
    }

    @Override
    public void addFirst(Object e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.addFirst(e); 
    }

    @Override
    public Object removeLast() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.removeLast(); 
    }

    @Override
    public Object removeFirst() {
        if (lazyLoad) {
            this.lazyLoad();
        }
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
        return super.retainAll(c); 
    }

    @Override
    public boolean removeAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
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
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.finalize(); 
    }

    

}
