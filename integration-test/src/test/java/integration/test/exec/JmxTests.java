package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import integration.test.fixtures.task.JmxTestTask;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcException;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.logging.log4j.Log4jLogger;

@Isolated
public class JmxTests extends JmxTestBase {
    private static final Reporter REPORTER = Reporter.forClass(JmxTests.class);

    @SuppressWarnings("resource")
    @Disabled
    @Test
    void test_JMX() throws Throwable {
        Configurator.setLevel("integration.test", org.apache.logging.log4j.Level.DEBUG);
        GlobalReporter.globalInstance().addLogger(new Log4jLogger());
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);

        REPORTER.debug("CASCARA_HOME=" + getPhysicalHomePath());
        syncVfs();

        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString("hello-isolated-world");

        boolean ipcDebug = true;

        Class<?> taskClass = JmxTestTask.class;
        GlobalReporter.forClass(taskClass).setLevel(Level.DEBUG);

        // IpcServer ipcServer = IpcServer.start(true, ipcDebug);
        IpcServer ipcServer = new IpcServer()
            .setDiagnosticForwarding(true)
            .setIpcDebug(ipcDebug)
            .start();

        int jmxPort = findFreePort();

        JvmConfig config = newJmxTestConfig(jmxPort)
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setSystemProperty("cascara.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.PROP_IPC_SOCKET, ipcServer.getSocketPath().toString())
            // .setSystemProperty(IpcClient.PROP_DEBUG, String.valueOf(ipcDebug))
            .setEnv("CASCARA_HOME", getPhysicalHomePath());

        // Run sub-JVM process...
        JvmProcess.forClass(taskClass)
            .setConfig(config)
            .runAsync(json)
            .thenAccept(response -> {
                // Callback executed upon child process completion
                REPORTER.debug("Background process finished with exit code: " + response.exitCode);
                debug(response);
                try {
                    ipcServer.close();
                } catch (IpcException e) {
                    e.printStackTrace();
                }
            });


        String urlString = String.format("service:jmx:rmi:///jndi/rmi://127.0.0.1:%d/jmxrmi", jmxPort);
        JMXServiceURL url = new JMXServiceURL(urlString);

        JMXConnector jmxConnector = connectWithRetry(url, 20, 1000);
        MBeanServerConnection mbsc = jmxConnector.getMBeanServerConnection();

        invokeOperation(mbsc, "test.interfaces.beans:type=CascaraControl", "test", null, null, false);
        Thread.sleep(1000);

        invokeOperation(mbsc, "test.interfaces.beans:type=CascaraControl", "exit", null, null, false);

        List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);
        assertFalse(diagnostics.isEmpty());

        // Thread.sleep(60000);
        Thread.sleep(1000);
    }
}
