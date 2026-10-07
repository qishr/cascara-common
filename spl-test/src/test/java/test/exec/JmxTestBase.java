package test.exec;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import io.github.qishr.cascara.common.exec.JvmOptions;

public class JmxTestBase extends ExecTestBase {

    protected static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    protected JvmOptions newJmxTestOptions(int jmxPort) {
        JvmOptions options = new JvmOptions()
            .setSystemProperty("com.sun.management.jmxremote","")
            .setSystemProperty("com.sun.management.jmxremote.port", String.valueOf(jmxPort))
            .setSystemProperty("com.sun.management.jmxremote.rmi.port", String.valueOf(jmxPort))
            .setSystemProperty("com.sun.management.jmxremote.local.only", "false")
            .setSystemProperty("com.sun.management.jmxremote.authenticate", "false")
            .setSystemProperty("com.sun.management.jmxremote.ssl", "false");
        return options;
    }

    /// Connects to the remote JMX endpoint with retry polling and explicit socket environment settings.
    protected JMXConnector connectWithRetry(JMXServiceURL url, int maxRetries, long delayMs) throws Exception {
        Map<String, Object> env = new HashMap<>();

        // Provide explicit RMI client socket factory if network address lookup diverges
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {

                // try (Socket socket = new Socket("127.0.0.1", 9010)) {
                //     System.out.println("Port open");
                // }

                JMXConnector connector = JMXConnectorFactory.connect(url, env);
                return connector;
            } catch (IOException e) {
                // e.printStackTrace();
                lastException = e;
                Thread.sleep(delayMs);
            }
        }
        throw new IllegalStateException("Failed to connect to JMX server after " + maxRetries + " attempts", lastException);
    }

    protected void invokeOperation(MBeanServerConnection mbsc, String beanName, String operationName, Object[] params, String[] signature, boolean reportException) {
        try {
            ObjectName objectName = new ObjectName(beanName);

            // Poll up to 5 seconds for registration to complete
            long deadline = System.currentTimeMillis() + 1000;
            while (!mbsc.isRegistered(objectName)) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("Timed out waiting for MBean registration: " + beanName);
                }
                Thread.sleep(500);
            }

            mbsc.invoke(objectName, operationName, params, signature);
        } catch (Exception e) {
            if (reportException) {
                e.printStackTrace();
            }
        }
    }


    // } catch (InstanceNotFoundException e) {
    //     // TODO Auto-generated catch block
    //     e.printStackTrace();
    // } catch (MBeanException e) {
    //     // TODO Auto-generated catch block
    //     e.printStackTrace();
    // } catch (ReflectionException e) {
    //     // TODO Auto-generated catch block
    //     e.printStackTrace();
    // } catch (IOException e) {
    //     // TODO Auto-generated catch block
    //     e.printStackTrace();
    // } catch (MalformedObjectNameException e) {
    //     // TODO Auto-generated catch block
    //     e.printStackTrace();

}
