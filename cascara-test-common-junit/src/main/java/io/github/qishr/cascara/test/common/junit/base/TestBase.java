package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;

public abstract class TestBase {
    private static final Reporter REPORTER = Reporter.forClass(TestBase.class);

    @TempDir
    public Path tempDir;

    protected Path createTempFile() {

        try {
            Path tempFile = Files.createTempFile(tempDir, "cascara-test-", ".tmp");

            return tempFile;
        } catch (IOException e) {
            REPORTER.error(e, GenericMessage.ERROR, "Failed to create temp file");
            return null;
        }
    }

    @BeforeEach
    protected void setUp() throws IOException {
        GlobalReporter.globalInstance().setLevel(Level.INFO);
        GlobalReporter.globalInstance().setSystemOutputEnabled(true);
        GlobalReporter.globalInstance().setLineConsumer(null);
        GlobalReporter.globalInstance()
            .getFormatOptions()
                .setIncludeTime(false)
                .setIncluedPid(false)
                .setIncluedThread(false)
                .setUseSimpleClassName(true);
    }

    @AfterEach
    protected void tearDown() throws IOException {
    }

    protected Path getTempDir() {
        return tempDir;
    }
}
