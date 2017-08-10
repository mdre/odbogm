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
    private Map<SID,AccessRight> _acl;
    
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
        if (_acl==null) {
            this._acl = new HashMap<>();
        }
        
        _acl.put(sid, ar);
            
        return this;
    }
    
    
    public final void removeAcl(SID sid) {
        if (_acl!=null)
            _acl.remove(sid);
    }
    
    public final SObject setOwner(SID o) {
        this._owner = o;
        return this;
    }
    
    public final void validate(AccessToken ar) {
        
    }
    
}
