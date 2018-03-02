/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm;

import java.util.logging.Level;

/**
 * Configuración de los log de cada clase
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class LogginProperties {

    public static Level ArrayListEmbeddedProxy      = Level.INFO;
    public static Level ArrayListLazyProxy          = Level.INFO;
    public static Level Auditor                     = Level.FINER;
    public static Level ClassCache                  = Level.INFO;
    public static Level ClassDef                    = Level.INFO;
    public static Level DbManager                   = Level.INFO;
    public static Level HashMapEmbeddedProxy        = Level.INFO;
    public static Level HashMapLazyProxy            = Level.INFO;
    public static Level LinkedListLazyProxy         = Level.INFO;
    public static Level ObjectMapper                = Level.INFO;
    public static Level ObjectProxy                 = Level.FINER;
    public static Level ObjectProxyFactory          = Level.INFO;
    public static Level ObjectStruct                = Level.INFO;
    public static Level ReflectionUtils             = Level.INFO;
    public static Level SessionManager              = Level.INFO;
    public static Level SObject                     = Level.INFO;
    public static Level VectorLazyProxy             = Level.INFO;
    public static Level VertexUtil                  = Level.INFO;
    public static Level ThreadedGraphRecordFactory  = Level.INFO;
    public static Level Transaction                 = Level.INFO;
    
    public static Level TransparentDirtyDetector                   = Level.FINER;
    public static Level TransparentDirtyDetectorAdapter            = Level.FINER;
    public static Level TransparentDirtyDetectorInstrumentator     = Level.FINER;
    public static Level TransparentDirtyDetectorAgent              = Level.FINER;
    public static Level WriteAccessActivatorAdapter                = Level.FINER;
    
}
