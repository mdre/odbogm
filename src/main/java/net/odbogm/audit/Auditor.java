package net.odbogm.audit;

import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
        
        // verificar que la clase de auditoría exista
        if (this.transaction.getDBClass(ODBAUDITLOGVERTEXCLASS) == null) {
            OrientGraph odb = this.transaction.getGraphdb();
            OrientVertexType olog = odb.createVertexType(ODBAUDITLOGVERTEXCLASS);
            olog.createProperty("rid", OType.STRING);
            olog.createProperty("timestamp", OType.DATETIME);
            olog.createProperty("user", OType.STRING);
            olog.createProperty("action", OType.STRING);
            olog.createProperty("label", OType.STRING);
            olog.createProperty("log", OType.STRING);
            odb.commit();
            odb.shutdown();
        }
    }
    

    /**
     * Realiza una auditoría a partir del objeto indicado.
     *
     * @param o IOBjectProxy a auditar
     * @param at AuditType
     * @param label Etiqueta de referencia
     * @param data objeto a loguear con un toString
     */
    @Override
    public synchronized void auditLog(IObjectProxy o, int at, String label, Object data) {
        // guardar log de auditoría si corresponde.
        if (o.___getBaseClass().isAnnotationPresent(Audit.class)) {
            int logVal = o.___getBaseClass().getAnnotation(Audit.class).log();
            if ((logVal & at) > 0) {
                this.logdata.add(new LogData(o, at, label, data));
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
        
        OrientGraph odb = this.transaction.getGraphdb();
        
        for (LogData logData : logdata) {
            LOGGER.log(Level.FINER, "valid: {0} : {1}", new Object[]{logData.o.___isValid(), logData.rid});
            Map<String, Object> ologData = new HashMap<>();
            ologData.put("transactionID", ovLogID);
            ologData.put("rid", (logData.o.___isValid()&!logData.o.___isDeleted()?logData.o.___getRid():logData.rid));
            ologData.put("timestamp", DateHelper.getCurrentDateTime());
            ologData.put("user", this.auditUser);
            ologData.put("action", logData.auditType);
            ologData.put("label", logData.label);
            ologData.put("log", logData.odata != null ? logData.odata.toString() : logData.data);
            
            odb.addVertex("class:" + ODBAUDITLOGVERTEXCLASS, ologData);
        }
        
        odb.commit();
        odb.shutdown();
        this.logdata.clear();
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
            //keep the element if it's new, so we can save the final rid and not the temporary:
            if (data instanceof OrientElement && ((OrientElement)data).getIdentity().isNew()) {
                this.odata = data;
            } else {
                this.data = data.toString();
            }
        }
    }
}