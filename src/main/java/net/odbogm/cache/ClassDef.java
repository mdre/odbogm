/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.cache;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Logger;
import net.odbogm.LogginProperties;

/**
 * Estructura para soportar la definición de una clase.
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ClassDef {

    private final static Logger LOGGER = Logger.getLogger(ClassDef.class.getName());
    static {
        if (LOGGER.getLevel() == null) {
            LOGGER.setLevel(LogginProperties.ClassDef);
        }
    }
    
    
    
    
    /**
     * Mapa de todos los objetos Field
     */
    public HashMap<String, Field> fieldsObject = new HashMap<>();
    
    /**
     * Mapa de atributos básicos
     */
    public HashMap<String, Class<?>> fields = new HashMap<>();
    
    /**
     * Mapa de atributos básicos
     */
    public HashMap<String, Class<?>> enumFields = new HashMap<>();
    
    /**
     * Mapa de los embedded List y HashMap del objeto
     */
    public HashMap<String, Class<?>> embeddedFields = new HashMap<>();
    
    /**
     * Mapa de links a otros objetos
     */
    public HashMap<String, Class<?>> links = new HashMap<>();
    
    /**
     * Mapa de lista de links a otros objetos
     */
    public HashMap<String, Class<?>> linkLists = new HashMap<>();
    
    /**
     * Mapa de links indirectos. Son referencias IN hacia el objeto actual.
     */
    public HashMap<String, Class<?>> indirectLinks = new HashMap<>();
    
    /**
     * Mapa de links indirectos. Son referencias IN hacia el objeto actual.
     */
    public HashMap<String, Class<?>> indirectLinkLists = new HashMap<>();
    
    /**
     * Mapa de links indirectos. Son referencias IN hacia el objeto actual.
     */
    public HashMap<String, Class<?>> indirectOnQueryLinks = new HashMap<>();
    
    /**
     * Mapa de links indirectos. Son referencias IN hacia el objeto actual.
     */
    public HashMap<String, Class<?>> indirectOnQueryLinkLists = new HashMap<>();
    
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
