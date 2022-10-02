package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OVertex;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
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
import net.odbogm.exceptions.RelatedToNullException;
import net.odbogm.utils.ThreadHelper;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 * @param <E> class type
 */
public class ArrayListLazyProxy<E> extends ArrayList<E> implements ILazyCollectionCalls {

    private static final long serialVersionUID = -3396834078126983330L;

    private final static Logger LOGGER = Logger.getLogger(ArrayListLazyProxy.class.getName());

    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ArrayListLazyProxy);
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

    /**
     * Crea un ArrayList lazy.
     *
     * @param t Vínculo a la Transacción actual
     * @param parent : Vértice con el cual se relaciona la colección
     * @param field: atributo de relación
     * @param c: clase genérica de la colección.
     */
    @Override
    public synchronized void init(Transaction t, IObjectProxy parent, String field, Class<?> c, ODirection d) {
        if (parent == null) {
            throw new RelatedToNullException("Se ha detectado un ArraylistLazyProxy sin relación con un vértice!\n field: " + field + " Class: " + c.getSimpleName());
        }
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

    private synchronized void lazyLoad() {
        this.transaction.initInternalTx();
        this.lazyLoad = false;
        this.lazyLoading = true;
        
        // recuperar todos los elementos desde el vértice y agregarlos a la colección
        IObjectProxy theParent = this.parent.get();
        
        String auditLogLabel = theParent.___getAuditLogLabel();
        
        if (theParent != null) {
            LOGGER.log(Level.FINER, () -> String.format("relatedTo: %s - field: %s - Class: %s",
                theParent.___getVertex().toString(), field, fieldClass.getSimpleName()));
            Iterable<OVertex> rt = theParent.___getVertex().getVertices(this.direction, field);
            for (OVertex next : rt) {
                E o = (E)transaction.get(fieldClass, next.getIdentity().toString());
                this.add(o);
                // se asume que todos fueron borrados
                this.listState.put(o, ObjectCollectionState.REMOVED);
                
                // replicate the AuditLogLabel to inner objects
                if (auditLogLabel != null && o instanceof IObjectProxy) {
                    ((IObjectProxy)o).___setAuditLogLabel(auditLogLabel);
                }
            }
        }
        this.lazyLoading = false;
        this.transaction.closeInternalTx();
    }

    @Override
    public synchronized Map<Object, ObjectCollectionState> collectionState() {
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
    public synchronized void clearState() {
        this.dirty = false;

        this.listState.clear();

        for (Object o : this) {
            if (this.listState.get(o) == null) {
                // se asume que todos fueron borrados
                this.listState.put(o, ObjectCollectionState.REMOVED);
            }
        }
    }
    
    
    
    private synchronized void setDirty() {
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

                LOGGER.log(Level.FINER, ThreadHelper.getCurrentStackTrace());
            }
        }
    }

    @Override
    public synchronized boolean isDirty() {
        return this.dirty;
    }

    @Override
    public synchronized void rollback() {
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
    public ArrayListLazyProxy() {
        super();
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
    public String toString() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.toString(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.containsAll(c); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<E> parallelStream() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.parallelStream(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<E> stream() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.stream(); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void sort(Comparator<? super E> c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.sort(c);
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.replaceAll(operator);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        boolean removed = super.removeIf(filter);
        if (removed) {
            this.setDirty();
        }
        return removed;
    }

    @Override
    public Spliterator<E> spliterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.spliterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.forEach(action);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public Iterator<E> iterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.iterator();
    }

    @Override
    public ListIterator<E> listIterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.listIterator(index);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        boolean changeDetected = super.retainAll(c);
        if (changeDetected) {
            this.setDirty();
        }
        return changeDetected;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        boolean changeDetected = super.removeAll(c);
        if (changeDetected) {
            this.setDirty();
        }
        return changeDetected;
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
    public boolean addAll(int index, Collection<? extends E> c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.addAll(c);
    }

    @Override
    public void clear() {
        //FIXME: se puede optimizar. No tiene sentido cargar todo para luego borrar.
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.clear();
    }

    @Override
    public boolean remove(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }

        boolean changeDetected = super.remove(o);
        if (changeDetected) {
            this.setDirty();
        }
        return changeDetected;
    }

    @Override
    public E remove(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.remove(index);
    }

    @Override
    public void add(int index, E element) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.add(index, element);
    }

    @Override
    public boolean add(E e) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        if (!this.lazyLoading) {
            LOGGER.log(Level.FINER, "DIRTY: Elemento nuevo agregado: " + e.toString());
            this.setDirty();
        }
        return super.add(e);
    }

    @Override
    public E set(int index, E element) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        return super.set(index, element);
    }

    @Override
    public E get(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.get(index);
    }

    @Override
    public  <T extends Object> T[] toArray(T[] a) {
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
    public boolean contains(Object o) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.contains(o);
    }

    @Override
    public boolean isEmpty() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.isEmpty();
    }

    @Override
    public int size() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.size();
    }

    @Override
    public void ensureCapacity(int minCapacity) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.ensureCapacity(minCapacity);
    }

    @Override
    public void trimToSize() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        this.setDirty();
        super.trimToSize();
    }

}
