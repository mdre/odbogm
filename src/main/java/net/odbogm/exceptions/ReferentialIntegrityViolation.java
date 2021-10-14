package net.odbogm.exceptions;

import com.orientechnologies.orient.core.record.OVertex;
import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ReferentialIntegrityViolation extends OdbogmException {
    
    public ReferentialIntegrityViolation(OVertex referencedVertex, Transaction transaction) {
        super(String.format("The vertex %s still has incoming references.",
                referencedVertex.getIdentity()), transaction);
    }


    public ReferentialIntegrityViolation(String message, Transaction transaction) {
        super(message, transaction);
    }
    
}
