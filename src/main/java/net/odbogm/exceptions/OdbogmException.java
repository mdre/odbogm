package net.odbogm.exceptions;

import com.orientechnologies.common.concur.ONeedRetryException;
import net.odbogm.Transaction;

/**
 *
 * @author jbertinetti
 */
public class OdbogmException extends RuntimeException {
    
    private boolean canRetry = false;
    
    public OdbogmException(Transaction transaction) {
        shutdownTransaction(transaction);
    }
    
    public OdbogmException(String message, Transaction transaction) {
        super(message);
        shutdownTransaction(transaction);
    }

    public OdbogmException(Throwable cause, Transaction transaction) {
        super(cause.getMessage(), cause);
        shutdownTransaction(transaction);
        canRetry = cause instanceof ONeedRetryException;
    }
    
    private void shutdownTransaction(Transaction transaction) {
        transaction.shutdownInternalTx();
    }
    
    /**
     * @return Indica si se puede reintentar la acción que se estaba ejecutando
     * para completarla.
     */
    public boolean canRetry() {
        return canRetry;
    }
}
