package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.logging.log4j.core.config.Configurator;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.logging.log4j.Log4jLogger;
import integration.test.fixtures.payload.ReporterTestInput;
import integration.test.fixtures.task.JmxTestTask;
import org.apache.logging.log4j.core.config.Configurator;

public class JmxTest extends JmxTestBase {
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();

    }

    @Test
    void test_JMX() throws Exception {
        Configurator.setLevel("integration.test", org.apache.logging.log4j.Level.DEBUG);
        GlobalReporter.globalInstance().addLogger(new Log4jLogger());
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);

        reporter.debug("CASC_HOME=" + getPhysicalHomePath());
        syncVfs();

        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString(input);

        boolean ipcDebug = false;

        Class<?> taskClass = JmxTestTask.class;
        GlobalReporter.forClass(taskClass).setLevel(Level.DEBUG);
        IpcServer ipcServer = IpcServer.start(serializer, true, ipcDebug);

        int jmxPort = findFreePort();

        JvmOptions options = newJmxTestOptions(jmxPort)
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setSystemProperty("casc.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString())
            .setSystemProperty(IpcClient.DEBUG_PROP, String.valueOf(ipcDebug))
            .setEnv("CASC_HOME", getPhysicalHomePath());

        // Run sub-JVM process...
        JvmProcess.forClass(taskClass)
            .setOptions(options)
            .runAsync(json)
            .thenAccept(response -> {
                // Callback executed upon child process completion
                debug("Background process finished with exit code: " + response.exitCode);
                debug(response);
                try {
                    ipcServer.close();
                } catch (IOException e) {
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
