/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.exceptions;

import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public class ReferentialIntegrityViolation extends RuntimeException{
    private final static Logger LOGGER = Logger.getLogger(ReferentialIntegrityViolation.class .getName());
    private static final long serialVersionUID = 4808259721082709371L;
}
