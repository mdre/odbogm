package net.odbogm.audit;

import net.odbogm.proxy.IObjectProxy;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface IAuditor {
    
    void auditLog(IObjectProxy o, int at, String label, Object data);

    void commit();
    
    void rollback();
    
}
