package integration.test.exec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import integration.test.fixtures.task.ReporterConfigTask;
import integration.test.fixtures.task.ShutdownTestTask;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.format.FormatConfig;
import io.github.qishr.cascara.common.diagnostic.log.Logger;
import io.github.qishr.cascara.common.diagnostic.log.TimeSequencedAggregatorLogger;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.test.common.junit.extension.AssertReportedExtension;

public class TimeSequencedLoggerTests extends ExecTestBase {

    @RegisterExtension
    static final AssertReportedExtension assertReported = new AssertReportedExtension();

    @Test
    void test_isolated_classReporter() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(ShutdownTestTask.class, String.class)
            .setDiagnosticForwarding(true)
            .setJvmConfig(
                new JvmConfig()
                    .setTimeout(Duration.ofSeconds(120))
                    .setModuleName(ReporterConfigTask.class.getModule().getName())
                    .setModulePath(getModulePath())
                    .setEnv(EV_CASCARA_HOME, getPhysicalHomePath())
                    .setSystemProperty("cascara.report.level." + ShutdownTestTask.class.getName(), "debug")
            );

        // 4. Run the test task
        Response<String> response = exec.run("expected");
        debug(response);

        assertReported.debug("expected");
    }

    @Disabled
    @Test
    void test_tsLogger() throws Throwable {

        List<Diagnostic> aggregate = new ArrayList<>();

        TimeSequencedAggregatorLogger aggregator = new TimeSequencedAggregatorLogger(
            new Logger() {
                @Override public Logger log(Diagnostic diagnostic) {
                    aggregate.add(diagnostic);
                    return this;
                }

                @Override public Logger flush() {
                    return this;
                }

                @Override public Logger setFormatConfig(FormatConfig config) {
                    return this;
                }
            },
             800
        );

        GlobalReporter.globalInstance().addLogger(aggregator);
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);

    }
}
