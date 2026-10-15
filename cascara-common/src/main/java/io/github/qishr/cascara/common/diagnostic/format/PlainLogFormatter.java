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

package io.github.qishr.cascara.common.diagnostic.format;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.util.UriScheme;

public class PlainLogFormatter implements LogFormatter {
    protected static final ZoneId UTC = ZoneId.of("UTC");
    protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    protected boolean prefixEveryLine = true;
    protected boolean showProblemCodes = false;
    protected long processId;

    public PlainLogFormatter() {
        init();
    }

    private void init() {
        processId = ProcessHandle.current().pid();
    }

    public PlainLogFormatter withFields(Set<Field> fields) {
        return this;
    }

    public String format(Diagnostic diagnostic, boolean stackTraceEnabled) {
        StringBuilder sb = new StringBuilder();
        String[] lines = diagnostic.getFormattedMessage().split("\n");
        for (int i = 0; i < lines.length; i++) {
            formatLine(sb, diagnostic, lines[i], i);
        }
        if (stackTraceEnabled && diagnostic.getStackTrace() != null && diagnostic.getStackTrace().length > 0) {
            sb.append(formatStackTrace(diagnostic.getLevel(), diagnostic.getStackTrace(), diagnostic.getCause()));
        }
        return sb.toString();
    }

    @Experimental
    public PlainLogFormatter setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    protected void formatLine(StringBuilder sb, Diagnostic diagnostic, String msgLine, int msgLineNumber) {
        int diagnosticLineNumber = diagnostic.getLine();
        boolean showLineNumber = diagnosticLineNumber > 0;
        boolean showUri = false;

        String resource = null;
        URI diagnosticUri = diagnostic.getUri();
        if (diagnosticUri != null) {
            resource = UriScheme.of(diagnosticUri) == UriScheme.FILE
                ? Path.of(diagnostic.getUri()).toString()
                : diagnostic.getUri().toString();
            showUri = true;
        }

        Level level = diagnostic.getLevel();
        String timeStamp = ZonedDateTime.ofInstant(diagnostic.getTimestamp(), UTC).format(TIME_FORMAT);

        if (prefixEveryLine || msgLineNumber == 0) {
            sb.append("[");
            sb.append(level.getLogPrefix());
            sb.append("] ");
        } else {
            sb.append("        ");
        }

        if (msgLineNumber == 0) {

            sb.append("[");
            sb.append(timeStamp);
            sb.append("] ");

            sb.append("[");
            sb.append(diagnostic.getProcessId());
            sb.append("] ");

            sb.append("[");
            sb.append(diagnostic.getSource());
            sb.append("] ");

            if (showProblemCodes && diagnostic.getLevel().isProblem()) {
                String msgCode = diagnostic.getMessage().getCode();
                sb.append("[");
                sb.append(msgCode);
                sb.append("] ");
            }
        }

        sb.append(msgLine);

        if (showUri) {
            if (showLineNumber) {
                sb.append(" at ");
                sb.append(resource);
                sb.append(':');
                sb.append(diagnosticLineNumber);
            } else {
                sb.append(" in file ");
                sb.append(resource);
            }
        } else {
            if (showLineNumber) {
                if (diagnostic.getColumn() > 0) {
                    sb.append(" at ");
                    sb.append(diagnosticLineNumber);
                    sb.append(":");
                    sb.append(diagnostic.getColumn());
                } else {
                    sb.append(" at line ");
                    sb.append(diagnosticLineNumber);
                }
            }
        }

        sb.append('\n');
    }

    @Override
    public String formatStackTrace(Level level, StackTraceElement[] stackTrace, Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement frame : stackTrace) {
            String msgLine = String.format(
                "  at %s.%s(%s:%d)",
                frame.getClassName(),
                frame.getMethodName(),
                frame.getFileName(),
                frame.getLineNumber()
            );
            if (prefixEveryLine) {
                sb.append("[");
                sb.append(level.getLogPrefix());
                sb.append("] ");
            } else {
                sb.append("        ");
            }
            sb.append(msgLine);
            sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
