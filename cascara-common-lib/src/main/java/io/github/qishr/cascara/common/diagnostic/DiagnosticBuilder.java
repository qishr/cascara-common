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

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.lang.token.Token;

@Experimental
public class DiagnosticBuilder {
    /// With format string. This is used for debug and trace reports. They do not carry a stack trace or cause.
    public static Diagnostic build(String source, Level level, String format, Object... details) {
        return new Diagnostic(
            null,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, level, null, null, null, format, details
        );
    }

    /// With diagnostic message, and cause
    public static Diagnostic build(String source, Level level, StackTraceElement[] stackTrace, Throwable cause, LocalizableMessage message, Object... details) {
        return new Diagnostic(
            null,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, level, stackTrace, cause, message, null, details
        );
    }

    /// With diagnostic message, location, and cause
    public static Diagnostic build(URI uri, int line, int column, int startOffset, int endOffset, String source, Level level, StackTraceElement[] stackTrace, Throwable cause, LocalizableMessage message, Object... details) {
        return new Diagnostic(uri, line, column, startOffset, endOffset, source, level, stackTrace, cause, message, null, details);
    }

    /// With diagnostic message, token, and cause
    public static Diagnostic build(Token token, String source, Level level, StackTraceElement[] stackTrace, Throwable cause, LocalizableMessage message, Object... details) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        return new Diagnostic(null, token, source, level, stackTrace, cause, message, null, details);
    }
}
