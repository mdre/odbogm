package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Establece una vinculación entre dos objetos
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Entity {
    String name() default "";
    boolean isEdgeClass() default false;
}
