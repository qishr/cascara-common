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
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.util.ReflectionUtils;
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
    public GenericAstSequence serialize(EnumSet jvmInstance, Type jvmType, Serializer.Context ctx) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }
        Type enumType = ReflectionUtils.getElementTypeOfCollectionType(jvmType);

        GenericAstSequence seq = new GenericAstSequence();
        for (Object obj : jvmInstance) {
            if (obj instanceof Enum enumVal) {
                AstNode element = ctx.serialize(enumVal, enumType);
                seq.add(element);
            }
        }
        return seq;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumSet deserialize(AstNode astNode, Type jvmType, Serializer.Context ctx) throws SerializerException {
        if (!(astNode instanceof SequenceAstNode seqNode)) {
            return null;
        }

        // Extract the underlying Enum class from jvmType (e.g. EnumSet<DayOfWeek> -> DayOfWeek.class)
        Class<? extends Enum> enumClass = extractEnumClass(seqNode, jvmType, ctx);
        if (enumClass == null) {
            return null;
        }

        EnumSet enumSet = EnumSet.noneOf(enumClass);
        for (AstNode child : seqNode.getChildren()) {
            Object rawObject = ctx.deserialize(child, enumClass);
            if (rawObject instanceof Enum enumValue) {
                enumSet.add(enumValue);
            }
        }
        return enumSet;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Enum> extractEnumClass(SequenceAstNode seqNode, Type jvmType, Serializer.Context ctx) {
        if (jvmType instanceof ParameterizedType pt) {
            Type[] actualArgs = pt.getActualTypeArguments();
            if (actualArgs.length > 0 && actualArgs[0] instanceof Class<?> cls && Enum.class.isAssignableFrom(cls)) {
                // TODO: A test that reaches here
                return (Class<? extends Enum>) cls;
            }
        } else if (jvmType instanceof Class<?>) {
            // If it's a raw EnumSet, check if we can inspect further or default
            // (Usually fields are parameterized like EnumSet<DayOfWeek>)
        }

        if (!seqNode.isEmpty()) {
            Type enumType = ReflectionUtils.getEnumType(jvmType);
            Object element = ctx.deserialize(seqNode.getFirst(), enumType);
            if (element != null && element.getClass().isEnum()) {
                return (Class<? extends Enum>) element.getClass();
            }
        }

        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}