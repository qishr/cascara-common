package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;

public abstract class TestBase {
    private static final Level TEST_REPORTING_LEVEL = Level.INFO;

    public GlobalReporter reporter;

    @TempDir
    public Path tempDir;

    protected Path createTempFile() {

        try {
            Path tempFile = Files.createTempFile(tempDir, "cascara-test-", ".tmp");

            return tempFile;
        } catch (IOException e) {
            // REPORTER.error(e);
            return null;
        }
    }

    @BeforeEach
    protected void setUp() throws IOException {
        GlobalReporter.globalInstance().setLevel(TEST_REPORTING_LEVEL);
        reporter = GlobalReporter.forClass(getClass());
    }

    @AfterEach
    protected void tearDown() throws IOException {
    }

    protected Path getTempDir() {
        return tempDir;
    }
}
