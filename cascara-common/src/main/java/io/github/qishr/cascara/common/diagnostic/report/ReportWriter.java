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

package io.github.qishr.cascara.common.diagnostic.report;

import java.io.IOException;
import java.io.Writer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;

public class ReportWriter extends Writer {
    private final AbstractReporter<?> reporter;
    private final Diagnostic.Level level;
    private final StringBuilder buffer = new StringBuilder();

    ReportWriter(AbstractReporter<?> reporter, Diagnostic.Level level) {
        this.reporter = reporter;
        this.level = level;
    }


    public void write(int indent, String str) {
        try {
            if (indent == 0) {
                write(str.toString(), 0, str.length());
                return;
            }
            StringBuilder sb = new StringBuilder();
            String[] lines = str.split("\n");
            for (int i = 0; i < lines.length; i++) {
                sb.append(" ".repeat(indent));
                sb.append(lines[i]);
                sb.append("\n");
            }
            write(sb.toString(), 0, sb.length());
        } catch (IOException e) {}
    }

    //
    //
    //

    @Override
    public void write(char[] cbuf, int off, int len) {
        // buffer.append(" ".repeat(indent));
        buffer.append(cbuf, off, len);
        flushLines(false);
    }

    @Override
    public void flush() {
        flushLines(true);
    }

    @Override public void close() { flush(); }

    //
    //
    //

    private void flushLines(boolean force) {
        int newlineIdx;
        while ((newlineIdx = buffer.indexOf("\n")) != -1) {
            String line = buffer.substring(0, newlineIdx).replace("\r", "");
            // reporter.displayLine(level, line, msgLineNumber);
            reporter.logLine(level, line);
            buffer.delete(0, newlineIdx + 1);
        }
        if (force && buffer.length() > 0) {
            // reporter.displayLine(level, buffer.toString(), msgLineNumber);
            reporter.logLine(level, buffer.toString());
            buffer.setLength(0);
        }
    }

    // void displayLine(String msgLine, int msgLineNumber) {
    //     flush();
    //     reporter.displayLine(level, msgLine, msgLineNumber);
    // }

    void logLine(String msgLine) {
        flush();
        reporter.logLine(level, msgLine);
    }

}