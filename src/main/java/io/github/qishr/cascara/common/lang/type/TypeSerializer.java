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


package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.exception.SerializerException;

public interface TypeSerializer<T> extends TypeDescriptor<T> {
    /// Transforms a concrete Java object into its structural AST representation.
    ///
    /// @param jvmInstance The live runtime object instance to serialize.
    /// @return            The matching structural AstNode graph.
    AstNode serialize(T jvmInstance) throws SerializerException;

    /// Deserializes an AST node into a strongly-typed Java object.
    ///
    /// @param astNode The structural AST node being parsed (e.g., YamlScalarNode, YamlMapNode).
    /// @return        The fully constructed Java object instance.
    /// @throws SerializerException If the node structure violates the type constraints.
    public abstract T deserialize(AstNode astNode) throws SerializerException;

    /// Type serializers don't need to validate, but they do
    /// everything else a type descriptor does, so we provide a
    /// default validate method here so implementations don't need to.
    default boolean validate(String text, Reporter collector) {
        return true;
    }
}
