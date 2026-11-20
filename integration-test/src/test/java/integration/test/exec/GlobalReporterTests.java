package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Isolated;

import integration.test.fixtures.payload.ReporterTestOutput;
import integration.test.fixtures.task.GlobalReporterTestTask;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.test.common.junit.extension.AssertReportedExtension;

// ./gradlew :cascara-common:build publishToMavenLocal -x javadoc -x test -x testClasspath -x testJarClasspath --refresh-dependencies
// ./gradlew :spl-test:build -x javadoc :spl-test:test --tests "*GlobalReporterTests*"

@Isolated
public class GlobalReporterTests extends ExecTestBase {
    @RegisterExtension
    static final AssertReportedExtension assertReported = new AssertReportedExtension();

    @Test
    void test_debugLevel() throws Throwable {
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);

        String classReportLevel = "cascara.report.level." + GlobalReporterTestTask.class.getName();

        JvmConfig config = new JvmConfig()
            .setSystemProperty(classReportLevel, "DEBUG")
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
