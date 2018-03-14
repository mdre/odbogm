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
import java.util.stream.Collectors;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Entity;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class GroupSID extends SID {
    private final static Logger LOGGER = Logger.getLogger(GroupSID.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.GroupSID);
        }
    }
    private List<SID> participants;

    public GroupSID() {
        super();
    }
    
    public GroupSID(String name, String uuid) {
        super(name, uuid);
    }
    
    /**
     * <div class="en">Add a user or group to this group.</div>
     * <div class="es">Agrega un usuario o grupo.</div>
     * 
     * @param user reference to user.
     */
    public final void add(SID user) {
        if (participants == null) 
            this.participants = new ArrayList<>();
        this.participants.add(user);
    }
    
    public final void remove(SID user) {
        this.participants.remove(user);
    }
    
    public final List<SID> getParticipants() {
        // FIXME: ojo que se está retornando la lista de participantes y esto permite que se acceda a los objetos
        // internos de la misma.
        return this.participants.stream().map(sid -> sid).collect(Collectors.toList());
    }
    
}