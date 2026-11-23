package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Isolated;

import integration.test.fixtures.task.GlobalReporterTestTask;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcException;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.test.common.junit.extension.AssertReportedExtension;

@Isolated
public class JvmProcessTests extends ExecTestBase {
    @RegisterExtension
    static final AssertReportedExtension assertReported = new AssertReportedExtension();

    @Test
    void test_JvmProcess_withoutIPC() throws Exception {
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString("hello-isolated-world");

        JvmConfig config = new JvmConfig()
            .setTimeout(Duration.ofHours(24))
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getJvmModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED);

        JvmProcess tp = JvmProcess.forClass(GlobalReporterTestTask.class)
            .setConfig(config);

        JvmProcess.Response result = tp.run(json);

        debug(result);

        assertEquals(0, result.exitCode);
        assertFalse(result.timedOut);
        assertTrue(result.out.contains("isolated"));
    }

    @Test
    void test_JvmProcess_withIPC() throws Throwable {
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString("hello-isolated-world");

        GlobalReporter.get().setSystemOutputEnabled(false);

        try (IpcServer ipcServer = new IpcServer()) {
            ipcServer.setDiagnosticForwarding(true)
                     .setIpcDebug(false)
                     .start();

            Class<?> taskClass = GlobalReporterTestTask.class;

            String classKey = "CASCARA_REPORT_LEVEL_" + taskClass.getName().replace('.', '_').toUpperCase();

            JvmConfig config = new JvmConfig()
                .setTimeout(Duration.ofHours(24))
                .setModuleName(taskClass.getModule().getName())
                .setModulePath(getJvmModulePath())
                .setDebug(PROCESS_DEBUG_ENABLED)
                .setEnv(classKey, "DEBUG")
                .setSystemProperty("cascara.report.level." + taskClass.getName(), "DEBUG")
                .setSystemProperty(IpcClient.PROP_IPC_SOCKET, ipcServer.getSocketPath().toString());

            // Run sub-JVM process...
            JvmProcess.Response response = JvmProcess.forClass(taskClass).setConfig(config).run(json);

            ipcServer.close();

            GlobalReporter.get().setSystemOutputEnabled(true);
            debug(response);

            // Fetch diagnostics
            List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);

            assertEquals(1, diagnostics.size());
            Diagnostic diagnostic = diagnostics.getFirst();
            assertEquals("hello-isolated-world", diagnostic.getFormattedMessage());
        } catch (IpcException e) {

        }
    }

    @Test
    void test_IPC_forwardsDiagnostics() throws Throwable {
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString("hello-isolated-world");

        try (IpcServer ipcServer = new IpcServer()) {
            ipcServer.setDiagnosticForwarding(true)
                     .setIpcDebug(false)
                     .start();

            Class<?> taskClass = GlobalReporterTestTask.class;

            GlobalReporter.forClass(SPL.class).setLevel(Level.DEBUG);
            GlobalReporter.get().setSystemOutputEnabled(false);

            JvmConfig config = new JvmConfig()
                .setTimeout(Duration.ofHours(24))
                .setModuleName(taskClass.getModule().getName())
                .setModulePath(getJvmModulePath())
                .setDebug(PROCESS_DEBUG_ENABLED)
                .setSystemProperty("cascara.report.level." + SPL.class.getName(), "DEBUG")
                .setSystemProperty(IpcClient.PROP_IPC_SOCKET, ipcServer.getSocketPath().toString());

            // Run sub-JVM process...
            // JvmProcess.Response response =
            JvmProcess.forClass(taskClass).setConfig(config).run(json);
            // debug(response);
        } catch (IpcException e) {

        }

        // Verify that SPL's "Discovering providers" debug message appears
        // in the parent JVM's global diagnostics.
        assertReported.debug("Discovering provider candidates");
    }
}
