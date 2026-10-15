package io.github.qishr.cascara.common.type.util;

import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;

@SuppressWarnings("rawtypes")
public class EnumSetSerializer extends AbstractTypeDescriptor<EnumSet> implements TypeSerializer<EnumSet> {

    public EnumSetSerializer() {
        super(EnumSet.class, PrimitiveType.ARRAY);
    }

    @Override
    public PlainSequenceNode serialize(EnumSet jvmInstance) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }
        PlainSequenceNode seq = new PlainSequenceNode();
        for (Object enumVal : jvmInstance) {
            seq.add(new PlainScalarNode(enumVal.toString()));
        }
        return seq;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumSet deserialize(AstNode astNode, Type targetType) throws SerializerException {
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