/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.security;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public abstract class SObject {
    private final static Logger LOGGER = Logger.getLogger(SObject.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private SID _owner;
    // Access Control List
    private Map<String,Integer> _acl = new HashMap<>();;
    
    /**
     * Estados internos del objeto:
     * 0: sin acceso
     * 1: Read
     * 2: write
     * 4: delete
     * 8: list
     */ 
    private int state = 0;
    private boolean inherit = false;

    
    public SObject() {
    }
    
    public SObject(SID _owner) {
        this._owner = _owner;
    }
    
    void setState(int s) {
        this.state = s;
    }
    
    int getState() {
        return this.state;
    }
    
    
    /**
     * Add or update de AccessRight for the specified SID.
     * 
     * @param sid the Security ID 
     * @param ar the AccessRight to set
     * @return this SObject reference
     */
    public final SObject setAcl(SID sid,AccessRight ar)  {
        
        _acl.put(sid.getUUID(), ar.getRights());
            
        return this;
    }
    
    
    public final void removeAcl(SID sid) {
        if (_acl!=null)
            _acl.remove(sid.getUUID());
    }
    
    public final SObject setOwner(SID o) {
        this._owner = o;
        return this;
    }
    
    /**
     * Validate all groups aganis the acls and return the final state of the object
     * @param sc 
     */
    public final int validate(ISecurityCredentials sc) {
        int partialState = 0;
        int gal = 0;
        for (String securityCredential : sc.showSecurityCredentials()) {
            gal = this._acl.get(securityCredential);
            if (gal == AccessRight.NOACCESS) {
                partialState = 0;
                break;
            }
            partialState |= gal;
        }
        this.state = partialState;
        return this.state;
    }
    
    /**
     * Devuelve el estado de seguridad actual del objeto.
     * @return 
     */
    public final int getSecurityState() {
        return this.state;
    }
    
}
