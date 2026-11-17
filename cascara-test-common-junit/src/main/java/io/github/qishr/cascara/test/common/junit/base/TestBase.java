package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;

public abstract class TestBase {
    private static final Reporter REPORTER = Reporter.forClass(TestBase.class);

    @TempDir
    public Path tempDir;

    protected static TestCertificateAuthority rootCA;

    protected Path createTempFile() {

        try {
            Path tempFile = Files.createTempFile(tempDir, "cascara-test-", ".tmp");

            return tempFile;
        } catch (IOException e) {
            REPORTER.error(e, GenericMessage.ERROR, "Failed to create temp file");
            return null;
        }
    }

    @BeforeAll
    static void init() throws Exception {
        rootCA = TestCertificateAuthority.create("Root CA");
    }

    @BeforeEach
    protected void setUp() {
        GlobalReporter.globalInstance().setSystemOutputEnabled(true);
        GlobalReporter.globalInstance().setLineConsumer(null);
        GlobalReporter.globalInstance()
            .getFormatConfig()
                .setIncludeTime(false)
                .setIncluedPid(false)
                .setIncluedThread(false)
                .setUseSimpleClassName(true);
    }

    @AfterEach
    protected void tearDown() throws IOException {
        // Do this here in case a test wants to set SPL level itself
        GlobalReporter.globalInstance().setLevel(Level.INFO);
    }

    protected Path getTempDir() {
        return tempDir;
    }
}
