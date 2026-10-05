package test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmOptions;
import test.interfaces.ReporterTestInput;
import test.interfaces.ReporterTestOutput;
import test.task.GlobalReporterTestTask;

// ./gradlew :cascara-common:build publishToMavenLocal -x javadoc -x test -x testClasspath -x testJarClasspath --refresh-dependencies
// ./gradlew :spl-test:build -x javadoc :spl-test:test --tests "*GlobalReporterTests*"

public class GlobalReporterTests extends JvmProcessTestBase {
    @BeforeEach
    protected void setUp() {
        reporter = GlobalReporter.forClass(getClass());
        super.setUp();
    }

    @Test
    void test_debugLevel() throws Exception {
        ReporterTestInput input = new ReporterTestInput("debug-message");

        String classKey = "CASC_REPORT_LEVEL_" + GlobalReporterTestTask.class.getName().replace('.', '_').toUpperCase();

        JvmOptions options = new JvmOptions()
            .setEnv(classKey, "DEBUG")
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .addModule("test.task")
            .setTimeout(Duration.ofSeconds(5))
            .setDebug(PROCESS_DEBUG_ENABLED);

        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);

        Response<ReporterTestOutput> result = exec.run(input, options);

        debug(result);

        ReporterTestOutput output = result.orElseThrow();
        assertEquals(42, output.n());
        assertTrue(output.s().contains("debug-message"));
        assertEquals(1, result.getDiagnostics().size());
        Diagnostic diagnostic = result.getDiagnostics().getFirst();
        assertTrue(diagnostic.getFormattedMessage().contains("debug-message"));
    }
}
