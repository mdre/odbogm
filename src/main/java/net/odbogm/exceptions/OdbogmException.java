package net.odbogm.exceptions;

import com.orientechnologies.common.concur.ONeedRetryException;
import net.odbogm.Transaction;

/**
 *
 * @author jbertinetti
 */
public class OdbogmException extends RuntimeException {
    
    private boolean canRetry = false;
    
    public OdbogmException(String message) {
        super(message);
    }
    
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
        canRetry = cause instanceof ONeedRetryException;
    }
    
    /**
     * @return Indica si se puede reintentar la acción que se estaba ejecutando
     * para completarla.
     */
    public boolean canRetry() {
        return canRetry;
    }
}
