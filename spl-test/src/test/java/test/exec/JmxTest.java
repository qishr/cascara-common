package test.exec;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

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
import test.interfaces.JmxTestTask;
import test.interfaces.ReporterTestInput;

public class JmxTest extends JvmProcessTestBase {
    @BeforeEach
    protected void setUp() {
        reporter = GlobalReporter.forClass(getClass());
        super.setUp();
    }

    @Test
    void test_JMX() throws IOException {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString(input);

        Class<?> taskClass = JmxTestTask.class;
        GlobalReporter.forClass(taskClass).setLevel(Level.DEBUG);
        IpcServer ipcServer = IpcServer.start(serializer, true);

        JvmOptions options = new JvmOptions()
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setSystemProperty("casc.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString())
            .setSystemProperty("-Dcom.sun.management.jmxremote","")
            .setSystemProperty("-Dcom.sun.management.jmxremote.port", "9010")
            .setSystemProperty("-Dcom.sun.management.jmxremote.rmi.port", "9010")
            .setSystemProperty("-Dcom.sun.management.jmxremote.local.only", "false")
            .setSystemProperty("-Dcom.sun.management.jmxremote.authenticate", "false")
            .setSystemProperty("-Dcom.sun.management.jmxremote.ssl", "false");

        // Run sub-JVM process...
        JvmProcess.Response response = JvmProcess.forClass(taskClass).setOptions(options).run(json);
        ipcServer.close();

        debug(response);

        List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);
        assertFalse(diagnostics.isEmpty());
    }
}
