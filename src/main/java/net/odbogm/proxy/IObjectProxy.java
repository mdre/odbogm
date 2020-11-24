package net.odbogm.proxy;

import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;


/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface IObjectProxy {
    
    public void ___setDeletedMark();
    public boolean ___isDeleted();
    
    public OElement ___getElement();
    public OVertex ___getVertex();
    public String ___getRid();
    public void ___setVertex(OVertex v);
    public void ___injectRid();
    public void ___uptadeVersion();
    
    public OEdge ___getEdge();
    public void ___setEdge(OEdge v);
    
    public Class<?> ___getBaseClass();
    public Object ___getProxiedObject();
    
    public boolean ___isValid();
    
    public void ___setDirty();
    public boolean ___isDirty() ;
    public void ___removeDirtyMark();
    
    public void ___commit();
    public void ___reload();
    public void ___rollback();
    public void ___loadLazyLinks();
    public void ___eagerLoad();
    public void ___fullLoad();
    
}
