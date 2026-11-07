package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmOptions;
import integration.test.fixtures.payload.ReporterTestOutput;
import integration.test.fixtures.task.ThrowableTestTask;
import integration.test.fixtures.task.GlobalReporterTestTask;

public class IsolatedExecutorTests extends ExecTestBase {

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @Test
    void test_IsolatedExecutor_1() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);

        exec.setOptions(new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .setEnv("CASC_REPORT_LEVEL_IO_GITHUB_QISHR_CASCARA_COMMON_DIAGNOSTIC_GLOBALREPORTERTESTS", "DEBUG")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED));

        Response<ReporterTestOutput> result = exec.run("hello-isolated-world");

        debug(result);

        assertTrue(result.isSuccess());
        assertEquals(42, result.getPayload().n());
        assertTrue(result.getPayload().s().contains("hello-isolated-world"));
    }

    @Test
    void test_IsolatedExecutor_2() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);

        Response<ReporterTestOutput> result = exec.run(
            "hello-isolated-world",
            new JvmOptions()
                .setModuleName(GlobalReporterTestTask.class.getModule().getName())
                .setModulePath(getModulePath())
                .setEnv("CASC_REPORT_LEVEL_IO_GITHUB_QISHR_CASCARA_COMMON_DIAGNOSTIC_GLOBALREPORTERTESTS", "DEBUG")
                .setTimeout(Duration.ofSeconds(20))
                .setDebug(PROCESS_DEBUG_ENABLED)
        );

        assertTrue(result.isSuccess());
        assertEquals(42, result.getPayload().n());
        assertTrue(result.getPayload().s().contains("hello-isolated-world"));
    }

    @Test
    void test_isolatedExecution_classpathMode() throws Throwable {
        JvmOptions options = new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModuleName(null) // Forces pure -cp launch
            .setClassPath(getModulePath())
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED);

        Response<ReporterTestOutput> result = IsolatedExecutor.run(
            GlobalReporterTestTask.class,
            "classpath-mode-test",
            ReporterTestOutput.class,
            options);

        ReporterTestOutput output = result.orElseThrow();
        assertEquals(42, output.n());
        assertTrue(output.s().contains("classpath-mode-test"));
    }

    @Test
    void test_isolatedExecution_jpmsModuleMode() throws Throwable {
        JvmOptions options = new JvmOptions()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName()) // Forces --module-path launch
            .setModulePath(getModulePath())
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED);

        Response<ReporterTestOutput> result = IsolatedExecutor.run(
            GlobalReporterTestTask.class,
            "jpms-module-mode-test",
            ReporterTestOutput.class,
            options);

        debug(result);

        ReporterTestOutput output = result.orElseThrow();
        assertEquals(42, output.n());
        assertTrue(output.s().contains("jpms-module-mode-test"));
    }

    @Test
    void test_forwardsExceptions() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(ThrowableTestTask.class, String.class);
        exec.setExceptionForwarding(true);

        exec.setOptions(new JvmOptions()
            .setModuleName(ThrowableTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .setTimeout(Duration.ofSeconds(20)));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            Response<String> response = exec.run(
                IllegalStateException.class.getName()
            );
            debug(response);
        });
        assertEquals("test-exception", thrown.getMessage());
    }
}
