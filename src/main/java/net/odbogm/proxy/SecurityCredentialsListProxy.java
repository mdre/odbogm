package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.ODirection;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.odbogm.Transaction;

/**
 * Read-only collection of security credentials. It gathers all credentials with a
 * single query so it removes the need to load and traverse all entities to calculate
 * the list, making it more efficient (specially when the groups and user graph
 * is complex and many levels of depth).
 * 
 * @author jbertinetti
 */
public class SecurityCredentialsListProxy extends ArrayList<String> implements ILazyCollectionCalls {

    private static final Logger LOGGER = Logger.getLogger(SecurityCredentialsListProxy.class.getName());

    private Transaction transaction;
    private String relation;
    private WeakReference<IObjectProxy> parent;
    private boolean loaded = false;


    public SecurityCredentialsListProxy() {
    }

    @Override
    public void init(Transaction tx, IObjectProxy parent, String field, Class<?> c, ODirection d) {
        this.transaction = tx;
        this.relation = field;
        this.parent = new WeakReference<>(parent);
    }

    private synchronized void refill() {
        //load elements in one shot
        if (!this.loaded) {
            var theParent = this.parent.get();
            if (theParent != null) {
                LOGGER.finest("Filling SecurityCredentialsListProxy...");
                super.clear();
                this.transaction.initInternalTx();
                try (var res = this.transaction.getCurrentGraphDb().query(String.format(
                        "match {rid: %s, as: u}<-GroupSID_participants-{as: g, while: (true)} return distinct g.uuid",
                        theParent.___getRid()))) {
                    res.stream().map(r -> r.getProperty("g.uuid").toString()).forEach(super::add);
                }
                this.transaction.closeInternalTx();
            }
            this.loaded = true;
        }
    }

    @Override
    public int size() {
        refill();
        return super.size();
    }

    @Override
    public Map<Object, ObjectCollectionState> collectionState() {
        return Map.of();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void clearState() {
    }

    @Override
    public void rollback() {
    }

    @Override
    public void forceLoad() {
    }

    @Override
    public void updateAuditLogLabel(Set seen) {
    }

    @Override
    public String getRelationName() {
        return this.relation;
    }

}
