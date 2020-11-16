package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on an entity method to prevent the loading of lazy links
 * when the method is called. Otherwise, by default any method called on an entity
 * will trigger the load of lazy links.
 * 
 * @author jbertinetti
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DontLoadLinks {
}
