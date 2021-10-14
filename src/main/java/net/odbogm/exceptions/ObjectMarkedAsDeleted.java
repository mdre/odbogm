package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectMarkedAsDeleted extends OdbogmException {
    
    public ObjectMarkedAsDeleted(String message, Transaction t) {
        super(message, t);
    }

}
