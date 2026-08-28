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

public enum LangDiagnosticCode implements DiagnosticCode {
    EXPECTED_STREAM_START("LANG-101", "Expected stream start."),
    EXPECTED_STREAM_END("TOKEN-102", "Expected steam end."),
    UNEXPECTED_STREAM_END("TOKEN-103", "Unexpected stream end."),

    UNKNOWN_NODE_TYPE("LANG-201", "Unknown AST node type: {0}"),

    // Serializer
    FAILED_TO_MAP_TYPE("LANG-401", "Failed to map {0} to YAML AST: {1}"),
    FAILED_TO_MAP_AST("LANG-402", "Failed to map YAML AST to {0}: {1}"),
    EXPECTED_MAP_STRUCTURE("LANG-303", "Expected a map structure for class {0}"),
    FAILED_SERIALIZE("LANG-304", "Failed to serialize: {0}"),
    FAILED_DESERIALIZE("LANG-305", "Failed to deserialize: {0}: {1}."),
    WRONG_FORMAT("LANG-306", "Data {0} does not conform to format {1}"),
    INCOMPATIBLE_TYPES("LANG-307", "Incompatible types: Cannot map {0} to Java type {1}"),
    FAILED_DESERIALIZE_SCALAR("LANG-308", "Failed to deserialize scalar to {0}: {1}"),
    UNSUPPORTED_TYPE("LANG-309", "Unsupported field type: {0}"),
    EXPECTED_SEQUENCE("LANG-310", "Expected a sequence for field: {0}"),
    FIELD_NOT_ACCESSIBLE_REASON("LANG-311", "Field {0} is not accessible: {1}"),

    // JRE Exceptions
    CLASS_NOT_SERIALIZABLE("LANG-402", "Class {0} is not serializable"),
    NO_SUCH_METHOD("LANG-404", "No such method: {0}"),
    NO_SUCH_CONSTRUCTOR("LANG-405", "No such constructor: {0}"),
    FIELD_NOT_ACCESSIBLE("LANG-406", "Field {0} is not accessible"),
    INVOCATION_TARGET_EXCEPTION("LANG-407", "Method {0} threw an invocation target exception"),
    ILLEGAL_ARGUMENT_EXCEPTION("LANG-408", "Field {0} threw an illegal argument exception"),
    INSTANTIATION_EXCEPTION("LANG-409", "Field {0} threw an instantiation exception"),
    EXCEPTION_IN_INITIALIZER("LANG-410", "Exception in initializer for {0}"),

    // TODO: Where are these used?
    NOT_ARRAY_OR_OBJECT("LANG-501", "Value {0} is not an array or object therefore its key {1} cannot be resolved"),
    OUT_OF_BOUNDS("LANG-502", "index {0} is out of bounds - the array has {1} elements"),
    ERROR_READING_VALUE_AT("LANG-503", "Error reading value at index position {0"),
    NOT_AN_ARRAY_INDEX("LANG-504", "{0} is not an array index");

    private final String code;
    private final String message;

    LangDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}