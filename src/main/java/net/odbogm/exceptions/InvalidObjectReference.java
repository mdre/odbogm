package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class InvalidObjectReference extends OdbogmException {
    
    public InvalidObjectReference(Transaction t) {
        super("The NEW object has been rolledback and does no longer exist.", t);
    }
    
}
