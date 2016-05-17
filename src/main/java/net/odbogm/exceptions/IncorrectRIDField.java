/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.exceptions;

import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class IncorrectRIDField extends RuntimeException {
    private final static Logger LOGGER = Logger.getLogger(IncorrectRIDField.class .getName());
    private static final long serialVersionUID = 5098816851596315463L;

    public IncorrectRIDField() {
    }
}
