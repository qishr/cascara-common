// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class StandardReporter extends AbstractReporter<StandardReporter> {
    // Declaring ANSI_RESET so that we can reset the color
    private static final String ANSI_RESET = "\u001B[0m";

    private static final String ANSI_RED = "\u001B[31m";

    private static final String ANSI_GREEN = "\u001B[32m";

    private static final String ANSI_BLUE = "\u001B[34m";

    private static final String ANSI_YELLOW = "\u001B[33m";

    // private static final String ANSI_WHITE = "\u001B[37m";

    private static final String[] levelColors = new String[7];
    {
        // levelColors[Level.DEFAULT.ordinal()] = ANSI_WHITE;
        levelColors[Level.ERROR.ordinal()] = ANSI_RED;
        levelColors[Level.WARN.ordinal()] = ANSI_YELLOW;
        levelColors[Level.INFO.ordinal()] = ANSI_BLUE;
        // levelColors[Level.DEBUG.ordinal()] = ANSI_WHITE;
        // levelColors[Level.TRACE.ordinal()] = ANSI_WHITE;
    }

    private boolean ansiColoringEnabled;

    public StandardReporter(Consumer<String> writer) {
        super(writer);
    }

    public StandardReporter() {
        // Nothing to see here
    }

    public StandardReporter setAnsiColoringEnabled(boolean b) {
        ansiColoringEnabled = b;
        return this;
    }

    @Override
    protected StandardReporter self() { return this; }

    @Override
    protected void writeString(Diagnostic diagnostic) {
        writeString (
            diagnostic.getCause(),
            diagnostic.getLevel(),
            formatString(diagnostic)
        );
    }

    private String formatString(Diagnostic diagnostic) {
        Level diagnosticLevel = diagnostic.getLevel();
        URI diagnosticUri = diagnostic.getUri();
        int diagnosticLineNumber = diagnostic.getLine();

        boolean showUri = diagnosticUri == null;
        boolean showLineNumber = diagnosticLineNumber > 0;

        String[] lines = diagnostic.getMessage().split("\n");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            sb.append('[');
            if (ansiColoringEnabled) {
                String ansiCode = levelColors[diagnosticLevel.ordinal()];
                if (ansiCode != null) {
                    sb.append(ansiCode);
                    sb.append(diagnostic.getLevel().getLogPrefix());
                    sb.append(ANSI_RESET);
                } else {
                    sb.append(diagnostic.getLevel().getLogPrefix());
                }
            } else {
                sb.append(diagnostic.getLevel().getLogPrefix());
            }
            sb.append("] ");

            // First line is default color, subsequent lines from the same diagnostic message are green.
            if (ansiColoringEnabled && i > 0) {
                sb.append(ANSI_GREEN);
            }

            sb.append(lines[i]);
            if (showUri) {
                if (showLineNumber) {
                    sb.append(" at line ");
                    sb.append(diagnosticLineNumber);
                }
            } else {
                if (showLineNumber) {
                    sb.append(" at ");
                    sb.append(diagnosticLineNumber);
                    sb.append(':');
                    sb.append(diagnosticLineNumber);
                } else {
                    sb.append(" in file ");
                    sb.append(diagnosticUri);
                }
            }

            if (ansiColoringEnabled) {
                sb.append(ANSI_RESET);
            }

            sb.append('\n');
        }

        return sb.toString();
    }
}
