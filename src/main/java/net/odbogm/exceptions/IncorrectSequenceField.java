package net.odbogm.exceptions;

/**
 * Exception that indicates that a non Long field was annotated with @Sequence.
 * 
 * @author jbertinetti
 */
public class IncorrectSequenceField extends OdbogmException {
    
    public IncorrectSequenceField() {
        super("A field annotated with @Sequence must be of type Long.");
    }
    
}
