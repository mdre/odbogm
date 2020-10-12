package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A long field marked with this annotation gets populated with the next value of
 * the configured sequence from the DB during the commit process. Only gets a
 * value from the DB if the field value is null.
 * 
 * TODO: consider this annotation in DBManger
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
