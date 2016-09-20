/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.cache;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Estructura para soportar la definición de una clase.
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public class ClassDef {

    private final static Logger LOGGER = Logger.getLogger(ClassDef.class.getName());
    
    /**
     * Mapa de atributos básicos
     */
    public HashMap<String, Class<?>> fields = new HashMap<>();
    
    /**
     * Mapa de atributos básicos
     */
    public HashMap<String, Class<?>> enumFields = new HashMap<>();
    
    /**
     * Mapa de links a otros objetos
     */
    public HashMap<String, Class<?>> links = new HashMap<>();
    
    /**
     * Mapa de lista de links a otros objetos
     */
    public HashMap<String, Class<?>> linkLists = new HashMap<>();
    
    @Override
    public ClassDef clone() {
        ClassDef cd = new ClassDef();
        cd.fields = new HashMap<>(fields);
        cd.enumFields = new HashMap<>(enumFields);
        cd.links = new HashMap<>(links);
        cd.linkLists = new HashMap<>(linkLists);
        return cd;
    }
   
}
