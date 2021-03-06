package io.bitsquare.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

// Borrowed from: https://dzone.com/articles/programmatically-restart-java
public class RestartUtil {
    private static final Logger log = LoggerFactory.getLogger(RestartUtil.class);

    /**
     * Sun property pointing the main class and its arguments.
     * Might not be defined on non Hotspot VM implementations.
     */
    public static final String SUN_JAVA_COMMAND = "sun.java.command";

    public static void restartApplication(String logPath) throws IOException {
        try {
            String java = System.getProperty("java.home") + "/bin/java";
            List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            StringBuffer vmArgsOneLine = new StringBuffer();
            for (String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in conflict
                if (!arg.contains("-agentlib")) {
                    vmArgsOneLine.append(arg);
                    vmArgsOneLine.append(" ");
                }
            }
            // init the command to execute, add the vm args
            final StringBuffer cmd = new StringBuffer(java + " " + vmArgsOneLine);

            // program main and program arguments
            String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
            // program main is a jar
            if (mainCommand[0].endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                cmd.append("-jar " + new File(mainCommand[0]).getPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
            }
            // finally add program arguments
            for (int i = 1; i < mainCommand.length; i++) {
                cmd.append(" ");
                cmd.append(mainCommand[i]);
            }

            try {
                final String command = "nohup " + cmd.toString() + " >/dev/null 2>" + logPath + " &";
                log.warn("\n\n############################################################\n" +
                                "Executing cmd for restart: {}" +
                                "\n############################################################\n\n",
                        command);
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new IOException("Error while trying to restart the application", e);
        }
    }
}
