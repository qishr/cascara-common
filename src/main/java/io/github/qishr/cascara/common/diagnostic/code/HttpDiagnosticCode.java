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


package io.github.qishr.cascara.common.diagnostic.code;

public enum HttpDiagnosticCode implements DiagnosticCode {
    // Infrastructure Errors (Pre-response)
    // NAME_RESOLUTION_FAILED("HTTP-001", 1),
    // CONNECTION_TIMEOUT("HTTP-002", 1),
    // TLS_HANDSHAKE_FAILED("HTTP-003", 1),

    // Standard HTTP Mappings (Dynamic Catch-All or Explicit Enums)
    BAD_REQUEST("HTTP-400", "Bad Request: {0}"),
    FORBIDDEN("HTTP-403", "Forbidden: {0}"),
    NOT_FOUND("HTTP-404", "Not found: {0}"),
    SERVER_ERROR("HTTP-500", "Server error: {0}");

    private final String code;
    private final String message;

    HttpDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }

    /// Factory method to dynamically resolve or generate an HTTP code
    /// straight from a standard network response status code.
    public static String fromStatusCode(int statusCode) {
        return "HTTP-" + statusCode;
    }
}