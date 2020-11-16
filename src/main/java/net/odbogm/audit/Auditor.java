package net.odbogm.audit;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.Transaction;
import net.odbogm.annotations.Audit;
import net.odbogm.proxy.IObjectProxy;
import net.odbogm.utils.DateHelper;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class Auditor implements IAuditor {
    
    private final static Logger LOGGER = Logger.getLogger(Auditor.class .getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.Auditor);
        }
    }
    
    private final static String ODBAUDITLOGVERTEXCLASS = "ODBAuditLog";
    private final Transaction transaction;
    private final String auditUser;
    private final ArrayList<LogData> logdata = new ArrayList<>();
    
    
    public Auditor(Transaction t, String user) {
        this.transaction = t;
        this.auditUser = user;
        
        //verify and create the audit class in schema if necessary:
        if (this.transaction.getSessionManager().getConfig().isAuditorCreatesAuditSchema()) {
            if (this.transaction.getDBClass(ODBAUDITLOGVERTEXCLASS) == null) {
                ODatabaseSession odb = this.transaction.getSessionManager().getDBTx();
                OClass olog = odb.createClass(ODBAUDITLOGVERTEXCLASS, "V");
                olog.createProperty("rid", OType.STRING);
                olog.createProperty("timestamp", OType.DATETIME);
                olog.createProperty("transactionID", OType.STRING);
                olog.createProperty("opInTx", OType.INTEGER);
                olog.createProperty("user", OType.STRING);
                olog.createProperty("action", OType.INTEGER);
                olog.createProperty("label", OType.STRING);
                olog.createProperty("log", OType.STRING);
                odb.close();
            }
        }
    }
    

    /**
     * Realiza una auditoría a partir del objeto indicado.
     *
     * @param o IOBjectProxy a auditar
     * @param auditType AuditType
     * @param label Etiqueta de referencia
     * @param data objeto a loguear con un toString
     */
    @Override
    public synchronized void auditLog(IObjectProxy o, int auditType, String label, Object data) {
        // guardar log de auditoría si corresponde.
        if (o.___getBaseClass().isAnnotationPresent(Audit.class)) {
            int logVal = o.___getBaseClass().getAnnotation(Audit.class).log();
            if ((logVal & auditType) > 0) {
                this.logdata.add(new LogData(o, auditType, label, data));
                LOGGER.log(Level.FINER, "objeto auditado");
            } else {
                LOGGER.log(Level.FINER, "No corresponde auditar");
            }
        } else {
            LOGGER.log(Level.FINER, "No auditado: {0}", o.___getBaseClass().getSimpleName());
        }
    }
    
    
    @Override
    public void commit() {
        // crear un UUDI para todo el log a comitear.
        String ovLogID = UUID.randomUUID().toString();
        ODatabaseSession odb = this.transaction.getCurrentGraphDb();
        
        //ODatabaseSession odb = this.transaction.getSessionManager().getDBTx();
        int opInTx = 0; //operation number in transaction
        
        for (LogData logData : logdata) {
            opInTx++;
            LOGGER.log(Level.FINER, "valid: {0} : deleted: {1}  : o.getRid: {2}   : log.rid: {3}", 
                                        new Object[]{logData.o.___isValid(),
                                                     logData.o.___isDeleted(), 
                                                     logData.o.___isDeleted()?"-deleted-":logData.o.___getRid(), 
                                                     logData.rid}
                       );
            OVertex ologData = odb.newVertex(ODBAUDITLOGVERTEXCLASS);

            ologData.setProperty("transactionID", ovLogID);
            ologData.setProperty("opInTx", opInTx);
            ologData.setProperty("rid", (logData.o.___isValid() &
                                         !logData.o.___isDeleted() 
                                        ?logData.o.___getRid():logData.rid));
            ologData.setProperty("timestamp", DateHelper.getCurrentDateTime());
            ologData.setProperty("user", this.auditUser);
            ologData.setProperty("action", logData.auditType);
            ologData.setProperty("label", logData.label);
            ologData.setProperty("log", logData.odata != null ? logData.odata.toString() : logData.data);
            ologData.save();
        }
        
        //odb.commit();
        //odb.close();
        
        //current.activateOnCurrentThread();
        this.logdata.clear();
    }
    
    
    @Override
    public void rollback() {
        //discard the entries that aren't reads
        new ArrayList<>(logdata).forEach(l -> {
            if (l.auditType != Audit.AuditType.READ) {
                this.logdata.remove(l);
            }
        });
    }
}

class LogData {
    public IObjectProxy o;
    public String rid;
    public int auditType;
    public String label;
    public String data;
    public Object odata;

    public LogData(IObjectProxy o, int auditType, String label, Object data) {
        this.o = o;
        this.rid = o.___getVertex().getIdentity().toString();
        this.auditType = auditType;
        this.label = label;
        if (data == null) {
            this.data = "";
        } else {
            //keep the element if it's new, so we can save the final rid and not the temporary in the "log" field:
            if (data instanceof OElement && ((OElement)data).getIdentity().isNew()) {
                this.odata = data;
            } else {
                this.data = data.toString();
            }
        }
    }
}