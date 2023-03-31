package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A list attribute annotated with this will cause that collection to be
 * used only for adding new items to the relation. This collection will never
 * load its items, but the newly added items will be persisted.
 * 
 * Use this annotation when it's required to create new relations but loading
 * all elements in a normal collection it's expensive because of a large number
 * of them.
 * 
 * @author jbertinetti
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface OnlyAdd {
    
    String attribute() default "";
    
}
