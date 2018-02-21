/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.auditory;

import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;
import net.odbogm.SessionManager;
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
        LOGGER.setLevel(LogginProperties.Auditor);
    }
    
    private Transaction transaction;
    private String auditUser;
    private ArrayList<LogData> logdata = new ArrayList<>();
    private final String ODBAUDITLOGVERTEXCLASS = "ODBAuditLog";
    
    public Auditor(Transaction t, String user) {
        this.transaction = t;
        this.auditUser = user;
        
        // verificar que la clase de auditorías exista
        if (this.transaction.getDBClass(this.ODBAUDITLOGVERTEXCLASS) == null) {
            OrientVertexType olog = this.transaction.getGraphdb().createVertexType(this.ODBAUDITLOGVERTEXCLASS);
            olog.createProperty("rid", OType.STRING);
            olog.createProperty("timestamp", OType.DATETIME);
            olog.createProperty("user", OType.STRING);
            olog.createProperty("action", OType.STRING);
            olog.createProperty("label", OType.STRING);
            olog.createProperty("log", OType.STRING);
            this.transaction.commit();
        }
        
    }
    

    /**
     * realiza una auditoría a partir del objeto indicado.
     *
     * @param o IOBjectProxy a auditar
     * @param at AuditType
     * @param data objeto a loguear con un toString
     */
    @Override
    public synchronized void auditLog(IObjectProxy o, int at, String label, Object data) {
        // guardar log de auditoría si corresponde.
        if (o.___getBaseClass().isAnnotationPresent(Audit.class)) {
            int logVal = o.___getBaseClass().getAnnotation(Audit.class).log();
            if ((logVal & at) > 0) {
                this.logdata.add(new LogData(o.___getRid(), at, label, data));
                LOGGER.log(Level.FINER, "objeto auditado");
            } else {
                LOGGER.log(Level.FINER, "No auditado por no corresponder");
            }
        } else {
            LOGGER.log(Level.FINER, "No auditado: " + o.___getBaseClass().getSimpleName());
        }
    }
    
    @Override
    public void commit() {
        // crear un UUDI para todo el log a comitear.
        String ovLogID = UUID.randomUUID().toString();
        
        for (LogData logData : logdata) {
            Map<String, Object> ologData = new HashMap<>();
            ologData.put("transactionID",ovLogID);
            ologData.put("rid", logData.rid);
            ologData.put("timestamp", DateHelper.getCurrentDateTime());
            ologData.put("user", this.auditUser);
            ologData.put("action", logData.auditType);
            ologData.put("label", logData.label);
            ologData.put("log", logData.data.toString());
            
            OrientVertex ovlog = this.transaction.getGraphdb().addVertex("class:" + this.ODBAUDITLOGVERTEXCLASS, ologData);
        }
        
        this.logdata.clear();
    }
    
    
}

class LogData {
    public String rid;
    public int auditType;
    public String label;
    public Object data;

    public LogData(String rid, int auditType, String label, Object data) {
        this.rid = rid;
        this.auditType = auditType;
        this.label = label;
        this.data = data;
    }
    
}