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


package io.github.qishr.cascara.common.lang.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.exception.SerializerException;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeReference;

/// Handles the transformation between Java Objects (POJOs) and the AST or textual formats.
///
/// This interface provides a high-level API for data binding, allowing users to
/// move seamlessly between raw objects, structured ASTs, and the final string output.
///
/// @param <N> The specific subtype of AstNode used by the language implementation.
public interface Serializer<N extends AstNode> extends Processor {
    /// Converts a Java Object directly into its textual representation,
    /// for example JSON or YAML.
    ///
    /// @param jvmInstance The object to serialize.
    /// @return The formatted string (e.g., YAML or JSON).
    /// @throws SerializerException If serialization fails.
    String toString(Object jvmInstance) throws SerializerException;

    void toWriter(Object jvmInstance, Writer writer) throws IOException;

    /// Transforms a Java Object into an AST representation.
    ///
    /// @param jvmInstance The POJO or collection to transform.
    /// @return An AST representation of the provided object.
    /// @throws SerializerException If the object cannot be mapped to the AST.
    N toAst(Object jvmInstance);

    /// Parses a string (e.g. JSON or YAML) directly into a Java Object
    // of the specified type.
    ///
    /// @param text  The source text to parse and deserialize.
    /// @param jvmType The target type.
    /// @param <C>   The type of the resulting object.
    /// @return A populated instance of the requested class.
    /// @throws SerializerException If parsing or mapping fails.
    <C> C fromString(String text, Class<C> jvmType) throws SerializerException;

    <C> C fromText(String text, TypeReference<C> typeRef) throws SerializerException;

    <C> C fromReader(Reader reader, Class<C> jvmType) throws SerializerException;

    <C> C fromReader(Reader reader, TypeReference<C> typeRef) throws SerializerException;

    <C> C fromStream(InputStream is, Class<C> jvmType) throws SerializerException;

    <C> C fromStream(InputStream is, TypeReference<C> typeRef) throws SerializerException;

    /// Transforms an AST representation back into a specific Java type.
    ///
    /// @param astNode  The root AST node to interpret.
    /// @param jvmType The target type to instantiate and populate.
    /// @param <C>   The type of the resulting object.
    /// @return A populated instance of the requested class.
    /// @throws SerializerException If the AST structure does not match the target type.
    <C> C fromAst(N astNode, Class<C> jvmType);

    <C> C fromAst(N astNode, TypeReference<C> typeRef);

    Serializer<N> registerTypeDescriptor(TypeDescriptor<?> typeDescriptor);
    Serializer<N> setParser(AstParser<N,?,?> parser);
}
