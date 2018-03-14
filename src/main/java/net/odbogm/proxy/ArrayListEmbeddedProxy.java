/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.utils.ThreadHelper;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ArrayListEmbeddedProxy extends ArrayList implements IEmbeddedCalls {

    private static final long serialVersionUID = 3136116168236143774L;
    private final static Logger LOGGER = Logger.getLogger(ArrayListEmbeddedProxy.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ArrayListEmbeddedProxy);
        }
    }
    // referencia debil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;
    
    /**
     * Crea un ArrayList embebido.
     *
     * @param parent Vínculo al objeto que contiene la colección
     */
    @Override
    public synchronized void init(IObjectProxy parent) {
        try {
            this.parent = new WeakReference<>(parent);
        } catch (Exception ex) {
            Logger.getLogger(ArrayListEmbeddedProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    private synchronized void setDirty() {
        LOGGER.log(Level.FINER, "Colección marcada como Dirty. Avisar al padre.");
        LOGGER.log(Level.FINER, "weak:"+this.parent.get());
        // si el padre no está marcado como garbage, notificarle el cambio de la colección.
        if (this.parent.get()!=null) {
            this.parent.get().___setDirty();
            
            LOGGER.log(Level.FINER, ThreadHelper.getCurrentStackTrace());
        }
    }
    
    //====================================================================================

    public ArrayListEmbeddedProxy() {
        super();
    }
    
    public ArrayListEmbeddedProxy(IObjectProxy parent) {
        super();
        this.init(parent);
    }
    
    
    public ArrayListEmbeddedProxy(IObjectProxy parent, List l) {
        super(l);
        this.init(parent);
    }


    @Override
    public void replaceAll(UnaryOperator operator) {
        this.setDirty();
        super.replaceAll(operator);
    }

    @Override
    public boolean removeIf(Predicate filter) {
        boolean removed = super.removeIf(filter);
        if (removed)
            this.setDirty();
        return removed;
    }



    @Override
    public boolean retainAll(Collection c) {
        boolean changeDetected = super.retainAll(c);
        if (changeDetected)
            this.setDirty();
        return changeDetected;
    }

    @Override
    public boolean removeAll(Collection c) {
        boolean changeDetected = super.removeAll(c);
        if (changeDetected)
            this.setDirty();
        return changeDetected;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        this.setDirty();
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        this.setDirty();
        return super.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection c) {
        this.setDirty();
        return super.addAll(c);
    }

    @Override
    public void clear() {
        this.setDirty();
        super.clear();
    }

    @Override
    public boolean remove(Object o) {
        
        boolean changeDetected = super.remove(o);
        if (changeDetected)
            this.setDirty();
        return changeDetected;
    }

    @Override
    public Object remove(int index) {
        this.setDirty();
        return super.remove(index);
    }

    @Override
    public void add(int index, Object element) {
        this.setDirty();
        super.add(index, element);
    }

    @Override
    public boolean add(Object e) {
        LOGGER.log(Level.FINER, "DIRTY: Elemento nuevo agregado: "+e.toString());
        this.setDirty();
        return super.add(e);
    }

    @Override
    public Object set(int index, Object element) {
        this.setDirty();
        return super.set(index, element);
    }


    @Override
    public void trimToSize() {
        this.setDirty();
        super.trimToSize();
    }
    
}
