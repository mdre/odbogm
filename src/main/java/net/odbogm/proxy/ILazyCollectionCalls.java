/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.proxy;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import java.util.Map;
import net.odbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ILazyCollectionCalls extends ILazyCalls {
    public void init(Transaction sm, OrientVertex relatedTo, IObjectProxy parent, String field, Class<?> c);
    public Map<Object,ObjectCollectionState> collectionState();
    
}
