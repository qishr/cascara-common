package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.test.common.junit.extension.AssertReportedExtension;
import integration.test.fixtures.payload.ReporterTestOutput;
import integration.test.fixtures.task.GlobalReporterTestTask;

// ./gradlew :cascara-common:build publishToMavenLocal -x javadoc -x test -x testClasspath -x testJarClasspath --refresh-dependencies
// ./gradlew :spl-test:build -x javadoc :spl-test:test --tests "*GlobalReporterTests*"

public class GlobalReporterTests extends ExecTestBase {
    @RegisterExtension
    static final AssertReportedExtension assertReported = new AssertReportedExtension();

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @Test
    void test_debugLevel() throws Throwable {
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);

        String classKey = "CASCARA_REPORT_LEVEL_" + GlobalReporterTestTask.class.getName().replace('.', '_').toUpperCase();

        JvmConfig config = new JvmConfig()
            .setEnv(classKey, "DEBUG")
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED);

        IsolatedExecutor exec = IsolatedExecutor.forTask(GlobalReporterTestTask.class, ReporterTestOutput.class);
        exec.setDiagnosticForwarding(true);

        Response<ReporterTestOutput> result = exec.run("debug-message", config);

        GlobalReporter.globalInstance().setSystemOutputEnabled(true);
        debug(result);

        ReporterTestOutput output = result.orElseThrow();
        assertEquals(42, output.n());
        assertTrue(output.s().contains("debug-message"));

        assertReported.debug("debug-message");
    }
}
