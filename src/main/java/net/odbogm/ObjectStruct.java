package net.odbogm;

import java.util.HashMap;

/**
 * Estructura para soportar los valores de un objeto.
 * Mapea nombre del campo con valor del campo.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectStruct {

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
