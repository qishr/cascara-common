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

import io.github.qishr.cascara.common.i18n.LocalizableMessage;

public enum GenericMessage implements LocalizableMessage {
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
    STREAM_READ_ERROR("ERROR-108", "Failed to read from stream"),
    CONFIG_LOAD_FAILED("ERROR-109", "Failed to load config: {0}"),

    // JRE Exceptions
    EXCEPTION("ERROR-401", "Exception: {0}"),
    RUNTIME_EXCEPTION("ERROR-402", "Runtime exception: {0}"),
    NPE("ERROR-403", "Null pointer exception: {0}"),
    CLASS_NOT_FOUND("ERROR-404", "Class not found: {0}"),


    JRE_CLASS_CAST_EXCEPTION("ERROR-405", "Class cast exception: {0}"),
    CLASS_CAST_EXCEPTION("ERROR-406", "Class {0} cannot be cast to {1}"),


    NO_SUCH_METHOD("ERROR-417", "No such method: {0}"),
    NO_SUCH_CONSTRUCTOR("ERROR-418", "No such constructor: {0}"),
    INVOCATION_TARGET_EXCEPTION("ERROR-420", "Invocation target exception: {0}"),
    ILLEGAL_ARGUMENT_EXCEPTION("ERROR-421", "Iillegal argument exception {0}"),
    INSTANTIATION_EXCEPTION("ERROR-422", "Instantiation exception: {0}"),
    EXCEPTION_IN_INITIALIZER("ERROR-423", "Exception in initializer for {0}"),
    ABSTRACT_METHOD_ERROR("ERROR-424", "Abstract method error: {0}"),
    SERVICE_CONFIG_ERROR("ERROR-425", "Service configuration error: {0}"),
    SECURITY_EXCEPTION("ERROR-426", "Security exception: {0}"),
    ILLEGAL_ACCESS_EXCEPTION("ERROR-427", "Illegal access exception: {0}"),
    INACCESSIBLE_OBJECT_EXCEPTION("ERROR-428", "Inaccessible object exception: {0}"),
    INCONSISTENT_STATE("ERROR-429","Inconsistent state"),
    UNSUPPORTED_OPERATION("ERROR-430","Unsupported operation: {0}.{1}"),
    UNSATISFIED_LINK_ERROR("ERROR-431","Unsatisfied link error: {0"),
    INTERNAL_ERROR("ERROR-432","Internal error: {0}"),
    REFLECTIVE_OPERATION_EXCEPTION("ERROR-433","Reflective operation exception: {0}"),


    // TODO: These should probably be in SecurityMessage...
    CERTIFICATE_EXCEPTION("ERROR-433", "Certificate exception: {0}"),
    INVALID_ALGORITHM_PARAMETER_EXCEPTION("ERROR-434", "Invalid algorithm parameter exception: {0}"),
    NO_SUCH_ALGORITHM_EXCEPTION("ERROR-435", "No such algorithm exception: {0}"),


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
    NO_RESOURCE_PROVIDER("ERROR-501", "No resource provider for \"{0}\" URI scheme"),

    CLASS_NOT_SERIALIZABLE("ERROR-601", "Class {0} is not serializable");

    private final String code;
    private final String format;

    GenericMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}