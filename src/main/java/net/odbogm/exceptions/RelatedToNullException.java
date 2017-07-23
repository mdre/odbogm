/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.exceptions;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class RelatedToNullException extends RuntimeException{
    private static final long serialVersionUID = -6340905386895250014L;

    public RelatedToNullException(String message) {
        super(message);
    }

}
