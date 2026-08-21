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
import java.nio.file.Path;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.util.TermUtils;
import io.github.qishr.cascara.common.util.UriScheme;

public class StandardReporter extends AbstractReporter<StandardReporter> {

    public StandardReporter(Consumer<String> writer) {
        super(writer);
    }

    public StandardReporter() {
        // Nothing to see here
    }

    @Override
    protected StandardReporter self() { return this; }


    @Override
    protected String formatMessage(Diagnostic diagnostic, String msgLine, int msgLineNumber, boolean colorize) {
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

        StringBuilder sb = new StringBuilder();

        // showDiagnosticCodes
        if (showProblemCodes && diagnostic.getLevel().isProblem() && msgLineNumber == 0) {
            String msgCode = diagnostic.getCode().getCode();
            sb.append("[");
            if (colorize) {

                sb.append(TermUtils.ANSI_WHITE);
                sb.append(msgCode);
                sb.append(TermUtils.ANSI_RESET);
                // sb.append(colorStack.peek());

            } else {
                sb.append(msgCode);
            }
            sb.append("] ");
        }

        // // First line is default color, subsequent lines from the same diagnostic message are green.
        // if (colorize && msgLineNumber > 0) {
        //     sb.append(TermUtils.ANSI_GREEN);
        // }

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
                sb.append(" at line ");
                sb.append(diagnosticLineNumber);
                if (diagnostic.getColumn() > 0) {
                    sb.append(":");
                    sb.append(diagnostic.getColumn());
                }
            }
        }

        if (colorize) {
            sb.append(TermUtils.ANSI_RESET);
        }

        sb.append('\n');

        return sb.toString();
    }
}
