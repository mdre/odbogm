/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import net.odbogm.SessionManager;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.util.Map;

/**
 *
 * @author SShadow
 */
public interface ILazyCollectionCalls extends ILazyCalls {
    public void init(SessionManager sm, OrientVertex relatedTo, IObjectProxy parent, String field, Class<?> c);
    public Map<Object,ObjectCollectionState> collectionState();
    
}
