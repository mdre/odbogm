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
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ClassIndex {
    public enum IndexType{UNIQUE,NOTUNIQUE,FULLTEXT,LUCENE}
    
    /**
     * The full create index order to be used
     * Ej: CREATE INDEX addresses ON Employee (address) NOTUNIQUE METADATA {ignoreNullValues: true}
     * @return the index expression string.
     */
    String indexExpr();
    
    
}
