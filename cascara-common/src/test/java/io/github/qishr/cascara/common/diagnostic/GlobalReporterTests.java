package io.github.qishr.cascara.common.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.StringProperty;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
public class GlobalReporterTests {
@BeforeEach
    void resetGlobalReporter() {
        GlobalReporter.globalInstance().setLineConsumer(null);
        GlobalReporter.globalInstance().setLevel(GlobalReporterTests.class.getName(), Level.INFO);
    }

    @Test
    void test_reportHasClassNameAndMessage() {
        List<String> lines = new ArrayList<>();
        GlobalReporter.globalInstance().setLineConsumer(line -> lines.add(line));

        GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
        reporter.info(GenericMessage.INFO, "foo");

        assertEquals(1, lines.size());
        String logLine = lines.getFirst();
        assertTrue(logLine.contains(GlobalReporterTests.class.getSimpleName()));
        assertTrue(logLine.contains("foo"));
    }

    @Test
    void test_debugReportDoesNotAppearAtInfoLevel() {
        List<String> lines = new ArrayList<>();
        GlobalReporter.globalInstance().setLineConsumer(line -> lines.add(line));

        GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
        reporter.debug("foo");

        assertEquals(0, lines.size());
    }

    @Test
    void test_fails_setNonLocalLevelViaLocalInstance() {
        GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
        assertThrows(UnsupportedOperationException.class,
            () -> reporter.setLevel(ExceptionTests.class.getName(), Level.DEBUG)
        );
    }

    @Test
    void test_setLocalLevelViaLocalInstance() {
        List<String> lines = new ArrayList<>();
        GlobalReporter.globalInstance().setLineConsumer(line -> lines.add(line));

        GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
        reporter.setLevel(Level.DEBUG);
        reporter.debug("foo");

        assertEquals(1, lines.size());
        String logLine = lines.getFirst();
        assertTrue(logLine.contains(GlobalReporterTests.class.getSimpleName()));
        assertTrue(logLine.contains("foo"));
    }

    @Test
    void test_setLocalLevelViaGlobalInstance() {
        List<String> lines = new ArrayList<>();
        GlobalReporter.globalInstance().setLevel(GlobalReporterTests.class.getName(), Level.DEBUG);
        GlobalReporter.globalInstance().setLineConsumer(line -> lines.add(line));

        GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
        reporter.debug("foo");

        assertEquals(1, lines.size());
        String logLine = lines.getFirst();
        assertTrue(logLine.contains(GlobalReporterTests.class.getSimpleName()));
        assertTrue(logLine.contains("foo"));
    }

    @Test
    void test_setLocalLevelViaProperties() {
        List<String> lines = new ArrayList<>();

        Properties properties = new Properties()
            .add(new StringProperty(
                "casc.report.level." + GlobalReporterTests.class.getName(),
                "DEBUG"));

        GlobalReporter.globalInstance().setLevels(properties);
        GlobalReporter.globalInstance().setLineConsumer(line -> lines.add(line));

        GlobalReporter reporter1 = GlobalReporter.forClass(GlobalReporterTests.class);
        reporter1.debug("foo");

        GlobalReporter reporter2 = GlobalReporter.forClass(ExceptionTests.class);
        reporter2.debug("foo");

        assertEquals(1, lines.size());
        String logLine = lines.getFirst();
        assertTrue(logLine.contains(GlobalReporterTests.class.getSimpleName()));
        assertTrue(logLine.contains("foo"));
    }

    @Disabled
    @Test
    void test_setLocalLevelViaSystemProperty() {
        List<String> lines = new ArrayList<>();
        GlobalReporter.globalInstance().setLineConsumer(lines::add);

        String key = "casc.report.level." + GlobalReporterTests.class.getName();
        System.setProperty(key, "DEBUG");

        try {
            // Execute test against reporter reading system properties
            GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
            reporter.debug("foo");

            assertEquals(1, lines.size());
            assertTrue(lines.getFirst().contains("foo"));
        } finally {
            System.clearProperty(key);
        }
    }

    @Disabled
    @Test
    void test_environmentVariableConfiguration() {
        List<String> lines = new ArrayList<>();

        String key = "CASC_REPORT_LEVEL_" + GlobalReporterTests.class.getName().replace('.', '_').toUpperCase();
        setEnv(key, "DEBUG");

        GlobalReporter globalInstance = GlobalReporter.globalInstance();
        GlobalReporter.globalInstance().setLineConsumer(lines::add);

        try {
            // Execute test against reporter reading system properties
            GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTests.class);
            reporter.debug("foo");

            assertEquals(1, lines.size());
            assertTrue(lines.getFirst().contains("foo"));
        } finally {
            unsetEnv(key);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) {
        try {
            Class<?> peClass = Class.forName("java.lang.ProcessEnvironment");

            // Primary environment map (all OSes)
            java.lang.reflect.Field envField = peClass.getDeclaredField("theEnvironment");
            envField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) envField.get(null);
            env.put(key, value);

            // Windows-specific case-insensitive map
            try {
                java.lang.reflect.Field ciEnvField = peClass.getDeclaredField("theCaseInsensitiveEnvironment");
                ciEnvField.setAccessible(true);
                Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
                ciEnv.put(key, value);
            } catch (NoSuchFieldException ignored) {
                // Field only exists on Windows - safe to ignore on macOS/Linux
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set environment variable: " + key, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void unsetEnv(String key) {
        try {
            Class<?> peClass = Class.forName("java.lang.ProcessEnvironment");

            // Primary environment map (all OSes)
            java.lang.reflect.Field envField = peClass.getDeclaredField("theEnvironment");
            envField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) envField.get(null);
            env.remove(key);

            // Windows-specific case-insensitive map
            try {
                java.lang.reflect.Field ciEnvField = peClass.getDeclaredField("theCaseInsensitiveEnvironment");
                ciEnvField.setAccessible(true);
                Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
                ciEnv.remove(key);
            } catch (NoSuchFieldException ignored) {
                // Field only exists on Windows - safe to ignore on macOS/Linux
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to unset environment variable: " + key, e);
        }
    }
}
