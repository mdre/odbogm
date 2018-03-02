/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Test;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.security.GroupSID;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class GroupEx extends GroupSID {
    private final static Logger LOGGER = Logger.getLogger(GroupEx.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    private String nombre;
    private String descripción;

    public GroupEx() {
    }

    public GroupEx(String name, String uuid) {
        super(name, uuid);
    }
    
    
}
