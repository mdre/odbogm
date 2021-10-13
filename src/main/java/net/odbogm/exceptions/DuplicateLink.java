package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class DuplicateLink extends OdbogmException {

    public DuplicateLink(Transaction transaction) {
        super(transaction);
    }
    
}
