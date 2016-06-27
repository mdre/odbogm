/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 *
 * @author SShadow
 */
public interface IObjectProxy {
    public OrientVertex ___getVertex();
    public String ___getRid();
    public void ___setVertex(OrientVertex v);
    
    public OrientVertex ___getEdge();
    public void ___setEdge(OrientEdge v);
    
    public Class<?> ___getBaseClass();
    public Object ___getProxiObject();
    
    public void ___setDirty();
    public  boolean ___isDirty() ;
    public void ___removeDirtyMark();
    
    public void ___commit();
    public void ___rollback();
    public void ___loadLazyLinks();
}
