package net.odbogm.exceptions;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class RelatedToNullException extends RuntimeException{
    private static final long serialVersionUID = -6340905386895250014L;

    public RelatedToNullException(String message) {
        super(message);
    }

}
