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

import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.processor.Serializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class EnumMapSerializer extends AbstractTypeDescriptor<EnumMap> implements TypeSerializer<EnumMap> {

    public EnumMapSerializer() {
        super(EnumMap.class, PrimitiveType.OBJECT);
    }

    @Override
    public GenericAstMap serialize(EnumMap jvmInstance, Serializer.Context ctx) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }
        GenericAstMap node = new GenericAstMap();
        for (Object obj : jvmInstance.entrySet()) {
            if (obj instanceof Map.Entry<?, ?> entry) {
                if (entry.getKey() != null) {

                    // TODO: This is probably wrong:
                    // String value = String.valueOf(entry.getValue());
                    // node.put(entry.getKey().toString(), value);

                    // Should be more like:
                    AstNode keyNode = ctx.serialize(entry.getKey());
                    AstNode valueNode = ctx.serialize(entry.getValue());
                    node.put(keyNode, valueNode);
                }
            }
        }
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumMap deserialize(AstNode astNode, Type targetType, Serializer.Context ctx) throws SerializerException {
        if (!(astNode instanceof MapAstNode<?, ?, ?> mapNode)) {
            return null;
        }

        Class<? extends Enum> keyEnumClass = extractKeyEnumClass(targetType);
        if (keyEnumClass == null) {
            return null;
        }

        EnumMap enumMap = new EnumMap(keyEnumClass);
        for (Object keyObj : mapNode.keyStringSet()) {
            String keyStr = (String) keyObj;
            try {
                Enum enumKey = Enum.valueOf(keyEnumClass, keyStr);
                AstNode valueNode = mapNode.get(keyStr);
                Object deserializedVal = ctx.deserialize(valueNode, Object.class);
                enumMap.put(enumKey, deserializedVal);
            } catch (IllegalArgumentException e) {
                // Handle or ignore unknown enum keys
            }
        }
        return enumMap;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Enum> extractKeyEnumClass(Type targetType) {
        if (targetType instanceof ParameterizedType pt) {
            Type[] actualArgs = pt.getActualTypeArguments();
            if (actualArgs.length > 0 && actualArgs[0] instanceof Class<?> cls && Enum.class.isAssignableFrom(cls)) {
                return (Class<? extends Enum>) cls;
            }
        }
        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}