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

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public abstract class LocatableException extends LocalizableRuntimeException {
    // Common constants for LocatableException implementations

    private final int line;
    private final int column;
    private final URI uri;
    private final String message;
    private final String rawMessage;

    /// Standard Constructor
    public LocatableException(URI uri, int line, int column, Throwable cause, DiagnosticCode code, Object... details) {
        super(cause, code, details);
        this.message = messageWithLocation(code.getMessage(), line, uri);
        this.rawMessage = code.getMessage();
        this.line = line;
        this.column = column;
        this.uri = uri;
    }

    /// Standard constructor for parser-detected logic errors
    public LocatableException(URI uri, int line, int column, DiagnosticCode code, Object... details) {
        this(uri, line, column, null, code, details);
    }

    /// Constructor for when we only have a URI but no line or column
    public LocatableException(URI uri, Throwable cause, DiagnosticCode code, Object... details) {
        this(uri, Diagnostic.UNKNOWN_COORD, Diagnostic.UNKNOWN_COORD, cause, code, details);
    }

    private static String messageWithLocation(String message, int line, URI uri) {
        if (uri == null) {
            return String.format("%s at line %d", message, line);
        } else {
            return String.format("%s at %s:%d", message, uri.toString(), line);
        }
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public URI getUri() { return uri; }
    public String getRawMessage() { return rawMessage; }

    public String getMessage() {
        String baseMessage = super.getMessage();
        if (uri == null) {
            if (line > 0) {
                return String.format("%s at line %d", baseMessage, line);
            } else {
                return baseMessage;
            }
        } else {
            if (line > 0) {
                return String.format("%s in %s:%d", baseMessage, uri.toString(), line);
            } else {
                return String.format("%s in %s", baseMessage, uri.toString());
            }
        }
    }


    @Override
    public String getLocalizedMessage() {
        String baseMessage = super.getLocalizedMessage();
        if (uri == null) {
            // TODO: i18n this
            if (line > 0) {
                return String.format("%s at line %d", baseMessage, line);
            } else {
                return baseMessage;
            }
        } else {
            // TODO: i18n this
            if (line > 0) {
                return String.format("%s in %s:%d", baseMessage, uri.toString(), line);
            } else {
                return String.format("%s in %s", baseMessage, uri.toString());
            }
        }
    }
}