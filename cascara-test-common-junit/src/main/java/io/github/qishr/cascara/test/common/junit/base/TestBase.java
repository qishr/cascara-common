package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;

public abstract class TestBase {
    // Environment variables
    public static final String EV_CASCARA_HOME = "CASCARA_HOME";

    // System properties
    public static final String SP_CASCARA_SPL_DISCOVER = "cascara.spl.discover";
    public static final String SP_CASCARA_SPL_BOOT_ERRORS_TO_CONSOLE = "cascara.spl.boot-errors-to-console";
    public static final String SP_CASCARA_SPL_VERIFY = "cascara.spl.verify";
    public static final String SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED = "cascara.spl.policy.require-signed";
    public static final String SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED_CLASSES = "cascara.spl.policy.require-signed-classes";
    public static final String SP_CASCARA_SPL_POLICY_CHECK_EXPIRY = "cascara.spl.policy.check-expiry";

    // Properties file path system properties
    public static final String SP_CASCARA_REPORT_PROPERTIES = "cascara.report.properties";
    public static final String SP_CASCARA_SPL_PROPERTIES = "cascara.spl.properties";

    // private static final Reporter REPORTER = Reporter.forClass(TestBase.class);

    @TempDir
    public Path tempDir;

    protected static TestCertificateAuthority rootCA;

    protected Path createTempFile() {

        try {
            Path tempFile = Files.createTempFile(tempDir, "cascara-test-", ".tmp");

            return tempFile;
        } catch (IOException e) {
            System.err.println("Failed to create temp file");
            return null;
        }
    }

    @BeforeAll
    static void init() throws Exception {
        // Do not call anything that acccesses the Cascara class or other Config classes from this method
    }

    @BeforeEach
    protected void setUp() {
        try {
            if (rootCA == null) {
                rootCA = TestCertificateAuthority.create("Root CA");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
