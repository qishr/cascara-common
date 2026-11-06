package integration.test.exec;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.LocalReporter;
import io.github.qishr.cascara.common.diagnostic.report.ReportWriter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.test.common.junit.base.VfsTestBase;
import io.github.qishr.cascara.test.common.util.ArtifactResolver;

public class ExecTestBase extends VfsTestBase {
    protected static final boolean TEST_DEBUG_ENABLED = true;
    protected static final boolean PROCESS_DEBUG_ENABLED = false;

    protected static final String CASCARA_HOME = "CASCARA_HOME";

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
    }

    protected String getModulePath() {
        Pair<String,String> mp = ArtifactResolver.buildModulePath(
            List.of("cascara-common", "integration-test-fixtures", "cascara-lang-json"),
            ExecTestBase.class
        );
        String modulePath = mp.getL();
        return modulePath;
    }

    protected void debug(Response<?> response) {
        if (TEST_DEBUG_ENABLED) {
            Reporter reporter = new LocalReporter().setLevel(Level.DEBUG);
            ReportWriter writer = reporter.getWriter(Level.DEBUG);
            if (!response.getSystemOut().isBlank()) {
                reporter.debug("System.out was:\n");
                writer.write(2, response.getSystemOut());
            }
            if (!response.getSystemErr().isBlank()) {
                reporter.debug("System.err was:\n");
                writer.write(2, response.getSystemErr());
            }
        }
    }

    protected void debug(JvmProcess.Response response) {
        if (TEST_DEBUG_ENABLED) {
            Reporter reporter = new LocalReporter().setLevel(Level.DEBUG);
            ReportWriter writer = reporter.getWriter(Level.DEBUG);
            if (!response.out.isBlank()) {
                reporter.debug("JvmProcess.Response.out was:\n");
                writer.write(2, response.out);
            }
            if (!response.err.isBlank()) {
                reporter.debug("JvmProcess.Response.err was:\n");
                writer.write(2, response.err);
            }
        }
    }
}
