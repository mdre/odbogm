package net.odbogm.exceptions;

import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectMarkedAsDeleted extends RuntimeException{
    private final static Logger LOGGER = Logger.getLogger(ObjectMarkedAsDeleted.class .getName());
    private static final long serialVersionUID = -8295794538949106123L;

    public ObjectMarkedAsDeleted() {
    }

    public ObjectMarkedAsDeleted(String message) {
        super(message);
    }

}
