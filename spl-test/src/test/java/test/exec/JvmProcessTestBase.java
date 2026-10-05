package test.exec;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.ReportWriter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.exec.ArtifactResolver;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.util.Pair;
import test.interfaces.ReporterTestOutput;

public class JvmProcessTestBase {
    protected static final boolean TEST_DEBUG_ENABLED = true;
    protected static final boolean PROCESS_DEBUG_ENABLED = false;
    protected Reporter reporter;

    @BeforeEach
    void setUp() {
        // reporter = new StandardReporter()
        //     .setAnsiColoringEnabled(true);

        if (TEST_DEBUG_ENABLED) {
            reporter.setLevel(Level.DEBUG);
        }
    }

    protected String getModulePath() {
        Pair<String,String> mp = ArtifactResolver.buildModulePath(
            List.of("cascara-common", "spl-test-interfaces", "test-task", "cascara-lang-json"),
            "spl-test"
        );
        String modulePath = mp.getL();
        return modulePath;
    }

    protected void debug(String msg) {
        reporter.debug(msg);
    }

    protected void debug(Response<ReporterTestOutput> response) {
        if (TEST_DEBUG_ENABLED) {
            ReportWriter writer = reporter.getWriter(Level.DEBUG);
            if (!response.getSystemOut().isBlank()) {
                reporter.debug("System.out:\n");
                writer.write(2, response.getSystemOut());
            }
            if (!response.getSystemErr().isBlank()) {
                reporter.debug("System.err:\n");
                writer.write(2, response.getSystemErr());
            }
        }
    }

    protected void debug(JvmProcess.Response response) {
        if (TEST_DEBUG_ENABLED) {
            ReportWriter writer = reporter.getWriter(Level.DEBUG);
            if (!response.out.isBlank()) {
                reporter.debug("JvmProcess.Response.out:\n");
                writer.write(2, response.out);
            }
            if (!response.err.isBlank()) {
                reporter.debug("JvmProcess.Response.err:\n");
                writer.write(2, response.err);
            }
        }
    }
}
