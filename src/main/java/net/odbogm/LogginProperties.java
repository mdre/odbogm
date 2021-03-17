package net.odbogm;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración de los log de cada clase
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class LogginProperties {
    
    public static Level AccessRight                 = Level.INFO;
    public static Level ArrayListEmbeddedProxy      = Level.INFO;
    public static Level ArrayListLazyProxy          = Level.INFO;
    public static Level Auditor                     = Level.FINEST;
    public static Level ClassCache                  = Level.INFO;
    public static Level ClassDef                    = Level.INFO;
    public static Level DbManager                   = Level.INFO;
    public static Level HashMapEmbeddedProxy        = Level.INFO;
    public static Level HashMapLazyProxy            = Level.INFO;
    public static Level LinkedListLazyProxy         = Level.INFO;
    public static Level ObjectMapper                = Level.INFO;
    public static Level ObjectProxy                 = Level.FINEST;
    public static Level ObjectProxyFactory          = Level.INFO;
    public static Level ObjectStruct                = Level.INFO;
    public static Level ReflectionUtils             = Level.INFO;
    public static Level SessionManager              = Level.INFO;
    public static Level SimpleCache                 = Level.INFO;
    public static Level VectorLazyProxy             = Level.INFO;
    public static Level VertexUtil                  = Level.INFO;
    public static Level ThreadedGraphRecordFactory  = Level.INFO;
    public static Level Transaction                 = Level.FINEST;
    
    public static Level SID                         = Level.INFO;
    public static Level GroupSID                    = Level.INFO;
    public static Level UserSID                     = Level.INFO;
    public static Level SObject                     = Level.INFO;
    
    
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
