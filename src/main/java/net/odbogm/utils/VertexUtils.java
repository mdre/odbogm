/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.utils;

import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
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
     * @param edgeLabel etiqueta del edge
     * @return true if any conection exist
     */
    public static boolean areConected(OVertex v1, OVertex v2, String edgeLabel) {
        boolean connected = false;
        if ((v1 != null)&&(v2 != null)) {
            Iterable<OVertex> result = v1.getVertices(ODirection.BOTH.BOTH, edgeLabel==null?"E":edgeLabel);
            for (OVertex ov : result) {
                if (ov.getIdentity().toString().equals(v2.getIdentity().toString())) {
                    connected = true;
                    break;
                }
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
    public static boolean isConectedTo(OVertex v1, OVertex v2, String edgeLabel) {
        boolean connected = false;
        LOGGER.log(Level.FINER, "Verificando edges entre {0} y {1} a través de la realción {2}", new Object[]{v1.getIdentity(), v2.getIdentity(), edgeLabel});
        Iterable<OEdge> result = v1.getEdges(ODirection.OUT, edgeLabel==null?"E":edgeLabel);
        for (OEdge oe : result) {
            if (oe.getTo().getIdentity().toString().equals(v2.getIdentity().toString())) {
                LOGGER.log(Level.FINER, "Conectados por el edge: {0}", oe.getIdentity());
                connected = true;
                break;
            }
        }
        return connected;
    }
    
    /**
     * Helper class to fill elements with properties throught a Map
     * MMAPI does has not have that funcionality as provided by Tinkerpop
     * Helper para asignación masiva de campos a través de un hashmap. 
     * MMAPI no tiene esa funcionalidad como la proveía Tinkerpop
     * @param oe     vértice a completar con los datos
     * @param omap  pares de valores con el nombre de los campos y los valores a asignar.
     */
    public static void fillElement(OElement oe, Map<String,Object> omap) {
        omap.entrySet().stream().forEach(entry->{
            oe.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
