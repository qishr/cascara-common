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


package io.github.qishr.cascara.common.lang.ast;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public interface ScalarAstNode<T extends AstNode> extends AstNode {
    /// Returns the [QuoteStyle] used by a node: PLAIN, SINGLE, DOUBLE, LITERAL, or FOLDED.
    QuoteStyle getQuoteStyle();

    /// Sets the [QuoteStyle] used by a node
    ScalarAstNode<T> setQuoteStyle(QuoteStyle style);

    /// Returns the exact, unparsed text block directly from the file buffer.
    @Nullable
    String getLexeme();

    /// Returns the Java-native representation of the scalar (e.g., Integer, Boolean, String).
    @Nullable
    Object getPrimitive();

    PrimitiveType getPrimitiveType();

    @Nullable
    String getContent();

    /// Returns the string form or the primitive.
    /// If the primitive is `null`, an empty string is returned.
    String asString();

    int asInteger();
    int asInteger(int defaultValue);
    double asDouble();
    double asDouble(double defaultValue);

    /// Returns the boolean value of the scalar, if there is one.
    boolean asBoolean();

    /// Returns the boolean value of the scalar, if there is one, otherwise the specified default is returned.
    boolean asBoolean(boolean defaultValue);
}