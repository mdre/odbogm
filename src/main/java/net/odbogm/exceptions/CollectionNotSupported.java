/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.exceptions;

import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class CollectionNotSupported extends RuntimeException{
    private final static Logger LOGGER = Logger.getLogger(CollectionNotSupported.class .getName());
    private static final long serialVersionUID = -3559188020512888418L;

    public CollectionNotSupported() {
    }

    public CollectionNotSupported(String message) {
        super("La colección: "+message+" no está soportada.");
    }

}
