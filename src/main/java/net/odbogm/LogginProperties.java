package net.odbogm;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración de los log de cada clase
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class LogginProperties {
    
    public static Level AccessRight                 = Level.WARNING;
    public static Level ArrayListEmbeddedProxy      = Level.WARNING;
    public static Level ArrayListLazyProxy          = Level.WARNING;
    public static Level Auditor                     = Level.WARNING;
    public static Level ClassCache                  = Level.WARNING;
    public static Level ClassDef                    = Level.WARNING;
    public static Level DbManager                   = Level.WARNING;
    public static Level HashMapEmbeddedProxy        = Level.WARNING;
    public static Level HashMapLazyProxy            = Level.WARNING;
    public static Level LinkedListLazyProxy         = Level.WARNING;
    public static Level ObjectMapper                = Level.WARNING;
    public static Level ObjectProxy                 = Level.FINEST;
    public static Level ObjectProxyFactory          = Level.WARNING;
    public static Level ObjectStruct                = Level.WARNING;
    public static Level ReflectionUtils             = Level.WARNING;
    public static Level SessionManager              = Level.WARNING;
    public static Level SimpleCache                 = Level.WARNING;
    public static Level VectorLazyProxy             = Level.WARNING;
    public static Level VertexUtil                  = Level.WARNING;
    public static Level ThreadedGraphRecordFactory  = Level.WARNING;
    public static Level Transaction                 = Level.WARNING;
    
    public static Level SID                         = Level.WARNING;
    public static Level GroupSID                    = Level.WARNING;
    public static Level UserSID                     = Level.WARNING;
    public static Level SObject                     = Level.WARNING;
    
    
    /**
     * Shutdown all loggers.
     */
    public static void allLoggersOff() {
        LogginProperties.AccessRight = Level.OFF;
        LogginProperties.ArrayListEmbeddedProxy = Level.OFF;
        LogginProperties.ArrayListLazyProxy = Level.OFF;
        LogginProperties.Auditor = Level.OFF;
        LogginProperties.ClassCache = Level.OFF;
        LogginProperties.ClassDef = Level.OFF;
        LogginProperties.DbManager = Level.OFF;
        LogginProperties.HashMapEmbeddedProxy = Level.OFF;
        LogginProperties.HashMapLazyProxy = Level.OFF;
        LogginProperties.LinkedListLazyProxy = Level.OFF;
        LogginProperties.ObjectMapper = Level.OFF;
        LogginProperties.ObjectProxy = Level.OFF;
        LogginProperties.ObjectProxyFactory = Level.OFF;
        LogginProperties.ObjectStruct = Level.OFF;
        LogginProperties.ReflectionUtils = Level.OFF;
        LogginProperties.SessionManager = Level.OFF;
        LogginProperties.SimpleCache = Level.OFF;
        LogginProperties.VectorLazyProxy = Level.OFF;
        LogginProperties.VertexUtil = Level.OFF;
        LogginProperties.Transaction = Level.OFF;
        LogginProperties.SID = Level.OFF;
        LogginProperties.GroupSID = Level.OFF;
        LogginProperties.UserSID = Level.OFF;
        LogginProperties.SObject = Level.OFF;
        Logger.getLogger("net.odbogm.agent.TransparentDirtyDetectorAgent").setLevel(Level.OFF);
    }
    
}
