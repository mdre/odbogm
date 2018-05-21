/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.security;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Indirect;

@Entity
public final class UserSID extends SID implements ISecurityCredentials {
    private final static Logger LOGGER = Logger.getLogger(UserSID.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.UserSID);
        }
    }
    @Indirect(linkName = "GroupSID_participants")
    private List<GroupSID> groups = new ArrayList<>();;
            
    public UserSID() {
        super();
    }
    
    public UserSID(String name, String uuid) {
        super(name, uuid);
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
        // recuperar todos los grupos a los que pertenece el UserSID actual.
        // FIXME: la lista retornada debería ser inmutable.
        List<String> sc = new ArrayList<>();
        if (this.groups != null) {
            sc = this.groups.stream().map(gid -> gid.getUUID()).collect(Collectors.toList());
        }
        for (GroupSID group : this.groups) {
            sc.addAll(group.getIndirectCredentialsGroups());
        }
        return sc;
    }
    
    /**
     * Retorna una lista con todos los grupos a los que pertenece el usuario.
     * @return lista de GroupSID
     */
    public List<GroupSID> getGroups() {
        // FIXME: existe algún riesgo en esto?
        List<GroupSID> gr = new ArrayList<>();
        if (this.groups!=null) {
             gr = this.groups.stream().map(gid -> gid).collect(Collectors.toList());
        }
        return gr;
    }

}
