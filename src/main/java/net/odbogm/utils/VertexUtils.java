/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.utils;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;

/**
 *
 * @author SShadow
 */
public class VertexUtils {

    private final static Logger LOGGER = Logger.getLogger(VertexUtils.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.VertexUtil);
    }

    /**
     * Check if two vertex are conected
     *
     * @param v1 first vertex
     * @param v2 second vertex
     * @return true if any conection exist
     */
    public static boolean areConected(OrientVertex v1, OrientVertex v2, String edgeLabel) {
        boolean connected = false;
        if ((v1 != null)&&(v2 != null)) {
            Iterable<Edge> result = v1.getEdges(v2, Direction.BOTH, edgeLabel==null?"E":edgeLabel);
            for (Edge e : result) {
                connected = true;
                break;
            }
        }
        return connected;
    }
    
    
    /**
     * Check if v1 is conected to (out) v2
     *
     * @param v1 source vertex
     * @param v2 target vertex
     * @param edgeLabel edge label to test. Null to test any label.
     * @return true if a conection exist
     */
    public static boolean isConectedTo(OrientVertex v1, OrientVertex v2, String edgeLabel) {
        boolean connected = false;
        LOGGER.log(Level.FINER, "Verificando edges entre "+v1.getId()+" y "+v2.getId()+ " a través de la realción "+edgeLabel);
        if ((v1 != null)&&(v2 != null)) {
            Iterable<Edge> result = v1.getEdges(v2, Direction.OUT, edgeLabel==null?"E":edgeLabel);
            for (Edge e : result) {
                LOGGER.log(Level.FINER, "Conectados por el edge: "+e.getId());
                connected = true;
                break;
            }
        }
        return connected;
    }
}
