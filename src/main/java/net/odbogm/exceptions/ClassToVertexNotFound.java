package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ClassToVertexNotFound extends OdbogmException {

    public ClassToVertexNotFound(String message, Transaction t) {
        super(message, t);
    }

}
