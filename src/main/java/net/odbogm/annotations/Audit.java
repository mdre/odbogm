package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity as audited, ie. logs are saved when defined operations are
 * done against it.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Audit {

    public interface AuditType {

        public static final int READ = 1;
        public static final int WRITE = 2;
        public static final int DELETE = 4;
        public static final int ALL = 7;

    }

    //only write and delete operations logged by default
    int log() default AuditType.WRITE | AuditType.DELETE;
}
