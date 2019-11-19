package net.odbogm.exceptions;

/**
 *
 * @author jbertinetti
 */
public class CircularReferenceException extends RuntimeException {
    
    public CircularReferenceException() {
        super("The GroupSID to add as a child is already an ancestor.");
    }
    
}
