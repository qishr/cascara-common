package test.spl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;
import test.interfaces.ReporterTestInput;
import test.task.GlobalReporterTestTask;
import test.interfaces.ReporterTestOutput;

public class IsolatedExecutorTests {

    @Test
    void test_JvmProcess() throws Exception {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");

        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");
        String json = serializer.toString(input);

        JvmProcess tp = JvmProcess.forClass(GlobalReporterTestTask.class);

        JvmProcess.Response result = tp.run(json);

        assertEquals(0, result.exitCode);
        assertFalse(result.timedOut);
        assertTrue(result.out.contains("isolated"));
    }

    @Test
    void test_IsolatedExecutor_1() throws Exception {
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");

        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);

        exec.setOptions(new JvmOptions()
            .setEnv("CASC_REPORT_LEVEL_IO_GITHUB_QISHR_CASCARA_COMMON_DIAGNOSTIC_GLOBALREPORTERTESTS", "DEBUG")
            .setTimeout(Duration.ofSeconds(3)));

        Response<ReporterTestOutput> result = exec.run(input);

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
                .setEnv("CASC_REPORT_LEVEL_IO_GITHUB_QISHR_CASCARA_COMMON_DIAGNOSTIC_GLOBALREPORTERTESTS", "DEBUG")
                .setTimeout(Duration.ofSeconds(3))
        );

        assertTrue(result.isSuccess());
        assertEquals(42, result.getPayload().n());
        assertTrue(result.getPayload().s().contains("hello-isolated-world"));
    }
}
