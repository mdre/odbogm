package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author jbertinetti
 */
public class OdbogmException extends RuntimeException {
    
    public OdbogmException(Transaction transaction) {
        transaction.closeInternalTx();
    }
    
    public OdbogmException(String message, Transaction transaction) {
        super(message);
        transaction.closeInternalTx();
    }

    public OdbogmException(Throwable cause, Transaction transaction) {
        super(cause.getMessage(), cause);
        transaction.closeInternalTx();
    }
    
}
