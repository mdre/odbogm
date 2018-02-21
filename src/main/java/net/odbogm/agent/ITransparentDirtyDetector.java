/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.agent;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ITransparentDirtyDetector {
    public boolean ___ogm___isDirty();
    public void ___ogm___setDirty(boolean b);
}
