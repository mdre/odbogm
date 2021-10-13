package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class UnmanagedObject extends OdbogmException {

    public UnmanagedObject(Transaction transaction) {
        super(transaction);
    }
    
}
