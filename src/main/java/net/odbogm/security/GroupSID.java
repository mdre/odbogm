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
public class GroupSID extends SID {
    private final static Logger LOGGER = Logger.getLogger(GroupSID.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    private List<SID> participants;
    
    public final void add(SID user) {
        if (participants == null) 
            this.participants = new ArrayList<>();
        this.participants.add(user);
    }
    
}
