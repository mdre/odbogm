/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.utils.ThreadHelper;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class HashMapEmbeddedProxy extends HashMap<Object, Object> implements IEmbeddedCalls {

    private final static Logger LOGGER = Logger.getLogger(HashMapEmbeddedProxy.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.HashMapEmbeddedProxy);
        }
    }
    private boolean dirty = false;
    
    
    // referencia devil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;
    
    /**
     * Crea un HashMap con detección de cambios.
     *
     * @param parent link al padre
     */
    @Override
    public void init(IObjectProxy parent) {
        this.parent = new WeakReference<>(parent);
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
    
    
    /**
     * Crea un map utilizando los atributos del Edge como key. Si se utiliza un objeto para representar los atributos, se debe declarar en el
     * annotation.
     */
    public HashMapEmbeddedProxy() {
        super();
    }

    public HashMapEmbeddedProxy(IObjectProxy parent) {
        super();
        this.init(parent);
    }
    
    public HashMapEmbeddedProxy(IObjectProxy parent, Map source) {
        super(source);
        this.init(parent);
    }

    
    @Override
    public Object clone() {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
        this.setDirty();
        super.replaceAll(function); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void forEach(BiConsumer<? super Object, ? super Object> action) {
        super.forEach(action); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.setDirty();
        return super.merge(key, value, remappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object compute(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return super.compute(key, remappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return super.computeIfPresent(key, remappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
        return super.computeIfAbsent(key, mappingFunction); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object replace(Object key, Object value) {
        this.setDirty();
        return super.replace(key, value); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        this.setDirty();
        return super.replace(key, oldValue, newValue); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean remove(Object key, Object value) {
        this.setDirty();
        return super.remove(key, value); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        Object res = super.putIfAbsent(key, value); //To change body of generated methods, choose Tools | Templates.
        if (res!=null)
            this.setDirty();
        return res;
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return super.getOrDefault(key, defaultValue); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return super.entrySet(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Object> values() {
        return super.values(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Object> keySet() {
        return super.keySet(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(value); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        this.setDirty();
        super.clear(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object remove(Object key) {
        this.setDirty();
        return super.remove(key); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> m) {
        this.setDirty();
        super.putAll(m); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object put(Object key, Object value) {
        this.setDirty();
        return super.put(key, value); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object get(Object key) {
        return super.get(key); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int size() {
        return super.size(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toString() {
        return super.toString(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

}
