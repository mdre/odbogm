/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.cache;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface Cache {
    
    void add(String key, Object value);
 
    void remove(String key);
 
    Object get(String key);
 
    void clear();
 
    long size();
}