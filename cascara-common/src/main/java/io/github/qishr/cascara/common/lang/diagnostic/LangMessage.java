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


package io.github.qishr.cascara.common.lang.diagnostic;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public enum LangMessage implements DiagnosticMessage {
    EXPECTED_STREAM_START("LANG-101", "Expected stream start."),
    EXPECTED_STREAM_END("TOKEN-102", "Expected steam end."),
    UNEXPECTED_STREAM_END("TOKEN-103", "Unexpected stream end."),

    UNKNOWN_NODE_TYPE("LANG-201", "Unknown AST node type: {0}"),

    // Serializer
    FAILED_TO_MAP_TYPE("LANG-401", "Failed to map {0} to AST: {1}"),
    FAILED_TO_MAP_AST("LANG-402", "Failed to map AST to {0}: {1}"),
    EXPECTED_MAP_STRUCTURE("LANG-303", "Expected a map structure for {0}"),
    FAILED_SERIALIZE("LANG-304", "Failed to serialize: {0}"),
    FAILED_DESERIALIZE("LANG-305", "Failed to deserialize: {0}: {1}."),
    WRONG_FORMAT("LANG-306", "Data {0} does not conform to format {1}"),
    INCOMPATIBLE_TYPES("LANG-307", "Incompatible types: Cannot map {0} to Java type {1}"),
    FAILED_DESERIALIZE_SCALAR("LANG-308", "Failed to deserialize scalar to {0}: {1}"),
    FAILED_DESERIALIZE_SCALAR_TO_NON_SCALAR("LANG-309", "Failed to deserialize scalar field to non-scalar type '{0}'"),
    FAILED_DESERIALIZE_SCALAR_TO_NON_SCALAR_NAMED("LANG-310", "Failed to deserialize scalar field '{0}' to non-scalar type '{1}'"),
    UNSUPPORTED_TYPE("LANG-311", "Unsupported field type: {0}"),
    EXPECTED_SEQUENCE("LANG-312", "Expected a sequence for field: {0}"),
    EXPECTED_SCALAR("LANG-313", "Expected a scalar but found '{0}'"),
    EXPECTED_SCALAR_NAMED("LANG-314", "Expected a scalar for field '{0}' but found '{1}'"),
    FIELD_NOT_ACCESSIBLE_REASON("LANG-315", "Field {0} is not accessible: {1}"),
    AMBIGUOUS_POLYMORPHIC("LANG-316", "Ambiguous polymorphic type resolution for base class {0}"),
    NON_SCALAR_KEY("LANG-317", "Non-scalar key not supported {0}"),
    AST_FACTORY_FAIL_SCALAR("LANG-318", "AST factory failed to create scalar"),
    AST_FACTORY_FAIL_MAP("LANG-319", "AST factory failed to create map"),
    AST_FACTORY_FAIL_SEQUENCE("LANG-320", "AST factory failed to create sequence"),
    AST_FACTORY_FAIL_KEY("LANG-321", "AST factory failed to create key"),

    // JRE Exceptions
    CLASS_NOT_SERIALIZABLE("LANG-402", "Class {0} is not serializable"),
    NO_SUCH_METHOD("LANG-404", "No such method: {0}"),
    NO_SUCH_CONSTRUCTOR("LANG-405", "No such constructor: {0}"),
    FIELD_NOT_ACCESSIBLE("LANG-406", "Field {0} is not accessible"),
    INVOCATION_TARGET_EXCEPTION("LANG-407", "Method {0} threw an invocation target exception"),
    ILLEGAL_ARGUMENT_EXCEPTION("LANG-408", "Field {0} threw an illegal argument exception"),
    INSTANTIATION_EXCEPTION("LANG-409", "Field {0} threw an instantiation exception"),
    EXCEPTION_IN_INITIALIZER("LANG-410", "Exception in initializer for {0}"),
    ABSTRACT_METHOD_ERROR("LANG-411", "Abstract method error: {0}"),
    NO_CLASS_DEF_FOUND_ERROR("LANG-412", "No class definition found: {0}"),
    SERVICE_CONFIG_ERROR("LANG-413", "Service configuration error: {0}"),
    SECURITY_EXCEPTION("LANG-414", "Security exception: {0}"),
    ILLEGAL_ACCESS_EXCEPTION("LANG-415", "Illegal access exception: {0}"),
    CLASS_CAST_EXCEPTION("LANG-416", "Class cast exception: {0}"),

    UNKNOWN_GENERIC_TYPE("LANG-501", "Could not resolve generic type argument for {0}");

    private final String code;
    private final String format;

    LangMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}