package test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.exec.ExecutionException;
import io.github.qishr.cascara.common.exec.ExecutionMessage;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.DiagnosticIpcClient;
import io.github.qishr.cascara.common.exec.ipc.DiagnosticIpcServer;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;
import test.interfaces.ReporterTestInput;
import test.task.GlobalReporterTestTask;

public class JvmProcessTests extends JvmProcessTestBase {
    @Test
    void test_JvmProcess() throws Exception {
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

        debug("result.out: " + result.out);
        debug("result.err: " + result.err);

        assertEquals(0, result.exitCode);
        assertFalse(result.timedOut);
        assertTrue(result.out.contains("isolated"));
    }

    @Test
    void test_ipc() throws IOException {
        try (DiagnosticIpcServer diagServer = DiagnosticIpcServer.start()) {
            Class<?> taskClass = GlobalReporterTestTask.class;

            ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
            Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
            String json = serializer.toString(input);

            String classKey = "CASC_REPORT_LEVEL_" + taskClass.getName().replace('.', '_').toUpperCase();

            JvmOptions options = new JvmOptions()
                .setModuleName(taskClass.getModule().getName())
                .setModulePath(getModulePath())
                .setDebug(PROCESS_DEBUG_ENABLED)
                .setEnv(classKey, "DEBUG")
                .setSystemProperty("casc.report.level." + taskClass.getName(), "DEBUG")
                .setSystemProperty(DiagnosticIpcClient.SOCKET_PROP, diagServer.getSocketPath().toString());

            // Run sub-JVM process...
            JvmProcess.Response response = JvmProcess.forClass(taskClass).setOptions(options).run(json);

            debug(response);

            // Read diagnostics isolated from stdout/stderr
            List<String> rawDiags = diagServer.getRawDiagnostics();

            // Deserialize
            List<Diagnostic> diagnostics = new ArrayList<>();
            try {
                for (String s : rawDiags) {
                    Diagnostic d = serializer.fromString(s, Diagnostic.class);
                    diagnostics.add(d);
                }
            } catch (SerializerException e) {
                throw new ExecutionException(e, ExecutionMessage.OUTPUT_FAILED, taskClass.getName(), response.out, response.err);
            }

            assertFalse(diagnostics.isEmpty());
            Diagnostic diagnostic = diagnostics.getFirst();
            assertEquals("hello-isolated-world", diagnostic.getFormattedMessage());
        }
    }
}
