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

import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.util.ReflectionUtils;
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
    public GenericAstMap serialize(EnumMap jvmInstance, Type jvmType, Serializer.Context ctx) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }

        Type keyType = ReflectionUtils.getKeyTypeOfMapType(jvmType);

        GenericAstMap node = new GenericAstMap();
        for (Object obj : jvmInstance.entrySet()) {
            if (obj instanceof Map.Entry<?, ?> entry) {
                if (entry.getKey() != null) {
                    AstNode keyNode = ctx.serialize(entry.getKey(), keyType);
                    AstNode valueNode = ctx.serialize(entry.getValue(), null);
                    node.put(keyNode, valueNode);
                }
            }
        }
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumMap deserialize(AstNode astNode, Type jvmType, Serializer.Context ctx) throws SerializerException {
        if (!(astNode instanceof MapAstNode<?, ?, ?> mapNode)) {
            return null;
        }

        Class<? extends Enum> keyEnumClass = extractKeyEnumClass(mapNode, jvmType, ctx);
        if (keyEnumClass == null) {
            return null;
        }

        EnumMap enumMap = new EnumMap(keyEnumClass);
        for (Object keyObj : mapNode.keySet()) {
            Enum enumKey = deserializeKey(keyObj, keyEnumClass, ctx);
            AstNode valueNode = mapNode.get(keyObj);
            Object deserializedVal = ctx.deserialize(valueNode, Object.class);
            enumMap.put(enumKey, deserializedVal);
        }
        return enumMap;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Enum> extractKeyEnumClass(MapAstNode<?, ?, ?> mapNode, Type jvmType, Serializer.Context ctx) {
        if (jvmType instanceof ParameterizedType pt) {
            Type[] actualArgs = pt.getActualTypeArguments();
            if (actualArgs.length > 0 && actualArgs[0] instanceof Class<?> cls && Enum.class.isAssignableFrom(cls)) {
                // TODO: A test that reaches here
                return (Class<? extends Enum>) cls;
            }
        }

        if (!mapNode.isEmpty()) {

            Type enumType;
            if (jvmType instanceof ParameterizedType paramaterizedType) {
                // "unbox" the enum type
                Type[] paramTypes = paramaterizedType.getActualTypeArguments();
                if (paramTypes != null && paramTypes.length == 2) {
                    enumType = paramTypes[0];
                } else {
                    // TODO: This should not be possible
                    throw new IllegalStateException("wrong parameter count");
                }
            } else {
                enumType = jvmType;
            }

            Object key = mapNode.getEntry(0).getKey();
            Enum keyEnum = deserializeKey(key, enumType, ctx);
            if (keyEnum != null) {
                return (Class<? extends Enum>) keyEnum.getClass();
            }
        }
        throw new SerializerException(LangMessage.AMBIGUOUS_TARGET_TYPE, jvmType);
    }

    private Enum deserializeKey(Object key, Type enumType, Serializer.Context ctx) {
        Object element;
        if (key instanceof AstNode keyNode) {
            element = ctx.deserialize(keyNode, enumType);
        } else if (key != null) {
            element = ctx.deserializeEnum(key.toString(), enumType);
        } else {
            return null;
        }
        if (element != null && element.getClass().isEnum()) {
            return (Enum) element;
        }
        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}