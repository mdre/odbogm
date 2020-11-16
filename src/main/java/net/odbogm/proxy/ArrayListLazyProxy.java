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
 */
public class ArrayListLazyProxy extends ArrayList implements ILazyCollectionCalls {

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
    private OVertex relatedTo;
    private String field;
    private Class<?> fieldClass;
    private ODirection direction;

    // referencia debil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;

    /**
     * Crea un ArrayList lazy.
     *
     * @param t Vínculo a la Transacción actual
     * @param relatedTo: Vértice con el cual se relaciona la colección
     * @param field: atributo de relación
     * @param c: clase genérica de la colección.
     */
    @Override
    public synchronized void init(Transaction t, OVertex relatedTo, IObjectProxy parent, String field, Class<?> c, ODirection d) {
        try {
            if (relatedTo == null) {
                throw new RelatedToNullException("Se ha detectado un ArraylistLazyProxy sin relación con un vértice!\n field: " + field + " Class: " + c.getSimpleName());
            }
            this.transaction = t;
            this.relatedTo = relatedTo;
            this.parent = new WeakReference<>(parent);
            this.field = field;
            this.fieldClass = c;
            this.direction = d;
            LOGGER.log(Level.FINER, "relatedTo: {0} - field: {1} - Class: {2}", new Object[]{relatedTo, field, c.getSimpleName()});
            //LOGGER.log(Level.FINER, "relatedTo.getGraph : " + relatedTo.getGraph());
        } catch (Exception ex) {
            Logger.getLogger(ArrayListLazyProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //********************* change control **************************************
    private Map<Object, ObjectCollectionState> listState = new ConcurrentHashMap<>();

    private synchronized void lazyLoad() {
        this.transaction.initInternalTx();
        
        //LOGGER.log(Level.FINER, "getGraph: " + relatedTo.getGraph());
//        if (relatedTo.getGraph() == null) {
//            this.transaction.getSessionManager().getGraphdb().attach(relatedTo);
//        }

//        LOGGER.log(Level.FINER, "getRawGraph: "+relatedTo.getGraph().getRawGraph());
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
            // LOGGER.log(Level.INFO, "loading: " + next.getId().toString());
            // el Lazy SIEMPRE carga los datos desde la base de datos esquivando los objetos que se encuentren en 
            // el cache.
            Object o = null;
            o = transaction.get(fieldClass, next.getIdentity().toString());
            this.add(o);
            
            // se asume que todos fueron borrados
            this.listState.put(o, ObjectCollectionState.REMOVED);
        }
        this.lazyLoading = false;
        this.transaction.closeInternalTx();
    }

    public synchronized Map<Object, ObjectCollectionState> collectionState() {
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
    public boolean containsAll(Collection c) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.containsAll(c); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream parallelStream() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.parallelStream(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream stream() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.stream(); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    protected void finalize() throws Throwable {
//        if (lazyLoad) {
//            this.lazyLoad();
//        }
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
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
    public boolean removeIf(Predicate filter) {
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
    public Spliterator spliterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.spliterator();
    }

    @Override
    public void forEach(Consumer action) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        super.forEach(action);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public Iterator iterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.iterator();
    }

    @Override
    public ListIterator listIterator() {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.listIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        if (lazyLoad) {
            this.lazyLoad();
        }
        return super.listIterator(index);
    }

    @Override
    public boolean retainAll(Collection c) {
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
    public boolean removeAll(Collection c) {
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
    public boolean add(Object e) {
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
