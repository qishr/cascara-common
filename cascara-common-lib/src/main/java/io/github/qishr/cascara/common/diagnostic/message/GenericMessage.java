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


package io.github.qishr.cascara.common.diagnostic.message;

public enum GenericMessage implements DiagnosticMessage {
    INFO("INFO-101", "{0}"),

    // Warnings
    WARN("WARN-101", "{0}"),

    // Errors
    ERROR("ERROR-101", "{0}"),
    UNIMPLEMENTED_METHOD("ERROR-102", "Unimplemented method: {0}.{1}"),
    UNEXPECTED_NULL_PARAMETER("ERROR-103","Unexpected null paramter \"{1}\" in {0}"),
    UNEXPECTED_NULL_RETURN("ERROR-104", "Unexpected null return value from {0}.{1}"),
    MANIFEST_NOT_FOUND("ERROR-105", "Manifest not found for {0}"),
    MANIFEST_READ("ERROR-106", "Failed to read manifest for {0}"),
    RESOURCE_INACCESSIBLE("ERROR-107", "Resource '{0}' cannot be loaded. Module " +
        "'{1}' does not open package '{2}' to module '{3}'. " +
        "To fix this, add 'opens {4} to {5};' to the module-info.java of '{6}'"),

    // JRE Exceptions
    EXCEPTION("ERROR-401", "Exception: {0}"),
    RUNTIME_EXCEPTION("ERROR-402", "Runtime exception: {0}"),
    NPE("ERROR-403", "Null pointer exception: {0}"),
    NO_CLASS_DEF_FOUND_ERROR("LANG-404", "No class definition found: {0}"),
    CLASS_CAST_EXCEPTION("LANG-405", "Class cast exception: {0}"),
    CLASS_NOT_SERIALIZABLE("LANG-406", "Class {0} is not serializable"),
    NO_SUCH_METHOD("LANG-407", "No such method: {0}"),
    NO_SUCH_CONSTRUCTOR("LANG-408", "No such constructor: {0}"),
    FIELD_NOT_ACCESSIBLE("LANG-409", "Field {0} is not accessible"),
    INVOCATION_TARGET_EXCEPTION("LANG-410", "Method {0} threw an invocation target exception"),
    ILLEGAL_ARGUMENT_EXCEPTION("LANG-411", "Field {0} threw an illegal argument exception"),
    INSTANTIATION_EXCEPTION("LANG-412", "Field {0} threw an instantiation exception"),
    EXCEPTION_IN_INITIALIZER("LANG-413", "Exception in initializer for {0}"),
    ABSTRACT_METHOD_ERROR("LANG-414", "Abstract method error: {0}"),
    SERVICE_CONFIG_ERROR("LANG-415", "Service configuration error: {0}"),
    SECURITY_EXCEPTION("LANG-416", "Security exception: {0}"),
    ILLEGAL_ACCESS_EXCEPTION("LANG-417", "Illegal access exception: {0}"),
    INCONSISTENT_STATE("ERROR-418","Inconsistent state"),
    UNSUPPORTED_OPERATION("ERROR-419","Unsupported operation: {0}.{1}"),
    UNSATISFIED_LINK_ERROR("ERROR-420","Unsatisfied link error: {0"),
    INTERNAL_ERROR("ERROR-421","Internal error: {0}"),

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

    // INVALID_SCHEMA_URI("SCHEMA-103", "Not a valid schema URI: {0}."),
    // UNRECOGNIZED_LIFECYCLE("SCHEMA-104", "Unrecognized schema lifecycle: {0}."),
    // MISSING_MODULE_NAME("SCHEMA-105", "Missing module name"),
    // MISSING_SCHEMA_NAME("SCHEMA-106", "Missing schema name"),
    // MISSING_VERSION("SCHEMA-107", "Missing version"),

    // Resource Errors
    NO_RESOURCE_PROVIDER("ERROR-501", "No resource provider for \"{0}\" URI scheme");


    private final String code;
    private final String format;

    GenericMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}