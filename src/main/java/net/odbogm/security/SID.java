/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.security;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public abstract class SID {
    private final static Logger LOGGER = Logger.getLogger(SID.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private String name = "";
    private String uuid = "";
            
    public SID() {
    }

    public SID(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }
    
    public final String getName() {
        return name;
    }

    public final SID setName(String name) {
        this.name = name;
        return this;
    }

    public final String getUUID() {
        return uuid;
    }

    public final SID setId(String uuid) {
        this.uuid = uuid;
        return this;
    }

    @Override
    public final String toString() {
        return "SID{" + "id=" + uuid + ", name="+this.name+"}";
    }
    
}
