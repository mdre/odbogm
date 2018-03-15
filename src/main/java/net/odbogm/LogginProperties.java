/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
    public static Level Auditor                     = Level.INFO;
    public static Level ClassCache                  = Level.INFO;
    public static Level ClassDef                    = Level.INFO;
    public static Level DbManager                   = Level.INFO;
    public static Level HashMapEmbeddedProxy        = Level.INFO;
    public static Level HashMapLazyProxy            = Level.INFO;
    public static Level LinkedListLazyProxy         = Level.INFO;
    public static Level ObjectMapper                = Level.INFO;
    public static Level ObjectProxy                 = Level.INFO;
    public static Level ObjectProxyFactory          = Level.INFO;
    public static Level ObjectStruct                = Level.INFO;
    public static Level ReflectionUtils             = Level.INFO;
    public static Level SessionManager              = Level.INFO;
    public static Level VectorLazyProxy             = Level.INFO;
    public static Level VertexUtil                  = Level.INFO;
    public static Level ThreadedGraphRecordFactory  = Level.INFO;
    public static Level Transaction                 = Level.INFO;
    
    public static Level SID                         = Level.INFO;
    public static Level GroupSID                    = Level.INFO;
    public static Level UserSID                     = Level.INFO;
    public static Level SObject                     = Level.INFO;
    
    public static Level TransparentDirtyDetector                   = Level.INFO;
    public static Level TransparentDirtyDetectorAdapter            = Level.FINER;
    public static Level TransparentDirtyDetectorInstrumentator     = Level.INFO;
    public static Level TransparentDirtyDetectorAgent              = Level.INFO;
    public static Level InstrumentableClassDetector                = Level.INFO;
    public static Level WriteAccessActivatorAdapter                = Level.INFO;
    
    
}
