package net.odbogm.proxy;

import java.util.Set;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ILazyCalls {
    boolean isDirty();
    void clearState();
    void rollback();
    void forceLoad();
    void updateAuditLogLabel(Set seen);
    String getRelationName();
}
