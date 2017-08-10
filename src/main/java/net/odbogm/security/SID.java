/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.security;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public class SID {
    private final static Logger LOGGER = Logger.getLogger(SID.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private String name = "";
    private String id = "";

    public SID() {
    }

    public final String getName() {
        return name;
    }

    public final SID setName(String name) {
        this.name = name;
        return this;
    }

    public final String getId() {
        return id;
    }

    public final SID setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public final String toString() {
        return "SID{" + "id=" + id + '}';
    }
    
}
