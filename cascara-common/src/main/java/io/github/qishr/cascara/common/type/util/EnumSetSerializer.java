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

package io.github.qishr.cascara.common.type.util;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.generic.GenericAstScalar;
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.processor.Serializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;

@SuppressWarnings("rawtypes")
public class EnumSetSerializer extends AbstractTypeDescriptor<EnumSet> implements TypeSerializer<EnumSet> {

    public EnumSetSerializer() {
        super(EnumSet.class, PrimitiveType.ARRAY);
    }

    @Override
    public GenericAstSequence serialize(EnumSet jvmInstance, Serializer.Context ctx) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }
        GenericAstSequence seq = new GenericAstSequence();
        for (Object enumVal : jvmInstance) {
            seq.add(new GenericAstScalar(enumVal.toString()));
        }
        return seq;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumSet deserialize(AstNode astNode, Type targetType, Serializer.Context ctx) throws SerializerException {
        if (!(astNode instanceof SequenceAstNode seqNode)) {
            return null;
        }

        // Extract the underlying Enum class from targetType (e.g. EnumSet<DayOfWeek> -> DayOfWeek.class)
        Class<? extends Enum> enumClass = extractEnumClass(targetType);
        if (enumClass == null) {
            return null;
        }

        EnumSet enumSet = EnumSet.noneOf(enumClass);
        for (AstNode child : seqNode.getChildren()) {
            String val = child.asString();
            if (val != null && !val.isBlank()) {
                try {
                    Enum enumValue = Enum.valueOf(enumClass, val);
                    enumSet.add(enumValue);
                } catch (IllegalArgumentException e) {
                    // Handle or ignore unknown enum constants based on your error policy
                }
            }
        }
        return enumSet;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Enum> extractEnumClass(Type targetType) {
        if (targetType instanceof ParameterizedType pt) {
            Type[] actualArgs = pt.getActualTypeArguments();
            if (actualArgs.length > 0 && actualArgs[0] instanceof Class<?> cls && Enum.class.isAssignableFrom(cls)) {
                return (Class<? extends Enum>) cls;
            }
        } else if (targetType instanceof Class<?>) {
            // If it's a raw EnumSet, check if we can inspect further or default
            // (Usually fields are parameterized like EnumSet<DayOfWeek>)
        }
        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}