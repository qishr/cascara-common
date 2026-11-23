package io.github.qishr.cascara.common.diagnostic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatConfig;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.LocalReporter;

public class LocalReporterTests {
    private LogFormatConfig format;

    // private boolean aLineContains(List<String> lines, String text) {
    //     for (String line : lines) {
    //         if (line.contains(text)) {
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    @BeforeEach
    void setUp() {
        format = new LogFormatConfig()
            .setIncludeTime(false)
            .setIncluedPid(false)
            .setIncluedThread(false)
            .setIncluedSource(true);
    }

    @Test
    void test_reportHasLevel() {
        LocalReporter reporter = new LocalReporter()
            .setConsoleOutputEnabled(false)
            .setLevel(Level.DEBUG);

        AtomicBoolean lineHasLevel = new AtomicBoolean();
        reporter.setLineConsumer(line -> {
            if (line.contains("DEBUG")) {
                lineHasLevel.set(true);
            }
        });

        reporter.debug("test");
        assertTrue(lineHasLevel.get());
    }

    @Test
    void test_reportHasStackTrace() {
        AtomicBoolean lineHasException = new AtomicBoolean();
        AtomicBoolean lineHasStackTrace = new AtomicBoolean();

        format.setIncludeStackTrace(true);

        LocalReporter reporter = new LocalReporter()
            .setFormatConfig(format)
            .setConsoleOutputEnabled(false)
            .setLineConsumer(line -> {
                if (line.contains("قشر")) {
                    lineHasException.set(true);
                }
                if (line.contains(".java")) {
                    lineHasStackTrace.set(true);
                }
            }
        );

        Throwable cause = new IllegalAccessError("قشر");
        reporter.error(cause, GenericMessage.ERROR, cause.getMessage());

        assertTrue(lineHasException.get());
        assertTrue(lineHasStackTrace.get());
    }

    @Test
    void test_format_prefixEveryLine_enabled() {
        format.setPrefixEveryLine(true);

        LocalReporter reporter = new LocalReporter()
            .setSource("LocalReporterTests")
            .setConsoleOutputEnabled(false)
            .setLevel(Level.DEBUG)
            .setFormatConfig(format);

        List<String> lines = new ArrayList<>();
        reporter.setLineConsumer(d -> {
            lines.add(d);
        });

        reporter.debug("Split1\n  Split2");

        assertTrue(lines.contains("[DEBUG] [LocalReporterTests] Split1"));
        assertTrue(lines.contains("[DEBUG]   Split2"));
    }

    @Test
    void test_format_prefixEveryLine_disabled() {
        format.setPrefixEveryLine(false);

        LocalReporter reporter = new LocalReporter()
            .setSource("LocalReporterTests")
            .setConsoleOutputEnabled(false)
            .setLevel(Level.DEBUG)
            .setFormatConfig(format);

        List<String> lines = new ArrayList<>();
        reporter.setLineConsumer(d -> {
            lines.add(d);
        });

        reporter.debug("Split1\n  Split2");

        assertTrue(lines.contains("[DEBUG] [LocalReporterTests] Split1"));
        assertTrue(lines.contains("  Split2"));

        // assertTrue(aLineContains(lines, "[LocalReporterTests] Split1"));
        // assertTrue(aLineContains(lines, "  Split2"));
    }

    // @Test
    // void test_format_source() {
    //     LocalReporter reporter = new LocalReporter()
    //         .setSystemOutputEnabled(false)
    //         .setLevel(Level.DEBUG);

    //     List<String> lines = new ArrayList<>();
    //     reporter.setLineConsumer(d -> {
    //         lines.add(d);
    //     });

    //     reporter.setSource("LocalReporterTests");
    //     reporter.setFormatConfig(new FormatConfig().setIncluedSource(true));
    //     reporter.debug("One");
    //     assertTrue(aLineContains(lines, "[LocalReporterTests] One"));
    // }
}
