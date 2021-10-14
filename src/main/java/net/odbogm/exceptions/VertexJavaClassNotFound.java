package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class VertexJavaClassNotFound extends OdbogmException {

    public VertexJavaClassNotFound(Transaction t) {
        super("The vertex class doesn't have the custom attribute 'javaClass'.", t);
    }

}
