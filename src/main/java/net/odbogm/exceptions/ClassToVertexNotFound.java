package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ClassToVertexNotFound extends OdbogmException {

    public ClassToVertexNotFound(String className, Transaction t) {
        super(String.format("No class definition found in database for class %s.", className), t);
    }

}
