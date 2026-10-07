package test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;
import io.github.qishr.cascara.common.service.SPL;
import test.interfaces.payload.ReporterTestInput;
import test.interfaces.task.GlobalReporterTestTask;

public class JvmProcessTests extends ExecTestBase {
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @Test
    void test_JvmProcess_withoutIPC() throws Exception {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");

        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString(input);

        JvmOptions options = new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED);

        JvmProcess tp = JvmProcess.forClass(GlobalReporterTestTask.class)
            .setOptions(options);

        JvmProcess.Response result = tp.run(json);

        // debug("result.out: " + result.out);
        // debug("result.err: " + result.err);
        debug(result);

        assertEquals(0, result.exitCode);
        assertFalse(result.timedOut);
        assertTrue(result.out.contains("isolated"));
    }

    @Test
    void test_JvmProcess_withIPC() throws IOException {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString(input);

        IpcServer ipcServer = IpcServer.start(serializer, false, false);
        Class<?> taskClass = GlobalReporterTestTask.class;

        String classKey = "CASC_REPORT_LEVEL_" + taskClass.getName().replace('.', '_').toUpperCase();

        JvmOptions options = new JvmOptions()
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setEnv(classKey, "DEBUG")
            .setSystemProperty("casc.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString());

        // Run sub-JVM process...
        JvmProcess.Response response = JvmProcess.forClass(taskClass).setOptions(options).run(json);
        ipcServer.close();

        debug(response);

        // Fetch diagnostics
        List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);

        assertFalse(diagnostics.isEmpty());
        Diagnostic diagnostic = diagnostics.getFirst();
        assertEquals("hello-isolated-world", diagnostic.getFormattedMessage());
    }

    @Test
    void test_IPC_forwardsDiagnostics() throws IOException {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString(input);

        IpcServer ipcServer = IpcServer.start(serializer, true, false);
        Class<?> taskClass = GlobalReporterTestTask.class;

        GlobalReporter.forClass(SPL.class).setLevel(Level.DEBUG);

        List<Diagnostic> diagnostics = new ArrayList<>();
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);
        GlobalReporter.globalInstance().setDiagnosticConsumer(d -> diagnostics.add(d));

        JvmOptions options = new JvmOptions()
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString());

        // Run sub-JVM process...
        JvmProcess.Response response = JvmProcess.forClass(taskClass).setOptions(options).run(json);
        ipcServer.close();

        debug(response);

        // Verify that SPL's "Discovering providers" debug message appears
        // in the parent JVM's global diagnostics.
        assertFalse(diagnostics.isEmpty());
        boolean foundSplBoot = false;
        for (Diagnostic d : diagnostics) {
            if (d.getFormattedMessage().contains("Discovering providers")) {
                foundSplBoot = true;
            }
        }
        assertTrue(foundSplBoot);

    }
}
