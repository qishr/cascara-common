package io.github.qishr.cascara.common.type.util;

import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;

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
    public PlainMapNode serialize(EnumMap jvmInstance) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }
        PlainMapNode node = new PlainMapNode();
        for (Object obj : jvmInstance.entrySet()) {
            if (obj instanceof Map.Entry<?, ?> entry) {
                if (entry.getKey() != null) {
                    // TODO: This is probably wrong
                    String value = String.valueOf(entry.getValue());
                    node.put(entry.getKey().toString(), value);
                }
            }
        }
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumMap deserialize(AstNode astNode, Type targetType) throws SerializerException {
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

                // Value deserialization can delegate to the standard value deserializer processor
                // or handle basic scalar conversions.

                //--------------------------------------------------
                // TODO: TypeSerializers need access to the AbstractSerializer methods.
                // See comment in SerializerTypesCascaraTests:
                //
                //     This gets close to the idea that AbstractSerializer should just be a
                //     TypeSerializer<Object>, but thats a philosophical discussion for another day.
                //     The mere existence of a TypeSerializer<Object> is likely to break everything downstream.
                //
                //--------------------------------------------------

                // TODO: This needs to be implemented correctly
                // Object deserializedVal = valueNode != null ? valueNode.getValue() : null;
                Object deserializedVal = valueNode != null ? valueNode : null;
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