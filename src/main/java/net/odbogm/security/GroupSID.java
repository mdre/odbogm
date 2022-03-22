package net.odbogm.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Indexed;
import net.odbogm.annotations.Indirect;
import net.odbogm.exceptions.CircularReferenceException;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public final class GroupSID implements ISID {
    private final static Logger LOGGER = Logger.getLogger(GroupSID.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.GroupSID);
        }
    }
    @Indexed(type = Indexed.IndexType.UNIQUE)
    private String name = "";
    @Indexed(type = Indexed.IndexType.UNIQUE)
    private String uuid = "";
    
    private final List<ISID> participants = new ArrayList<>();;
    
    // list of groups to which it was added
    @Indirect(linkName = "GroupSID_participants")
    private final List<GroupSID> addedTo = new ArrayList<>();

    public GroupSID() {
        super();
    }
    
    public GroupSID(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }
    
    @Override
    public final String getName() {
        return name;
    }
    
    @Override
    public final void setName(String name) {
        this.name = name;
    }

    @Override
    public final String getUUID() {
        return uuid;
    }

    @Override
    public final void setUUID(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public final String toString() {
        return "GSID{" + "id=" + uuid + ", name="+this.name+"}";
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
        if (isAncestor(gsid)) throw new CircularReferenceException();
        if (!this.participants.contains(gsid)) {
            this.participants.add(gsid);
            gsid.addedTo(this);
        }
    }
    
    private boolean isAncestor(GroupSID gsid) {
        if (Objects.equals(this, gsid)) return true;
        return this.addedTo.stream().anyMatch(g -> g.isAncestor(gsid));
    }
    
    public final void remove(UserSID user) {
        if (this.participants.remove(user)) {
            user.removeGroup(this);
        }
    }
    
    public final boolean remove(GroupSID group) {
        boolean ok = this.participants.remove(group);
        if (ok) group.removeAddedTo(this);
        return ok;
    }
    
    public final List<ISID> getParticipants() {
        return Collections.unmodifiableList(this.participants);
    }
    
    public final void addedTo(GroupSID gAddedTo) {
        if (!this.addedTo.contains(gAddedTo)) {
            this.addedTo.add(gAddedTo);
        }
    }
    
    public final void removeAddedTo(GroupSID gAddedTo) {
        this.addedTo.remove(gAddedTo);
    }
    
    // devuelve las credenciales de todos los grupos a los que fue agregado este grupo.
    final List<String> getIndirectCredentialsGroups() {
        Set<String> indirect = getIndirectCredentialsGroups(new HashSet<>());
        return new ArrayList<>(indirect);
    }
    
    /**
     * To detect loops in IndirectCredentials we use a fordward aproach to preserve
     * what was already check.
     * 
     * @param indirect
     * @return 
     */
    private Set<String> getIndirectCredentialsGroups(Set<String> indirect) {
        for (GroupSID gsid : this.addedTo) {
            if (!indirect.contains(gsid.getUUID())) {
                indirect.add(gsid.getUUID());
                indirect.addAll(gsid.getIndirectCredentialsGroups(indirect));
            }
        }
        return indirect;
    }
    
}