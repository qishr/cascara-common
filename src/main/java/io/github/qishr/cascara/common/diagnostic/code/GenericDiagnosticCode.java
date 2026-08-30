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

public enum GenericDiagnosticCode implements DiagnosticCode {
    INFO("INFO-101", "{0}"),

    // Warnings
    WARN("WARN-101", "{0}"),
    UNIMPLEMENTED_METHOD("WARN-102", "Unimplemented method: {0}.{1}"),

    // Errors
    ERROR("ERROR-101", "Error: {0}."),
    UNEXPECTED_NULL_PARAMETER("ERROR-102","Unexpected null paramter \"{1}\" in {0}"),
    UNEXPECTED_NULL_RETURN("ERROR-103", "Unexpected null return value from {0}.{1}"),
    MANIFEST_NOT_FOUND("ERROR-104", "Manifest not found for {0}"),
    MANIFEST_READ("ERROR-105", "Failed to read manifest for {0}"),
    RESOURCE_INACCESSIBLE("ERROR-106", "Resource '{0}' cannot be loaded. Module " +
        "'{1}' does not open package '{2}' to module '{3}'. " +
        "To fix this, add 'opens {4} to {5};' to the module-info.java of '{6}'."),

    // Exceptions
    EXCEPTION("ERROR-202", "Exception: {0}"),
    RUNTIME_EXCEPTION("ERROR-203", "Runtime exception: {0}"),
    NPE("ERROR-204", "Null pointer exception: {0}"),
    INCONSISTENT_STATE("ERROR-206","Inconsistent state"),
    UNSUPPORTED_OPERATION("ERROR-207","Unsupported operation: {0}.{1}"),

    // IO Errors
    IO_ERROR("ERROR-301", "IO error: {0}"),
    INTERRUPT_ERROR("ERROR-301", "Interrupt error: {0}"),

    // Formatting Errors
    FORMAT_ERROR("ERROR-401", "Format error: {0}"),
    INVALID_URI("ERROR-402", "Invalid URI: {0}"),
    UNKNOWN_URI_SCHEME("ERROR-403", "Unknown URI scheme: {0}"),
    MALFORMED_BASE64("ERROR-404", "Malformed Base64 payload"),
    MESSAGE_FORMATTING_ERROR("ERROR-405", "Problem encountered while formatting message with key {0}: {1}"),
    DIAGNOSTIC_FORMATTING_ERROR("ERROR-406", "Problem encountered while formatting error with code {0}: {1}"),

    // Resource Errors
    NO_RESOURCE_PROVIDER("ERROR-501", "No resource provider");


    private final String code;
    private final String message;

    GenericDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}