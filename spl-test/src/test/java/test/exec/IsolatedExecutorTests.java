package test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmOptions;
import test.interfaces.ReporterTestInput;
import test.interfaces.ReporterTestOutput;
import test.task.GlobalReporterTestTask;

public class IsolatedExecutorTests extends JvmProcessTestBase {

    @Test
    void test_IsolatedExecutor_1() throws Exception {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");

        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);

        exec.setOptions(new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .setEnv("CASC_REPORT_LEVEL_IO_GITHUB_QISHR_CASCARA_COMMON_DIAGNOSTIC_GLOBALREPORTERTESTS", "DEBUG")
            .setTimeout(Duration.ofSeconds(3))
            .setDebug(PROCESS_DEBUG_ENABLED));

        Response<ReporterTestOutput> result = exec.run(input);

        debug("payload: " + result.getPayload());
        debug("diagnostics: " + result.getDiagnostics());

        assertTrue(result.isSuccess());
        assertEquals(42, result.getPayload().n());
        assertTrue(result.getPayload().s().contains("hello-isolated-world"));
    }

    @Test
    void test_IsolatedExecutor_2() throws Exception {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");

        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);

        Response<ReporterTestOutput> result = exec.run(
            input,
            new JvmOptions()
                .setModuleName(GlobalReporterTestTask.class.getModule().getName())
                .setModulePath(getModulePath())
                .setEnv("CASC_REPORT_LEVEL_IO_GITHUB_QISHR_CASCARA_COMMON_DIAGNOSTIC_GLOBALREPORTERTESTS", "DEBUG")
                .setTimeout(Duration.ofSeconds(3))
                .setDebug(PROCESS_DEBUG_ENABLED)
        );

        assertTrue(result.isSuccess());
        assertEquals(42, result.getPayload().n());
        assertTrue(result.getPayload().s().contains("hello-isolated-world"));
    }

    @Test
    void test_isolatedExecution_classpathMode() throws Exception {
        ReporterTestInput input = new ReporterTestInput("classpath-mode-test");

        JvmOptions options = new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModuleName(null) // Forces pure -cp launch
            .setClassPath(getModulePath())
            .setTimeout(Duration.ofSeconds(5))
            .setDebug(PROCESS_DEBUG_ENABLED);

        Response<ReporterTestOutput> result = IsolatedExecutor.run(
            GlobalReporterTestTask.class,
            input,
            ReporterTestOutput.class,
            options);

        ReporterTestOutput output = result.orElseThrow();
        assertEquals(42, output.n());
        assertTrue(output.s().contains("classpath-mode-test"));
    }

    @Test
    void test_isolatedExecution_jpmsModuleMode() throws Exception {
        ReporterTestInput input = new ReporterTestInput("jpms-module-mode-test");

        JvmOptions options = new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName()) // Forces --module-path launch
            .setModulePath(getModulePath())
            .addModule("test.task")
            .setTimeout(Duration.ofSeconds(5))
            .setDebug(PROCESS_DEBUG_ENABLED);

        Response<ReporterTestOutput> result = IsolatedExecutor.run(
            GlobalReporterTestTask.class,
            input,
            ReporterTestOutput.class,
            options);

        debug(result);

        ReporterTestOutput output = result.orElseThrow();
        assertEquals(42, output.n());
        assertTrue(output.s().contains("jpms-module-mode-test"));
    }
}
