/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import java.util.HashMap;

/**
 * Estructura para soportar los valores de un objeto.
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectStruct {

//    private final static Logger LOGGER = Logger.getLogger(ObjectStruct.class.getName());

    /**
     * Mapa de atributos básicos
     */
    public HashMap<String, Object> fields = new HashMap<>();
    
    /**
     * Mapa de atributos básicos
     */
    public HashMap<String, Object> enumFields = new HashMap<>();
    
    /**
     * Mapa de links a otros objetos
     */
    public HashMap<String, Object> links = new HashMap<>();
    
    /**
     * Mapa de lista de links a otros objetos
     */
    public HashMap<String, Object> linkLists = new HashMap<>();
}
