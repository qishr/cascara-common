package io.github.qishr.cascara.common.diagnostic;

import java.util.Arrays;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class Reporter {
    Level level = Level.INFO;

    private Logger logger = null;

    public Reporter(Logger logger) {
        this.logger = logger;
    }

    public Reporter() {
        // Nothing to see here
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    /// Reports an trace message through the reporter.
    /// @param m The message to report.
    public void reportTrace(Object... m) {
        if (level == Level.TRACE) {
            print(Level.TRACE, m);
        }
    }

    /// Reports an debug message through the reporter.
    /// @param m The message to report.
    public void reportDebug(Object... m) {
        if (level.compareTo(Level.DEBUG) >= 0) {
            print(Level.DEBUG, m);
        }
    }

    /// Reports an informational message through the reporter.
    /// @param m The message to report.
    public void reportInfo(Object... m) {
        if (level.compareTo(Level.INFO) >= 0) {
            print(Level.INFO, m);
        }
    }

    /// Reports a warning message including location information.
    /// @param m The warning message to report.
    public void reportWarning(Object... m) {
        if (level.compareTo(Level.WARNING) >= 0) {
            print(Level.WARNING, m);
        }
    }

    /// Reports an error message including location information.
    /// @param m The error message to report.
    public void reportError(Object... m) {
        print(Level.ERROR, m);
    }

    private void print(Level kind, Object... s) {
        if (s == null || s.length == 0) {
            print(Level.ERROR, "Null or empty message");
        } else if (s[0] instanceof String m) {
            if (s.length == 1) {
                write("[" + kind + "] " + m + "\n");
            } else {
                String message = String.format(m, Arrays.<Object>copyOfRange(s, 1, s.length));
                write("[" + kind + "] " + message + "\n");
            }
        } else {
            print(Level.ERROR, "First parameter must be of type String");
        }
    }

    private void write(String text) {
        if (logger != null) {
            logger.write(text);
        } else {
            System.err.print(text);
        }
    }
}
