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
    private List<SID> participants = new ArrayList<>();;
    
    // lista de grupo a los que fue agregado el presente
    private List<GroupSID> addedTo = new ArrayList<>();

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
    public final void add(UserSID user) {
        // verificar que el SID no exista
        if (!this.participants.contains(user)) {
            this.participants.add(user);
            user.addGroup(this);
        }
    }
    
    public final void add(GroupSID gsid) {
        // verificar que el SID no exista
        if (!this.participants.contains(gsid)) {
            this.participants.add(gsid);
            gsid.addedTo(this);
        }
    }
    
    public final void remove(SID user) {
        if (this.participants.remove(user)) {
            // si lo que se está removiendo es un Grupo, eliminar la doble referencia.
            if (user instanceof GroupSID) {
                ((GroupSID)user).removeAddedTo(this);
            } else {
                ((UserSID)user).removeGroup(this);
            }
        }
    }
    
    public final List<SID> getParticipants() {
        // FIXME: ojo que se está retornando la lista de participantes y esto permite que se acceda a los objetos
        // internos de la misma.
        List<SID> p = new ArrayList<>();
        if (this.participants != null) {
             p = this.participants.stream().map(sid -> sid).collect(Collectors.toList());
        }
        return p;
    }
    
    final void addedTo(GroupSID gAddedTo) {
        if (!this.addedTo.contains(gAddedTo)) {
            this.addedTo.add(gAddedTo);
        }
    }
    
    final void removeAddedTo(GroupSID gAddedTo) {
        this.addedTo.remove(gAddedTo);
    }
    
    // devuelve las credenciales de todos los grupos a los que fue agregado este grupo.
    final List<String> getIndirectCredentialsGroups() {
        ArrayList<String> indirect = new ArrayList<>();
        for (GroupSID gsid : this.addedTo) {
            indirect.add(gsid.getUUID());
            indirect.addAll(gsid.getIndirectCredentialsGroups());
        }
        return indirect;
    }
}