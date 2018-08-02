/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.security;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Entity;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class AccessRight {
    private final static Logger LOGGER = Logger.getLogger(AccessRight.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.AccessRight);
        }
    }
    /**
     * Estados internos del objeto:
     * 0: sin acceso
     * 1: Read
     * 2: write
     * 4: delete
     * 8: list
     */ 
    public static final int NOACCESS = 0;
    public static final int ACCESSCONTROL    = 1;
    public static final int READ             = 1<<1;
    public static final int WRITE            = 1<<2;
    public static final int DELETE           = 1<<3;
    public static final int LIST             = 1<<4;
    public static final int PRINT            = 1<<5;
    
    public static final int FULLACCESS       = Integer.MAX_VALUE;
    
    private int rights = 0;

    public AccessRight() {
    }

    public AccessRight(int rights) {
        this.rights = rights;
    }

    public int getRights() {
        return rights;
    }

    public AccessRight setRights(int rights) {
        this.rights = rights;
        return this;
    }
    
    /**
     * Set the access right.
     * 
     * @param rights AccessRights a conceder.
     * @return return this instance.
     */
    public AccessRight setRights(int... rights) {
        this.rights = 0;
        // fusionar los valores
        for (int i: rights) {
            // la marca de sin acceso tiene prioridad sobre las demás.
            if (i==NOACCESS) {
                this.rights = NOACCESS;
                break;
            }
            this.rights|=i;
        }
        return this;
    }
    
}
