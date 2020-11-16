package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Eager load of entity links.
 * 
 * A field marked with this annotation will be loaded during the process of getting
 * the entity from database. If the field is a collection, all links are loaded.
 * 
 * If a class is marked with this annotation, all its links are loaded eagerly
 * including all the collections.
 * 
 * Indirect links are also considered with this annotation.
 * 
 * @author jbertinetti
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Eager {
}
