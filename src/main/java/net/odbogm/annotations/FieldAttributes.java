package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @FieldAttributes}: define varios attributos de los campos de acuerdo a
 * la cláusula ALTER PROPERTY.
 * 
 * LINKEDCLASS, the linked class name. Accepts a string as value. NULL to remove it.
 * LINKEDTYPE, the linked type name between those supported:Types. Accepts a string as value. NULL to remove it.
 * MIN, the minimum value as constraint. Accepts strings, numbers or dates as value. NULL to remove it.
 * MANDATORY, true if the property is mandatory. Accepts "true" or "false".
 * MAX, the maximum value as constraint. Accepts strings, numbers or dates as value. NULL to remove it.
 * NAME, the property name. Accepts a string as value.
 * NOTNULL, the property can't be null. Accepts "true" or "false".
 * REGEXP, the regular expression as constraint. Accepts a string as value. NULL to remove it.
 * TYPE, the type between those supported:Types. Accepts a string as value.
 * COLLATE, sets the collate to define the strategy of comparison. By default is case sensitive. 
 *          By setting it yo "ci", any comparison will be case-insensitive.
 * READONLY, the property value is immutable: it can't be changed after the first assignment. 
 *          Use this with DEFAULT to have immutable values on creation. Accepts "true" or "false".
 * CUSTOM, set custom properties. Syntax is {@code <name> = <value>}. Example: {@code stereotype = icon}
 * DEFAULT, set the default value. Default value can be a value or a function.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD) //on class level
public @interface FieldAttributes {
    public enum Bool{TRUE,FALSE,UNDEF}
    
    String linkedClass() default "";
    String linkedType() default "";
    String min() default "";
    String max() default "";
    Bool mandatory() default Bool.UNDEF;
//    String name() default "";
    Bool notNull() default Bool.UNDEF;
    String regexp() default "";
    String type() default "";
    String collate() default "";
    Bool readOnly() default Bool.UNDEF;
    String defaultVal() default "";
    
}
