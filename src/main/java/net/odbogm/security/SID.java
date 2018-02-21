/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.security;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SID {
    private final static Logger LOGGER = Logger.getLogger(SID.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private String name = "";
    private String uuid = "";
    
    @Deprecated
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

    public final SID setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    @Override
    public final String toString() {
        return "SID{" + "id=" + uuid + ", name="+this.name+"}";
    }
    
}
