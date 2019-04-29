package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class UnknownObject extends OdbogmException {

    public UnknownObject(Transaction transaction) {
        super(transaction);
    }
    
}
