package test.exec;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.ReportWriter;
import io.github.qishr.cascara.common.exec.ArtifactResolver;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.test.common.junit.util.VfsTestBase;
import test.interfaces.payload.ReporterTestOutput;

public class ExecTestBase extends VfsTestBase {
    protected static final boolean TEST_DEBUG_ENABLED = true;
    protected static final boolean PROCESS_DEBUG_ENABLED = false;
    protected GlobalReporter REPORTER;

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();

        REPORTER = GlobalReporter.forClass(getClass());

        // reporter = new StandardReporter()
        //     .setAnsiColoringEnabled(true);

        if (TEST_DEBUG_ENABLED) {
            REPORTER.setLevel(Level.DEBUG);
        }

    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
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
        REPORTER.debug(msg);
    }

    protected void debug(Response<ReporterTestOutput> response) {
        if (TEST_DEBUG_ENABLED) {
            ReportWriter writer = REPORTER.getWriter(Level.DEBUG);
            if (!response.getSystemOut().isBlank()) {
                REPORTER.debug("System.out was:\n");
                writer.write(2, response.getSystemOut());
            }
            if (!response.getSystemErr().isBlank()) {
                REPORTER.debug("System.err was:\n");
                writer.write(2, response.getSystemErr());
            }
        }
    }

    protected void debug(JvmProcess.Response response) {
        if (TEST_DEBUG_ENABLED) {
            ReportWriter writer = REPORTER.getWriter(Level.DEBUG);
            if (!response.out.isBlank()) {
                REPORTER.debug("JvmProcess.Response.out was:\n");
                writer.write(2, response.out);
            }
            if (!response.err.isBlank()) {
                REPORTER.debug("JvmProcess.Response.err was:\n");
                writer.write(2, response.err);
            }
        }
    }
}
