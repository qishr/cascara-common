package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatConfig;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;

public abstract class TestBase {
    // Environment variables
    public static final String EV_CASCARA_HOME = "CASCARA_HOME";
    public static final String EV_CASCARA_REPORT_ALLOW_API_OVERRIDE = "CASCARA_REPORT_ALLOW_API_OVERRIDE";
    public static final String EV_CASCARA_REPORT_CONSOLE_OUTPUT = "CASCARA_REPORT_CONSOLE_OUTPUT";

    // System properties
    public static final String SP_CASCARA_I18N_LOCALE = "cascara.i18n.locale";
    public static final String SP_CASCARA_I18N_LOCALIZE_EXCEPTIONS = "cascara.i18n.localize-exceptions";

    public static final String SP_CASCARA_LOG_FORMAT_INCLUDE_STACK_TRACE = "cascara.log.format.stack-trace";
    public static final String SP_CASCARA_REPORT_LEVEL_ = "cascara.report.level.";
    public static final String SP_CASCARA_REPORT_ALLOW_API_OVERRIDE = "cascara.report.allow-api-override";
    public static final String SP_CASCARA_REPORT_CONSOLE_OUTPUT = "cascara.report.console-output";

    public static final String SP_CASCARA_SPL_DISCOVER = "cascara.spl.discover";
    public static final String SP_CASCARA_SPL_BOOT_ERRORS_TO_CONSOLE = "cascara.spl.boot-errors-to-console";
    public static final String SP_CASCARA_SPL_BOOT_INFO_TO_CONSOLE = "cascara.spl.boot-info-to-console";
    public static final String SP_CASCARA_SPL_VERIFY = "cascara.spl.verify";
    public static final String SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED = "cascara.spl.policy.require-signed";
    public static final String SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED_CLASSES = "cascara.spl.policy.require-signed-classes";
    public static final String SP_CASCARA_SPL_POLICY_CHECK_EXPIRY = "cascara.spl.policy.check-expiry";


    // Properties file path system properties
    public static final String SP_CASCARA_REPORT_PROPERTIES = "cascara.report.properties";
    public static final String SP_CASCARA_SPL_PROPERTIES = "cascara.spl.properties";

    @TempDir
    private Path tempDir;

    private SPL rootLayer;
    public SPL spl;

    protected @TempDir Path homePath;
    private Path sharedPath;
    private Path modulePath;
    private Path tempPath;

    protected static TestCertificateAuthority rootCA;

    @BeforeAll
    static void init() throws Exception {
        // Do not call anything that acccesses the Cascara class or other Config classes from this method
    }

    @BeforeEach
    protected void setUp() {
        Cascara cascara;
        if (Cascara.isInitialized()) {
            cascara = Cascara.get();
            homePath = cascara.getHomePath();
        } else {
            homePath = tempDir.resolve(".cascara");
            cascara = Cascara.get(homePath);
        }

        sharedPath = cascara.getSharedPath();
        modulePath = cascara.getModulePath();
        tempPath = sharedPath.resolve("tmp");

        try {
            if (!Files.exists(sharedPath)) { Files.createDirectories(sharedPath); }
            if (!Files.exists(modulePath)) { Files.createDirectories(modulePath); }
            if (!Files.exists(tempPath)) { Files.createDirectories(tempPath); }
        } catch (IOException e) {
            e.printStackTrace();
        }

        rootLayer = SPL.getRoot();
        spl = rootLayer.create("baseTestLayer");

        try {
            if (rootCA == null) {
                rootCA = TestCertificateAuthority.create("Root CA");
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
        }

        GlobalReporter.get().setConsoleOutputEnabled(true);
        GlobalReporter.get().setLineConsumer(null);
        LogFormatConfig.get()
            .setIncludeTime(false)
            .setIncluedPid(false)
            .setIncluedThread(false)
            .setUseSimpleClassName(true);
    }

    @AfterEach
    protected void tearDown() throws IOException {
        // Do this here in case a test wants to set SPL level itself
        GlobalReporter.get().setLevel(Level.INFO);

        if (rootLayer != null && spl != null) {
            rootLayer.remove(spl.getName());
        }
    }

    protected Path getHomePath() {
        return homePath;
    }

    protected Path getSharedPath() {
        return sharedPath;
    }

    protected Path getModulePath() {
        return modulePath;
    }

    protected Path getTempPath() {
        return tempPath;
    }

    protected void displayHomeDir() {
        try (Stream<Path> walk = Files.walk(homePath)) {
            walk.filter(Files::isRegularFile).forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
