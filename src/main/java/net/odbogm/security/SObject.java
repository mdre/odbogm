package net.odbogm.security;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Ignore;

@Entity
public abstract class SObject {
    
    public static final int OTHERS_DEFAULT_ACCESS = AccessRight.READ;
    
    private static final String OTHERS_UUID = "__OTHERS__";

    private final static Logger LOGGER = Logger.getLogger(SObject.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.SObject);
        }
    }
    
    private ISID __owner;
    
    /** Access Control List */
    private final Map<String, Integer> __acl = new HashMap<>();
    
    /**
     * Estados internos del objeto:
     * 0: sin acceso
     * 1: Read
     * 2: write
     * 4: delete
     * 8: list
     */ 
    @Ignore
    private int __state = 0;
    
    /** Para que herede los ACL de este otro objeto */
    private SObject __inherit = null;

    
    public SObject() {
    }

    public SObject(ISID owner) {
        this.__owner = owner;
    }
    
    public SObject(UserSID owner) {
        this.__owner = owner;
    }
    
    public SObject(GroupSID owner) {
        this.__owner = owner;
    }

    protected final void setState(int s) {
        this.__state = s;
    }
    
    protected final int getState() {
        return this.__state;
    }

    /**
     * Adds or updates the AccessRight for the specified SID.
     *
     * @param sid the Security ID
     * @param ar the AccessRight to set
     * @return this SObject reference
     */
    public final SObject setAcl(ISID sid, AccessRight ar) {
        __acl.put(sid.getUUID(), ar.getRights());
        return this;
    }
    
    public final SObject setAcl(GroupSID sid, AccessRight ar) {
        __acl.put(sid.getUUID(), ar.getRights());
        return this;
    }
    
    public final SObject setAcl(UserSID sid, AccessRight ar) {
        __acl.put(sid.getUUID(), ar.getRights());
        return this;
    }

    /**
     * Removes the AccessRight for the specified SID.
     * 
     * @param sid 
     */
    public final void removeAcl(ISID sid) {
        __acl.remove(sid.getUUID());
    }
    
    public final void removeAcl(GroupSID sid) {
        __acl.remove(sid.getUUID());
    }
    
    public final void removeAcl(UserSID sid) {
        __acl.remove(sid.getUUID());
    }

    /**
     * Sets the AccessRight for others.
     * 
     * @param ar
     * @return 
     */
    public final SObject setOthersAcl(AccessRight ar) {
        __acl.put(OTHERS_UUID, ar.getRights());
        return this;
    }
    
    /**
     * Removes the AccessRight for others.
     * 
     * @return 
     */
    public final SObject removeOthersAcl() {
        __acl.remove(OTHERS_UUID);
        return this;
    }
    
    public final SObject setOwner(UserSID o) {
        this.__owner = o;
        return this;
    }

    public final ISID getOwner() {
        return this.__owner;
    }
    
    /**
     * Validate all groups against the object's acls and return the final state of the object.
     * The state of the object is changed.
     *
     * @param sc SecurityCredential
     * @return the security state computed.
     */
    public final int validate(ISecurityCredentials sc) {
        this.__state = validate(this.getAcls(), sc);
        return this.__state;
    }
    
    /**
     * Computes and returns the security state validating all groups against the object's acls.
     * The state of the object is not changed.
     * 
     * @param sc
     * @return 
     */
    protected final int computeSecurityState(ISecurityCredentials sc) {
        return validate(this.getAcls(), sc);
    }
    
    /**
     * Validate all groups of the ISecurityCredentials against the given acls
     * and return the calculated security state.
     * 
     * @param acls
     * @param sc
     * @return 
     */
    protected final int validate(Map<String, Integer> acls, ISecurityCredentials sc) {
        LOGGER.log(Level.FINER, "validando los permisos de acceso...");
        int partialState = 0;
        Integer gal = 0;
        LOGGER.log(Level.FINER, "Lista de acls: {0} : {1}", new Object[]{acls.size(), acls});
        boolean hasGal = false;
        if (!acls.isEmpty()) {
            for (String securityCredential : sc.showSecurityCredentials()) {
                gal = acls.get(securityCredential);
                LOGGER.log(Level.FINER, "SecurityCredential access: {0} {1}",
                        new Object[]{securityCredential, gal});
                if (gal != null) {
                    hasGal = true;
                    if (gal == AccessRight.NOACCESS) {
                        partialState = 0;
                        break;
                    }
                    partialState |= gal;
                }
            }
            if (!hasGal) {
                //si no hay ACL para sc, es considerado OTHER
                partialState = acls.getOrDefault(OTHERS_UUID, OTHERS_DEFAULT_ACCESS);
            }
        } else {
            // si no hay ACLs definidos, se conceden todos los permisos por defectos.
            partialState = AccessRight.FULLACCESS;
        }
        return partialState;
    }
    
    /**
     * Returns the current security state of the object.
     *
     * @return Current value.
     */
    public final int getSecurityState() {
        return this.__state;
    }

    /**
     * Retorna una copia de los ACLs establecidos para el objeto.
     *
     * @return {@literal Map<String,Integer>} de los acls
     */
    public final HashMap<String, Integer> getAcls() {
        HashMap<String, Integer> acls = new HashMap<>();

        if (this.__inherit != null) {
            acls.putAll(this.__inherit.getAcls());
        }
        acls.putAll(this.__acl);

        return acls;
    }

    /**
     * Establece el objecto desde el que se heredan los permisos.
     *
     * @param so objecto desde el que se heredan los permisos.
     */
    public final void setInheritFrom(SObject so) {
        this.__inherit = so;
    }
}
