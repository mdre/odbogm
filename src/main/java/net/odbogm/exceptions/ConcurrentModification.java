package net.odbogm.exceptions;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import net.odbogm.Transaction;

/**
 * Indica que un mismo elemento fue modificado por distintas transacciones a la
 * vez.
 * 
 * @author jbertinetti
 */
public class ConcurrentModification extends OdbogmException {
    
    public ConcurrentModification(OConcurrentModificationException cause,
            Transaction transaction) {
        super(cause, transaction);
    }
    
}
