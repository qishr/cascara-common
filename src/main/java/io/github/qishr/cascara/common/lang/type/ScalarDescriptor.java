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

import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public interface ScalarDescriptor<T> extends TypeDescriptor<T> {
    /// Converts the `text` into the JVM type specified by this TypeDescriptor.
    T toJvmType(String text);

    /// Converts the JVM type specified by this TypeDescriptor into a JSON Schema primitive type.
    /// @return One of: null, string, integer (JVM Long), number (JVM Double), boolean.
    Object toPrimitive(T jvmInstance);

    /// Retuns the JSON Schema `format` used by this TypeDescriptor.
    String getFormat();

    /// Returns the JSON Schema `contentEncoding` use by this TypeDescriptor.
    String getContentEncoding();

    @Override
    default void populateSchema(MapAstNode<?,?,?> node) {
        // Automatically inject the core properties every scalar might declare.
        node.put("type", getSchemaType());

        String format = getFormat();
        if (format != null && !format.isEmpty()) {
            node.put("format", format);
        }

        String encoding = getContentEncoding();
        if (encoding != null && !encoding.isEmpty()) {
            node.put("contentEncoding", encoding);
        }
    }
}
