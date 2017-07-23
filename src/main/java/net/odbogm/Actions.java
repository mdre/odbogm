/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import net.odbogm.exceptions.IncorrectRIDField;
import net.odbogm.exceptions.UnknownRID;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import net.odbogm.exceptions.VertexJavaClassNotFound;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface Actions {
    public interface Store {
        /**
         * Guarda un objeto en la base de datos descomponiéndolo en vértices y links
         * y retorna el @RID asociado. 
         * @param <T>
         * @param o
         * @return
         * @throws IncorrectRIDField 
         */
        <T> T store(T o) throws IncorrectRIDField;
    }
    
    public interface Delete {
        <T> void delete(T object);
        <T> void deleteAll(Class<T> type);
        void purgeDatabase();
        void clear();
    }
    
    public interface Get {
        // load a single object of Class type, with id id
        Object get(String rid) throws UnknownRID; 
        <T> T get(Class<T> type, String  rid) throws UnknownRID, VertexJavaClassNotFound ;
        public <T> T getEdgeAsObject(Class<T> type, OrientEdge e);
    }
    
    
    
}
