/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.odbogm.agent;

import com.ea.agentloader.AgentLoader;
import java.lang.instrument.Instrumentation;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import net.odbogm.LogginProperties;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class TransparentDirtyDetectorAgent {

    private final static Logger LOGGER = Logger.getLogger(TransparentDirtyDetectorAgent.class.getName());

    static {
        LOGGER.setLevel(LogginProperties.TransparentDirtyDetectorAgent);
    }

    private static Instrumentation instrumentation;

    static String[] pkgs;

    /**
     * @author Alexey Ragozin (alexey.ragozin@gmail.com)
     */
    private static boolean started;

    static {
        try {
            String javaHome = System.getProperty("java.home");
            String toolsJarURL = "file:" + javaHome + "/../lib/tools.jar";

            // Make addURL public
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);

            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            if (sysloader.getResourceAsStream("/com/sun/tools/attach/VirtualMachine.class") == null) {
                method.invoke(sysloader, (Object) new URL(toolsJarURL));
                Thread.currentThread().getContextClassLoader().loadClass("com.sun.tools.attach.VirtualMachine");
                Thread.currentThread().getContextClassLoader().loadClass("com.sun.tools.attach.AttachNotSupportedException");
            }

        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Java home points to " + System.getProperty("java.home") + " make sure it is not a JRE path");
            LOGGER.log(Level.INFO, "Failed to add tools.jar to classpath", e);
        }
        started = true;
    }

    ;

    /**
     * Determina si el API fue inicializado
     */
    public static void ensureToolsJar() {
        if (!started) {
            LOGGER.log(Level.INFO, "Attach API not initialized");
        }
    }

    /**
     * Agente para manipulación de las clases
     */
    public TransparentDirtyDetectorAgent() {
    }

    /**
     * JVM hook to statically load the javaagent at startup.
     *
     * After the Java Virtual Machine (JVM) has initialized, the premain method will be called. Then the real application main method will be called.
     *
     * @param args
     * @param inst
     * @throws Exception
     */
    public static void premain(String args, Instrumentation inst) throws Exception {
        LOGGER.log(Level.FINER, "premain method invoked with args: {0} and inst: {1}", new Object[]{args, inst});
        instrumentation = inst;
        instrumentation.addTransformer(new TransparentDirtyDetectorInstrumentator(pkgs));
    }

    /**
     * JVM hook to dynamically load javaagent at runtime.
     *
     * The agent class may have an agentmain method for use when the agent is started after VM startup.
     *
     * @param args
     * @param inst
     * @throws Exception
     */
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        LOGGER.log(Level.FINER, "premain method invoked with args: {0} and inst: {1}", new Object[]{args, inst});
        instrumentation = inst;
        instrumentation.addTransformer(new TransparentDirtyDetectorInstrumentator(pkgs));
    }

    /**
     * Programmatic hook to dynamically load javaagent at runtime.
     * @param pkgs
     */
    public static void initialize(String... pkgs) {
        if (instrumentation == null) {
            TransparentDirtyDetectorAgent.pkgs = pkgs;

            LOGGER.log(Level.INFO, "dynamically loading java agent...");
//            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
//            int p = nameOfRunningVM.indexOf('@');
//            String pid = nameOfRunningVM.substring(0, p);
//
//            try {
//                VirtualMachine vm = VirtualMachine.attach(pid);
//                String pathToAgent = TransparentDirtyDetectorInstrumentator.class
//                        .getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
//                LOGGER.log(Level.INFO, "path: "+pathToAgent);
//                vm.loadAgent(pathToAgent, "");
//                vm.detach();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
            AgentLoader.loadAgentClass(TransparentDirtyDetectorAgent.class.getName(), null, null, true, true, true);
        }
    }

}
