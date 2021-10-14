package net.odbogm.exceptions;

import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class CollectionNotSupported extends OdbogmException {
    
    public CollectionNotSupported(String col, Transaction t) {
        super(String.format("Collection of type %s not supported.", col), t);
    }

}
