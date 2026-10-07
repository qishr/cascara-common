package test.exec;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;
import test.interfaces.payload.ReporterTestInput;
import test.interfaces.task.JmxTestTask;

public class JmxTest extends JmxTestBase {
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    // TODO: this fails when running in parallel to other jmx tests. port number?
    @Test
    void test_JMX() throws IOException {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString(input);

        Class<?> taskClass = JmxTestTask.class;
        GlobalReporter.forClass(taskClass).setLevel(Level.DEBUG);
        IpcServer ipcServer = IpcServer.start(serializer, true, false);

        JvmOptions options = newJmxTestOptions()
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setSystemProperty("casc.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString());

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


        String urlString = "service:jmx:rmi:///jndi/rmi://127.0.0.1:9010/jmxrmi";
        JMXServiceURL url = new JMXServiceURL(urlString);

        try {
            JMXConnector jmxConnector = connectWithRetry(url, 20, 1000);
            MBeanServerConnection mbsc = jmxConnector.getMBeanServerConnection();

            debug("Got MBeanServerConnection");
            // invokeOperation(mbsc, "test.interfaces:type=SPLStatus", "exit", null, null, false);
            invokeOperation(mbsc, "test.interfaces:type=CascaraControl", "exit", null, null, false);

            jmxConnector.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }

        List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);
        assertFalse(diagnostics.isEmpty());
    }
}
