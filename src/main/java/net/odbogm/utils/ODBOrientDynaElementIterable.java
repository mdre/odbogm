/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.utils;

import com.tinkerpop.blueprints.impls.orient.OrientDynaElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ODBOrientDynaElementIterable<T>  {
    private final static Logger LOGGER = Logger.getLogger(ODBOrientDynaElementIterable.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }

    OrientDynaElementIterable iterable;
    OrientGraph localtx;
    
    public ODBOrientDynaElementIterable(OrientGraph graph, OrientDynaElementIterable it) {
        this.iterable = it;
    }

    public Iterator<Object> iterator() {
        return iterable.iterator();
    }

    public void close() {
        iterable.close();
    }

    public void forEach(Consumer<? super Object> action) {
        iterable.forEach(action);
    }

    public Spliterator<Object> spliterator() {
        return iterable.spliterator();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
        this.localtx.shutdown(true, false);
    }
    
}
