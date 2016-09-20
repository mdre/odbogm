/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Establece una vinculación entre dos objetos
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
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

    // por defecto realiza log solo sobre operaciones de write
    int log() default 4;
}
