/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.auditory;

import net.odbogm.proxy.IObjectProxy;

/**
 *
 * @author Marcelo D. Ré <marcelo.re@gmail.com>
 */
public interface IAuditor {
    public void auditLog(IObjectProxy o, int at, String label, Object data);

    public void commit();
}
