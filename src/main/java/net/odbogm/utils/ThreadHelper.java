/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.odbogm.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author SShadow
 */
public class ThreadHelper {
    private final static Logger LOGGER = Logger.getLogger(ThreadHelper.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    /**
     * Retorna un string con el stacktrace hasta el método que lo invoca.
     * Útil para tareas de debuggin.
     * @return String con el stack.
     */
    public synchronized static String getCurrentStackTrace() {
        StringBuffer stb = new StringBuffer();
        stb.append("\n*********************************\n");
        stb.append("*      CURRENT STACK TRACE      *\n");
        stb.append("*********************************\n");
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        // se ignoran los primeros dos renglones porque son la llamada a este método y la invocación a Thread.getStackTrace();
        for (int i = 2; i< st.length; i++) {
            StackTraceElement stackTraceElement = st[i];
            stb.append(stackTraceElement+"\n    ");
        }
        return stb.toString();
    }
    
    public synchronized static String getFullLineDescript() {
        StackTraceElement l = new Exception().getStackTrace()[0];
        return l.getClassName()+"/"+l.getMethodName()+":"+l.getLineNumber();
    }
    
    public synchronized static int getLineNumber() {
        return (new Exception()).getStackTrace()[0].getLineNumber();
    }
}
