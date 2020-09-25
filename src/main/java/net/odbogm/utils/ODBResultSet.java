package net.odbogm.utils;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ODBResultSet implements OResultSet, AutoCloseable {
    private final static Logger LOGGER = Logger.getLogger(ODBResultSet.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(Level.INFO);
        }
    }
    
    private final OResultSet ors;
    private final ODatabaseSession localtx;
    
    public ODBResultSet(ODatabaseSession graph, OResultSet it) {
        this.localtx = graph;
        this.ors = it;
    }

    @Override
    public void close() {
        ors.close();
        this.localtx.close();
    }

    public OResultSet getResultSet() {
        return this.ors;
    }

    private void activate() {
        if (!this.localtx.isActiveOnCurrentThread()) {
           localtx.activateOnCurrentThread();
        }
    }
    @Override
    public boolean hasNext() {
        activate();
        return this.ors.hasNext();
    }

    @Override
    public OResult next() {
        activate();
        return this.ors.next();
    }

    @Override
    public void remove() {
        activate();
        ors.remove();
    }

    @Override
    public Optional<OExecutionPlan> getExecutionPlan() {
        activate();
        return ors.getExecutionPlan();
    }

    @Override
    public Map<String, Long> getQueryStats() {
        activate();
        return ors.getQueryStats();
    }

    @Override
    public void reset() {
        activate();
        ors.reset();
    }

    @Override
    public boolean tryAdvance(Consumer<? super OResult> action) {
        activate();
        return ors.tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super OResult> action) {
        activate();
        ors.forEachRemaining(action);
    }

    @Override
    public OResultSet trySplit() {
        activate();
        return ors.trySplit();
    }

    @Override
    public long estimateSize() {
        activate();
        return ors.estimateSize();
    }

    @Override
    public int characteristics() {
        activate();
        return ors.characteristics();
    }

    @Override
    public Stream<OResult> stream() {
        activate();
        return ors.stream();
    }

    @Override
    public Stream<OElement> elementStream() {
        activate();
        return ors.elementStream();
    }

    @Override
    public Stream<OVertex> vertexStream() {
        activate();
        return ors.vertexStream();
    }

    @Override
    public Stream<OEdge> edgeStream() {
        activate();
        return ors.edgeStream();
    }

    @Override
    public long getExactSizeIfKnown() {
        activate();
        return ors.getExactSizeIfKnown();
    }

    @Override
    public boolean hasCharacteristics(int characteristics) {
        activate();
        return ors.hasCharacteristics(characteristics);
    }

    @Override
    public Comparator<? super OResult> getComparator() {
        activate();
        return ors.getComparator();
    }

}

