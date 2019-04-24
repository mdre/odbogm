package net.odbogm.proxy;

import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface IObjectProxy {
    
    public void ___setDeletedMark();
    public boolean ___isDeleted();
    
    public OrientVertex ___getVertex();
    public String ___getRid();
    public void ___setVertex(OrientVertex v);
    
    public OrientVertex ___getEdge();
    public void ___setEdge(OrientEdge v);
    
    public Class<?> ___getBaseClass();
    public Object ___getProxiedObject();
    
    public boolean ___isValid();
    
    public void ___setDirty();
    public  boolean ___isDirty() ;
    public void ___removeDirtyMark();
    
    public void ___commit();
    public void ___reload();
    public void ___rollback();
    public void ___loadLazyLinks();
    public void ___updateIndirectLinks();
    
}
