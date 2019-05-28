package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Al anotar un campo String de una entidad con esta anotación, automáticamente
 * en el mismo será inyectado el RID del vértice asociado.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RID {
}
