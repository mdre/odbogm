package net.odbogm.utils;

import com.tinkerpop.blueprints.impls.orient.OrientDynaElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 * @param <Object>
 */
public class ODBOrientDynaElementIterable<Object> implements Iterable<Object>, AutoCloseable {
    private final static Logger LOGGER = Logger.getLogger(ODBOrientDynaElementIterable.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }

    private final OrientDynaElementIterable iterable;
    private final OrientGraph localtx;
    
    public ODBOrientDynaElementIterable(OrientGraph graph, OrientDynaElementIterable it) {
        this.localtx = graph;
        this.iterable = it;
    }

    @Override
    public void close() {
        iterable.close();
        this.localtx.shutdown(true, false);
    }

    @Override
    public Iterator<Object> iterator() {
        return (Iterator<Object>) this.iterable.iterator();
    }

    @Override
    public Spliterator<Object> spliterator() {
        return Iterable.super.spliterator();
    }
    
}
