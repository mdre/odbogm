package net.odbogm.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Indexed;
import net.odbogm.annotations.Indirect;

@Entity
public final class UserSID implements ISID, ISecurityCredentials {
    private final static Logger LOGGER = Logger.getLogger(UserSID.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.UserSID);
        }
    }
    @Indexed(type = Indexed.IndexType.UNIQUE)
    private String name = "";
    @Indexed(type = Indexed.IndexType.UNIQUE)
    private String uuid = "";
    
    @Indirect(linkName = "GroupSID_participants")
    private List<GroupSID> groups = new ArrayList<>();
            
    public UserSID() {
        super();
    }
    
    public UserSID(String name, String uuid) {
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
        return "USID{" + "id=" + uuid + ", name="+this.name+"}";
    }
    
    public void addGroup(GroupSID gsid) {
        // ojo con las referencias cruzadas entre UserSID y GroupSID
        if (!this.groups.contains(gsid)) {
            this.groups.add(gsid);
            gsid.add(this);
        }
    }
    
    public void removeGroup(GroupSID gsid) {
        // ojo con las referencias cruzadas entre UserSID y GroupSID
        if (this.groups.remove(gsid)) {
            gsid.remove(this);
        }
    }
    
    /**
     * Retorna una lista con todos los UUID de los grupos a los que pertenece el usuario.
     * @return {@literal List<String::UUID>} lista de grupos a los que pertene el usuario.
     */
    @Override
    public List<String> showSecurityCredentials() {
        //uuid del UserSID actual
        List<String> sc = new ArrayList<>(List.of(uuid));
        //recuperar todos los grupos a los que pertenece el UserSID actual
        sc.addAll(this.groups.stream().map(gid -> gid.getUUID()).collect(Collectors.toList()));
        //grupos a los que pertenecen los grupos
        for (GroupSID group : this.groups) {
            sc.addAll(group.getIndirectCredentialsGroups());
        }
        return Collections.unmodifiableList(sc);
    }
    
    /**
     * Retorna una lista con todos los grupos a los que pertenece el usuario.
     * @return lista de GroupSID
     */
    public List<GroupSID> getGroups() {
        return Collections.unmodifiableList(this.groups);
    }

}
