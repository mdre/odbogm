package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A long field marked with this annotation gets populated with the next value of
 * the configured sequence from the DB during the store process.
 * 
 * @author jbertinetti
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sequence {
    
    /**
     * Name of the DB sequence to use.
     * @return 
     */
    String sequenceName();
    
}
