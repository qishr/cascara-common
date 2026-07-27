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


package io.github.qishr.cascara.common.lang.reference;

import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.ast.*;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ReferenceScalarNode extends ReferenceNode implements ScalarAstNode<ReferenceNode> {

    private Object value;
    private QuoteStyle quoteStyle = QuoteStyle.PLAIN;

    public ReferenceScalarNode(Object value) {
        this.value = value;
    }

    public ReferenceScalarNode(Object value, QuoteStyle quoteStyle) {
        this.value = value;
        this.quoteStyle = quoteStyle;
    }

    @Override
    public ReferenceScalarNode setQuoteStyle(QuoteStyle style) {
        this.quoteStyle = style;
        return this;
    }

    @Override
    @Nullable
    public String getLexeme() {
        return value == null ? null : value.toString();
    }

    @Override
    public String asString() {
        return value == null ? null : value.toString();
    }

    @Override
    public int asInteger() {
        return asInteger(0);
    }

    @Override
    public int asInteger(int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(asString()); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public double asDouble() {
        return asDouble(0);
    }

    @Override
    public double asDouble(double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(asString()); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public boolean asBoolean() {
        return asBoolean(false);
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(asString());
    }

    @Nullable
    @Override
    public Object getPrimitive() {
        return value;
    }

    // @Override
    // public ReferenceScalarNode setPrimitive(Object value) {
    //     this.value = value;
    //     return this;
    // }

    @Override
    public List<? extends AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public List<CommentAstNode> getComments() {
        return Collections.emptyList();
    }

    @Override
    public QuoteStyle getQuoteStyle() {
        return quoteStyle;
    }

    @Override
    public String getContent() {
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferenceScalarNode that)) return false;
        return Objects.equals(value, that.value)
            && quoteStyle == that.quoteStyle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, quoteStyle);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.of(value);
    }
}
