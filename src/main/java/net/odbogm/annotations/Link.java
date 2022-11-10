package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Relation to another object embedded in vertex (attribute of type LINK).
 * 
 * For now, @RemoveOrphan or @CascadeDelete can't be used with this.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Link {

}
