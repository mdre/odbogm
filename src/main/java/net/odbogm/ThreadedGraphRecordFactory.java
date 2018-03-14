/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseThreadLocalFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import java.util.logging.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ThreadedGraphRecordFactory implements ODatabaseThreadLocalFactory {

    private final static Logger LOGGER = Logger.getLogger(ThreadedGraphRecordFactory.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ThreadedGraphRecordFactory);
        }
    }
    
    private String url;
    private String user;
    private String passwd;
    
    public ThreadedGraphRecordFactory(String url, String usr, String pass) {
        this.url = url;
        this.user = usr;
        this.passwd = pass;
    }

    
    public ODatabaseDocumentTx getDb() {
        return ODatabaseDocumentPool.global().acquire(url, user, passwd);
    }

    @Override
    public ODatabaseDocumentInternal getThreadDatabase() {
        return ODatabaseDocumentPool.global().acquire(url, user, passwd).getUnderlying();
    }
}
