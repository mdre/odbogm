package net.odbogm.exceptions;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ReferentialIntegrityViolation extends OdbogmException {
    
    public ReferentialIntegrityViolation(OrientVertex referencedVertex, Transaction transaction) {
        super(String.format("El vértice %s aún tiene referencias entrantes.",
                referencedVertex.getId()), transaction);
    }


    public ReferentialIntegrityViolation(String message, Transaction transaction) {
        super(message, transaction);
    }
    
}
