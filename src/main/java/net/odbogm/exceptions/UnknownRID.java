package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class UnknownRID extends OdbogmException {

    public UnknownRID(Transaction transaction) {
        super(transaction);
    }

    public UnknownRID(String message, Transaction transaction) {
        super(message, transaction);
    }
}
