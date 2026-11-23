package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import integration.test.fixtures.task.ReporterConfigApiOverrideTask;
import integration.test.fixtures.task.ReporterConfigTask;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.test.common.junit.extension.AssertNotReportedExtension;
import io.github.qishr.cascara.test.common.junit.extension.AssertReportedExtension;
import io.github.qishr.cascara.test.common.util.TestCascara;

public class ReporterConfigTests extends ExecTestBase {

    @RegisterExtension
    static final AssertReportedExtension assertReported = new AssertReportedExtension();
    static final AssertNotReportedExtension assertNotReported = new AssertNotReportedExtension();

    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void test_classLevel_sysProp() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            GlobalReporter.get().setConsoleOutputEnabled(false);
            IsolatedExecutor exec = IsolatedExecutor.forTask(ReporterConfigTask.class, String.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(ReporterConfigTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setEnv(EV_CASCARA_REPORT_CONSOLE_OUTPUT, false)
                        .setSystemProperty("cascara.report.level." + ReporterConfigTask.class.getName(), "debug")
                );

            exec.run("Woof!");
            debug(exec.getResponse());
            assertReported.debug("Woof!");
        }
    }

    @Test
    void test_apiOverride_sysProp() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            IsolatedExecutor exec = IsolatedExecutor.forTask(ReporterConfigApiOverrideTask.class, String.class)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(ReporterConfigApiOverrideTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        // .setEnv(EV_CASCARA_REPORT_CONSOLE_OUTPUT, false)
                        .setSystemProperty(SP_CASCARA_REPORT_ALLOW_API_OVERRIDE, false)
                );

            assertThrows(UnsupportedOperationException.class, () -> exec.run("Meow!"));
        }
    }

    @Test
    void test_apiOverride_envVar() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            IsolatedExecutor exec = IsolatedExecutor.forTask(ReporterConfigApiOverrideTask.class, String.class)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(ReporterConfigApiOverrideTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        // .setEnv(EV_CASCARA_REPORT_CONSOLE_OUTPUT, false)
                        .setEnv(EV_CASCARA_REPORT_ALLOW_API_OVERRIDE, false)
                        // .setSystemProperty(SP_CASCARA_SPL_BOOT_INFO_TO_CONSOLE, true)
                        .setSystemProperty(SP_CASCARA_LOG_FORMAT_INCLUDE_STACK_TRACE, true)
                );

            // try { exec.run("Meow!"); } catch (Throwable t) {}
            // debug(exec.getResponse());
            assertThrows(UnsupportedOperationException.class, () -> exec.run("Meow!"));
        }
    }

    @Test
    void test_classLevel_cascaraConfig() throws Throwable {
        GlobalReporter.get().setConsoleOutputEnabled(false);
        try (TestCascara cascara = new TestCascara()) {
            Path cascaraPropertiesPath = cascara.getSharedPath().resolve("cascara.properties");
            Files.writeString(cascaraPropertiesPath, """
                report.level.integration.test.fixtures.task.ReporterConfigTask = debug
                """);

            IsolatedExecutor exec = IsolatedExecutor.forTask(ReporterConfigTask.class, String.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(ReporterConfigTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setEnv(EV_CASCARA_REPORT_CONSOLE_OUTPUT, false)
                        .setDebug(PROCESS_DEBUG_ENABLED)
                    );

            exec.run("Woof!");
            debug(exec.getResponse());
            assertReported.debug("Woof!");
        }
    }

    @Test
    void test_classLevel_reportConfig() throws Throwable {
        GlobalReporter.get().setConsoleOutputEnabled(false);
        try (TestCascara cascara = new TestCascara()) {
            Path reportPropertiesPath = cascara.getSharedPath().resolve("report.properties");
            Files.writeString(reportPropertiesPath, """
                level.integration.test.fixtures.task.ReporterConfigTask = debug
                """);

            IsolatedExecutor exec = IsolatedExecutor.forTask(ReporterConfigTask.class, String.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(ReporterConfigTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setEnv(EV_CASCARA_REPORT_CONSOLE_OUTPUT, false)

                        // Specify the path of report.properties via system propery
                        .setSystemProperty(SP_CASCARA_REPORT_PROPERTIES, reportPropertiesPath)
                );

            exec.run("Woof!");
            debug(exec.getResponse());
            assertReported.debug("Woof!");
        }
    }
}
