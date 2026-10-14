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

package io.github.qishr.cascara.common.lang.processor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.AnyGetter;
import io.github.qishr.cascara.common.annotation.AnySetter;
import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.UnexpectedNullReturnException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.AstNodeFactory;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.internal.DeserializationTargetResolver;
import io.github.qishr.cascara.common.lang.type.ScalarDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeReference;
import io.github.qishr.cascara.common.lang.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.util.LanguageOptions;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.service.ServiceProviderFactory;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public abstract class AbstractSerializer<
    T extends Serializer<N>,
    N extends AstNode,
    S extends ScalarAstNode<N>,
    L extends SequenceAstNode<N>,
    M extends MapAstNode<K,N,E>,
    E extends MapEntryAstNode<K,N>,
    K // Key type
> implements Serializer<N> {

    // Service
    private Properties properties;
    private final String contentType;

    // Options
    protected Reporter reporter = new NoOpReporter();
    protected int depthLimit = 500;
    private LanguageOptions<?> options;
    private boolean providerFactoryEnabled = true;
    private boolean heuristicMatchEnabled = true;

    // State
    protected int depth = 0;
    private Set<String> previousWarnings = new HashSet<>();

    private final DeserializationTargetResolver targetResolver = new DeserializationTargetResolver();
    private final Map<Class<?>,TypeDescriptor<?>> typeDescriptors = new HashMap<>();
    private final Set<String> classesWithNoTypeDescriptors = new HashSet<>();
    private final ServiceProviderFactory providerFactory = new ServiceProviderFactory();
    private final AstNodeFactory<N,S,L,M,E,K> astFactory;
    private final AstConverter<?> astConverter;

    protected AbstractSerializer(String contentType, AstNodeFactory<N,S,L,M,E,K> astFactory, LanguageOptions<?> options) {
        this(contentType, astFactory, null, options);
    }

    protected AbstractSerializer(String contentType, AstNodeFactory<N,S,L,M,E,K> astFactory, AstConverter<?> astConverter, LanguageOptions<?> options) {
        this.contentType = contentType;
        this.astFactory = astFactory;
        this.astConverter = astConverter;
        this.options = options;
    }

    @Override
    @NonNull
    public T setProviderFactoryEnabled(boolean b) {
        providerFactoryEnabled = b;
        return self();
    }

    @Override
    @NonNull
    public T setHeuristicMatchEnabled(boolean b) {
        heuristicMatchEnabled = b;
        return self();
    }

    protected abstract T self();

    protected abstract K serializeKey(Object key);

    //
    // Serialization Methods
    //

    /// Creates the appropriate AstNode (Scalar, Sequence, or Map) based on the Java value type.
    @SuppressWarnings("unchecked")
    @NonNull
    protected N serialize(Object jvmInstance) {

        if (jvmInstance == null) {
            return createScalar(null);
        }

        if (isOptional(jvmInstance)) {
            return serialize(unwrapOptional(jvmInstance));
        }

        depth++;
        if (depth > depthLimit) {
            throw new SerializerException(LangMessage.FAILED_SERIALIZE, "Depth limit exceeded");
        }

        try {
            // JVM primitive -> scalar
            if (isStandardScalar(jvmInstance)) {
                return createScalar(jvmInstance);
            }

            TypeDescriptor<?> typeDescriptor = getTypeDescriptor(jvmInstance.getClass());

            if (typeDescriptor != null) {

                // Custom serializer
                if (typeDescriptor instanceof TypeSerializer typeSerializer) {
                    AstNode rawNode = typeSerializer.serialize(jvmInstance);
                    if (astConverter == null) {
                        return castToNode(rawNode);
                    } else {
                        return castToNode(astConverter.fromAst(rawNode));
                    }
                }

                // ScalarDescriptor -> ScalarValue -> ScalarNode
                if (typeDescriptor instanceof ScalarDescriptor descriptor) {
                    Object scalarValue;
                    try {
                        scalarValue = descriptor.toPrimitive(jvmInstance);
                    } catch (Exception e) {
                        throw new SerializerException(
                            e,
                            LangMessage.FAILED_TO_MAP_AST,
                            jvmInstance.getClass().getName(),
                            e.getMessage()
                        );
                    }
                    return createScalar(scalarValue);
                }
            }

            if (jvmInstance instanceof Enum e) {
                String value = e.name();
                return createScalar(value);
            }

            if (jvmInstance.getClass().isArray()) {
                return (N) serializeArray(jvmInstance);
            }

            // Lists, Sets, etc
            if (jvmInstance instanceof Collection<?> list) {
                return (N) serializeCollection(list);
            }

            // Maps
            if (jvmInstance instanceof Map<?, ?> map) {
                return (N) serializeMap(map);
            }

            return (N) serializeObject(jvmInstance);
        } finally {
            depth--;
        }
    }

    @NonNull
    private M serializeObject(Object jvmInstance) {
        Class<?> jvmType = jvmInstance.getClass();
        M objectMap = astFactory.createMapNode();
        if (objectMap == null) {
            throw new SerializerException(LangMessage.AST_FACTORY_FAIL_MAP);
        }

        // getAllSerializableFields filters out fields with @DataIgnore
        for (Field field : getAllSerializableFields(jvmType)) {
            String fieldName = ReflectionUtils.getEffectiveName(field);
            int fieldModifiers = field.getModifiers();

            if (Modifier.isStatic(fieldModifiers) || Modifier.isTransient(fieldModifiers)) {
                continue;
            }

            // Try to make the field accessible. If this fails, continue to the next field.
            try {
                field.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                warnInaccessible(jvmType, field, e);
                continue;
            }

            if (field.isAnnotationPresent(AnySetter.class)) {
                Map<?, ?> map;
                try {
                    map = (Map<?, ?>) field.get(jvmInstance);
                } catch (IllegalAccessException e) {
                    throw new SerializerException(e, LangMessage.FIELD_NOT_ACCESSIBLE, field.getName());
                }
                if (map != null) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        K keyNode = serializeKey(entry.getKey());
                        N valueNode = serialize(entry.getValue());
                        objectMap.put(keyNode, valueNode);
                    }
                }
                continue;
            }

            Object value;
			try {
				value = field.get(jvmInstance);
			} catch (IllegalAccessException e) {
                throw new SerializerException(e, LangMessage.FIELD_NOT_ACCESSIBLE, field.getName());
			}
            if (value != null) {
                K keyNode = astFactory.createKey(fieldName);
                if (keyNode == null) {
                    throw new SerializerException(LangMessage.AST_FACTORY_FAIL_KEY);
                }
                N valueNode = serialize(value);
                objectMap.put(keyNode, valueNode);
            }
        }

        // 2. Process dynamic settings (@AnyGetter)
        for (Method method : ReflectionUtils.getAllMethods(jvmType)) {
            if (method.isAnnotationPresent(AnyGetter.class)) {
                // Try to make the method accessible. If this fails, continue to the next field.
                try {
                    method.setAccessible(true);
                } catch (InaccessibleObjectException e) {
                    warnInaccessible(jvmType, method, e);
                    continue;
                }

                // Invoke the method to get the Map
                Object result;

				try {
					result = method.invoke(jvmInstance);
                } catch (Exception e) {
                    throw error(e, method.getName());
                }

                if (result instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        K keyNode = serializeKey(entry.getKey());
                        N valueNode = serialize(entry.getValue());
                        objectMap.put(keyNode, valueNode);
                    }
                }
            }
        }
        return objectMap;
    }

    @NonNull
    private L serializeArray(Object jvmInstance) {
        L sequence = astFactory.createSequenceNode();
        if (sequence == null) {
            throw new SerializerException(LangMessage.AST_FACTORY_FAIL_SEQUENCE);
        }
        int length = Array.getLength(jvmInstance);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(jvmInstance, i);
            N element = item == null ? null : serialize(item);
            sequence.add(element);
        }
        return sequence;
    }

    /// Serializes a collection into a Sequence.
    @NonNull
    private L serializeCollection(Collection<?> list) {
        L sequence = astFactory.createSequenceNode();
        if (sequence == null) {
            throw new SerializerException(LangMessage.AST_FACTORY_FAIL_SEQUENCE);
        }
        for (Object item : list) {
            N element = item == null ? null : serialize(item);
            sequence.add(element);
        }
        return sequence;
    }

    @NonNull
    private M serializeMap(Map<?, ?> map) {
        M mapNode = astFactory.createMapNode();
        if (mapNode == null) {
            throw new SerializerException(LangMessage.AST_FACTORY_FAIL_MAP);
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            K keyNode = astFactory.createKey(entry.getKey());
            if (keyNode == null) {
                throw new SerializerException(LangMessage.AST_FACTORY_FAIL_KEY);
            }
            N valueNode = (entry.getValue() == null)
                ? createScalar(null)
                : serialize(entry.getValue());

            mapNode.put(keyNode, valueNode);
        }
        return mapNode;
    }

    //
    // Deserialization Methods
    //

    /// Converts an AST structure back into a Java object of the generic type referenced by typeRef.
    @SuppressWarnings("unchecked")
    @Nullable
    protected <C> C deserialize(AstNode node, TypeReference<C> typeRef) throws SerializerException {
        return (C) deserialize(node, typeRef.getType());
    }

    /// Converts an AST structure back into a Java object of the specified type.
    /// @param node The AST node to convert.
    /// @param targetType The class type to convert to.
    @SuppressWarnings("unchecked")
    @Nullable
    protected <C> C deserialize(AstNode node, Type targetType) throws SerializerException {
        // If the node is null, or it's a scalar representing a null value, we return null immediately.
        if (node == null || (node instanceof ScalarAstNode s && s.getPrimitive() == null)) {
            return null;
        }

        Class<?> rawClass = ReflectionUtils.getRawClass(targetType);
        TypeDescriptor<?> typeDescriptor = getTypeDescriptor(targetType);

        // 1. Custom type serializers
        if (typeDescriptor instanceof TypeSerializer<?> typeSerializer) {
            return (C) typeSerializer.deserialize(node);
        }

        // 2. Scalar type descriptor with potentially non-scalar targetType
        if (typeDescriptor instanceof ScalarDescriptor descriptor) {
            if (node instanceof ScalarAstNode scalar) {
                return (C) deserializeScalarWithDescriptor(scalar, descriptor);
            }
        }

        // 3. Optional targetType
        if (isOptionalType(rawClass)) {
            return (C) wrapOptional(node, targetType);
        }

        // 4. Array targetType
        if (rawClass.isArray()) {
            return (C) deserializeArray(node, targetType);
        }

        // 5. Map targetType
        if (ReflectionUtils.canAssign(targetType, Map.class)) {
            return (C) deserializeMap(node, targetType);
        }

        // 6. Collection targetType
        if (ReflectionUtils.canAssign(targetType, Collection.class)) {
            return (C) deserializeCollection(node, targetType);
        }

        // 7. Scalar node with scalar targetType
        if (targetType instanceof Class<?> cls) {
            if (ReflectionUtils.isScalarType(cls)) {
                if (node instanceof ScalarAstNode scalar) {
                    return (C) deserializeScalar(scalar, targetType);
                }

                // We reach here if targetType is scalar but the node is a collection.
                // This is not possible to resolve.
                throw new SerializerException(
                    node,
                    LangMessage.EXPECTED_SCALAR,
                    cls.getSimpleName()
                );
            }
        }

        // 8. Scalar node with non-scalar targetType and no type descriptor
        if (node instanceof ScalarAstNode scalar) {
            // Without a TypeDescriptor to do the conversion, the only
            // non-scalar targetType we can safely put a scalar value in is Object.
            if (rawClass == Object.class) {
                return (C) deserializeScalar(scalar, targetType);
            }
            throw new SerializerException(
                node,
                LangMessage.FAILED_DESERIALIZE_SCALAR_TO_NON_SCALAR,
                ReflectionUtils.getTypeName(targetType)
            );
        }

        // 10. node is a map or collection, but the targetType didn't match at stage #4 or #5
        //     targetType is just Object
        if (targetType == Object.class) {
            if (node instanceof MapAstNode mapNode) {
                return (C) convertAstMapToStandardMap(mapNode);
            }
            if (node instanceof SequenceAstNode seqNode) {
                return (C) convertAstSequenceToStandardCollection(seqNode);
            }
            if (node instanceof ScalarAstNode scalar) {
                return (C) scalar.getPrimitive();
            }
            // It's only possible to reach here if the node
            // was not scalar, map, or sequence. That means the node
            // is not YAML or JSON and we don't know how to handle it.
            throw new SerializerException(node, LangMessage.FAILED_TO_MAP_AST, node, targetType);
        }

        // Likely cause of arriving here is that the target type either:
        //   - Is a POJO
        //   - Is in a package that's not opened to cascara.common

        if (!(node instanceof MapAstNode mapNode)) {
            throw new SerializerException(node, LangMessage.EXPECTED_MAP_STRUCTURE, targetType);
        }

        C jvmInstance = newTypeInstance(mapNode, targetType);
        return deserializeObject(mapNode, jvmInstance);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private <C> C deserializeObject(AstNode node, C jvmInstance) {
        Class<?> jvmType = jvmInstance.getClass();
        Set<String> claimedKeys = new HashSet<>();

        // 1. Check against the generic MapAstNode interface
        if (!(node instanceof MapAstNode mapNode)) {
            throw new SerializerException(node, LangMessage.EXPECTED_MAP_STRUCTURE, jvmInstance.getClass()); //jvmType.getName());
        }

        // 2. Process Declared Fields
        for (Field field : getAllSerializableFields(jvmType)) {
            // Skip static or transient fields
            int fieldModifiers = field.getModifiers();
            if (Modifier.isStatic(fieldModifiers) || Modifier.isTransient(fieldModifiers)) {
                continue;
            }

            // Try to make the field accessible. If this fails, continue to the next field.
            try {
                field.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                warnInaccessible(jvmType, field, e);
                continue;
            }

            // Determine the key for this field
            String keyName = ReflectionUtils.getEffectiveName(field);

            claimedKeys.add(keyName);

            // Use the new generic 'get' method
            AstNode valueNode = mapNode.get(keyName);

            if (valueNode != null) {
                // We pass field.getType() so it knows this is a List, a String, etc.
                Object convertedValue = deserialize(valueNode, field.getGenericType());
                if (convertedValue != null) {
                    try {
                        field.set(jvmInstance, convertedValue);
                    } catch (Exception e) {
                        throw error(e, field.getName());
                    }
                }
            }
        }

        // 3. Handle dynamic properties via @AnySetter
        processAnySetter(jvmInstance, (M)node, claimedKeys, jvmType);

        return jvmInstance;
    }

    @NonNull
    private Object deserializeArray(AstNode node, Type targetType) {
        if (node == null) {
            Class<?> componentType = ReflectionUtils.getArrayComponentType(targetType);
            return Array.newInstance(componentType, 0);
        }

        Type itemType = ReflectionUtils.getArrayComponentType(targetType);

        if (itemType == null) {
            // This should not be possible as this method is only calle when targetType
            // is an array, therefore ReflectionUtils should not return null.
            throw new UnexpectedNullReturnException("ReflectionUtils", "getArrayComponentType");
        }

        // Fallback for single values in YAML where an array was expected
        if (node instanceof ScalarAstNode scalar) {
            Object val = deserializeScalar(scalar, itemType);

            if (val == null) {
                return Array.newInstance((Class<?>) itemType, 0);
            }

            Object array = Array.newInstance((Class<?>) itemType, 1);
            Array.set(array, 0, val);
            return array;
        }

        if (!(node instanceof SequenceAstNode sequence)) {
            throw new SerializerException(node, LangMessage.EXPECTED_SEQUENCE, ReflectionUtils.getTypeName(targetType));
        }

        Object array = Array.newInstance((Class<?>) itemType, sequence.getChildren().size());

        int index = 0;
        for (AstNode item : sequence.getChildren()) {
            Object val = deserialize(item, itemType);
            Array.set(array, index++, val);
        }

        return array;
    }

    @NonNull
    private Collection<?> deserializeCollection(AstNode node, Type targetType) {
        Class<?> rawClass = ReflectionUtils.getRawClass(targetType);
        Collection<Object> result = newCollectionInstance(rawClass);

        if (node == null) {
            return result;
        }

        Type itemIype = ReflectionUtils.getTypeArgumentTypeOfCollectionType(targetType);

        // Fallback for single values in YAML where a list was expected
        if (node instanceof ScalarAstNode scalar) {
            Object val = deserializeScalar(scalar, itemIype);
            // If the value is null (like an empty key), return an empty mutable list
            if (val == null) return result;

            result.add(val);
            return result;
        }

        if (!(node instanceof SequenceAstNode sequence)) {
            throw new SerializerException(node, LangMessage.EXPECTED_SEQUENCE, ReflectionUtils.getTypeName(targetType));
        }

        // List<Object> result = new ArrayList<>();
        for (AstNode item : sequence.getChildren()) {
            Object val = deserialize(item, itemIype);
            // YAML sequences can have null entries (- ), we should decide if we allow them.
            // Usually, for a list of strings/objects, we skip nulls or add them.
            result.add(val);
        }
        return result;
    }

    @NonNull
    private Map<?, ?> deserializeMap(AstNode node, Type targetType) {
        if (!(node instanceof MapAstNode)) return new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
		M mapNode = (M)node;
        Class<?> rawClass = ReflectionUtils.getRawClass(targetType);
        Map<Object, Object> result = newMapInstance(rawClass);

        Type keyType = ReflectionUtils.getKeyTypeArgumentTypeOfMapType(targetType);
        Type valType = ReflectionUtils.getValueTypeArgumentTypeOfMapType(targetType);

        for (E entry : mapNode.getEntries()) {
            Object key;
            if (entry.getKey() instanceof ScalarAstNode scalarKey) {
                key = deserializeScalar(scalarKey, keyType);
            } else if (entry.getKey() instanceof String string) {
                key = string;
            } else {
                throw new SerializerException(node, LangMessage.NON_SCALAR_KEY, entry.getKey());
            }
            Object val = deserialize(entry.getValue(), valType);
            if (key != null) result.put(key, val);
        }

        return result;
    }

    /// Converts a primitive value (already inferred by the AST) or a raw string into the target Java type.
    @SuppressWarnings({"rawtypes", "unchecked" })
    @Nullable
    private Object deserializeScalar(ScalarAstNode scalar, Type targetType) throws SerializerException {
        Object jvmInstance = scalar.getPrimitive();

        if (jvmInstance == null) return null;

        // 1. Exact Match / Wrapper Match
        if (ReflectionUtils.isInstance(jvmInstance, targetType) ||
           (isPrimitive(targetType) && getWrapperClass(targetType).isInstance(jvmInstance))) {
            return jvmInstance;
        }

        // 2. Numeric Narrowing (If AST already inferred a Number but target is different)
        if (jvmInstance instanceof Number num) {
            if (targetType == int.class || targetType == Integer.class) return num.intValue();
            if (targetType == long.class || targetType == Long.class) return num.longValue();
            if (targetType == double.class || targetType == Double.class) return num.doubleValue();
            if (targetType == float.class || targetType == Float.class) return num.floatValue();
            if (targetType == byte.class || targetType == Byte.class) return num.byteValue();
            if (targetType == short.class || targetType == Short.class) return num.shortValue();
        }

        // 3. ScalarDescriptor
        String text = jvmInstance.toString().trim();
        TypeDescriptor typeDescriptor = getTypeDescriptor(targetType);
        if (typeDescriptor instanceof ScalarDescriptor descriptor) {
            try {
                return descriptor.toJvmType(text);
            } catch (Exception e) {
                throw new SerializerException(scalar, e, LangMessage.FAILED_DESERIALIZE_SCALAR, jvmInstance.getClass(), e.getMessage());
            }
        }

        // 4. String-Based Parsing (Fallback for quoted values or string-only types)
        if (targetType == String.class) return text;

        if (isEnum(targetType)) {
            return Enum.valueOf((Class<Enum>) targetType, text);
        }

        throw new SerializerException(scalar, LangMessage.UNSUPPORTED_TYPE, ReflectionUtils.getTypeName(targetType));
    }

    @NonNull
    private Object deserializeScalarWithDescriptor(ScalarAstNode<?> scalar, ScalarDescriptor<?> descriptor) {
        Object val = scalar.getPrimitive();
        String stringValue = val != null ? val.toString() : "";
        try {
            // toJvmType never returns null but may throw a RuntimeException
            return descriptor.toJvmType(stringValue);
        } catch (Exception e) {
            throw new SerializerException(scalar, e, LangMessage.FAILED_TO_MAP_TYPE, descriptor.getJvmType().getName(), e.getMessage());
        }
    }

    //
    // Deserialization Helpers
    //

    @NonNull
    private <C> C newInstance(Class<C> rawClass) {
        try {
            return ReflectionUtils.createInstance(rawClass);
        } catch (Exception e) {
            throw error(e, rawClass.getTypeName());
        }
    }

    @NonNull
    private <C> C newTypeInstance(MapAstNode<?,?,?> node, Type jvmType) throws SerializerException {
        Class<? extends C> targetClass = targetResolver.resolveTarget(node, jvmType, heuristicMatchEnabled);
        return newInstance(targetClass);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private Collection<Object> newCollectionInstance(Class<?> rawClass) {
        if (rawClass.isInterface()) {
            if (List.class.isAssignableFrom(rawClass)) return new ArrayList<>();
            if (Set.class.isAssignableFrom(rawClass))  return new HashSet<>();
            return new ArrayList<>();
        } else {
            return (Collection<Object>) newInstance(rawClass);
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private Map<Object,Object> newMapInstance(Class<?> rawClass) {
        if (rawClass.isInterface()) {
            if (LinkedHashMap.class.isAssignableFrom(rawClass)) return new LinkedHashMap<>();
            return new LinkedHashMap<>();
        } else {
            return (Map<Object,Object>) newInstance(rawClass);
        }
    }

    /// Converting an MapAstNode to a Map when the target type is just Object.
    @NonNull
    private Map<String, Object> convertAstMapToStandardMap(MapAstNode<?,?,?> mapNode) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (MapEntryAstNode<?,?> entry : mapNode.getEntries()) {
            String key = entry.getKeyString();
            // Recursively convert the value
            Object value = deserialize(entry.getValue(), Object.class);
            result.put(key, value);
        }
        return result;
    }

    /// Converting a SequenceAstNode to a Collection when the target type is just Object.
    @NonNull
    private Collection<Object> convertAstSequenceToStandardCollection(SequenceAstNode<?> seqNode) {
        Collection<Object> result = new ArrayList<>();
        for (AstNode child : seqNode.getChildren()) {
            // Recursively convert each item in the list
            result.add(deserialize(child, Object.class));
        }
        return result;
    }

    @NonNull
    private Class<?> getWrapperClass(Type jvmType) {
        if (jvmType instanceof Class<?> jvmClass) {
            if (jvmClass == int.class) return Integer.class;
            if (jvmClass == boolean.class) return Boolean.class;
            if (jvmClass == long.class) return Long.class;
            if (jvmClass == double.class) return Double.class;
            return jvmClass;
        }
        throw new SerializerException(LangMessage.UNSUPPORTED_TYPE, ReflectionUtils.getTypeName(jvmType));
    }

    private void processAnySetter(Object instance, M rootMap, Set<String> claimedKeys, Class<?> jvmType) {
        for (Method method : ReflectionUtils.getAllMethods(jvmType)) {
            if (method.isAnnotationPresent(AnySetter.class)) {
                // Try to make the method accessible. If this fails, continue to the next field.
                try {
                    method.setAccessible(true);
                } catch (InaccessibleObjectException e) {
                    warnInaccessible(jvmType, method, e);
                    continue;
                }

                for (E entry : rootMap.getEntries()) {
                    String key = entry.getKeyString();

                    if (!claimedKeys.contains(key) && !isSchemaOrId(key)) {
                        Object value;
                        AstNode valueNode = entry.getValue();
                        if (valueNode instanceof ScalarAstNode scalar) {
                            value = scalar.getPrimitive();
                        } else if (valueNode instanceof MapAstNode map) {
                            value = convertAstMapToStandardMap(map);
                        } else if (valueNode instanceof SequenceAstNode seq) {
                            value = convertAstSequenceToStandardCollection(seq);
                        } else {
                            // If it's a complex object (Map/List), for now we pass the AST node
                            // or we'd need a recursive "astToMap" helper.
                            value = valueNode;
                        }
                        try {
                            method.invoke(instance, key, value);
                        } catch (IllegalAccessException e) {
                            throw new SerializerException(e, LangMessage.FIELD_NOT_ACCESSIBLE, method.getName());
                        } catch (InvocationTargetException e) {
                            throw new SerializerException(e, LangMessage.INVOCATION_TARGET_EXCEPTION, method.getName());
                        }
                    }
                }
            }
        }
    }

    private boolean isPrimitive(Type jvmType) {
        if (jvmType instanceof Class<?> jvmClass) {
            return jvmClass.isPrimitive();
        }
        return false;
    }

    private boolean isEnum(Type jvmType) {
        if (jvmType instanceof Class<?> jvmClass) {
            return jvmClass.isEnum();
        }
        return false;
    }

    private boolean isSchemaOrId(String key) {
        return "$schema".equals(key) || "$id".equals(key);
    }

    private boolean isOptionalType(Class<?> jvmType) {
        return Optional.class == jvmType ||
               OptionalInt.class == jvmType ||
               OptionalLong.class == jvmType ||
               OptionalDouble.class == jvmType;
    }

    private Type getWrappedType(Type jvmType) {
        Class<?> rawClass = ReflectionUtils.getRawClass(jvmType);
        if (Optional.class.equals(rawClass)) {
            Type paramType = ReflectionUtils.getTypeArgumentTypeOfGenericType(jvmType, 0);
            return paramType;
        }
        if (OptionalInt.class.equals(rawClass)) {
            return Integer.class;
        }
        if (OptionalLong.class.equals(rawClass)) {
            return Long.class;
        }
        if (OptionalDouble.class.equals(rawClass)) {
            return Double.class;
        }
        // This point cannot be reached due to the isOptionalType check
        throw new IllegalArgumentException("Unsupported optional type: " + rawClass.getName());
    }

    private Object wrapOptional(AstNode node, Type jvmType) {
        Type wrappedType = getWrappedType(jvmType);
        Object wrappedValue = deserialize(node, wrappedType);
        Class<?> rawClass = ReflectionUtils.getRawClass(jvmType);
        if (Optional.class.equals(rawClass)) {
            return Optional.of(wrappedValue);
        }
        if (OptionalInt.class.equals(rawClass) && wrappedValue instanceof Integer value) {
            return OptionalInt.of(value);
        }
        if (OptionalLong.class.equals(rawClass) && wrappedValue instanceof Long value) {
            return OptionalLong.of(value);
        }
        if (OptionalDouble.class.equals(rawClass) && wrappedValue instanceof Double value) {
            return OptionalDouble.of(value);
        }
        // This point cannot be reached due to the isOptionalType check
        throw new IllegalArgumentException("Unsupported optional type: " + rawClass.getName());
    }

    //
    // Serialization Helpers
    //

    private boolean isOptional(Object jvmInstance) {
        return jvmInstance instanceof Optional<?> ||
               jvmInstance instanceof OptionalInt ||
               jvmInstance instanceof OptionalLong ||
               jvmInstance instanceof OptionalDouble;
    }

    @Nullable
    private Object unwrapOptional(Object jvmInstance) {
        if (jvmInstance instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        if (jvmInstance instanceof OptionalInt optional) {
            return optional.isPresent()
                ? optional.getAsInt()
                : null;
        }
        if (jvmInstance instanceof OptionalLong optional) {
            return optional.isPresent()
                ? optional.getAsLong()
                : null;
        }
        if (jvmInstance instanceof OptionalDouble optional) {
            return optional.isPresent()
                ? optional.getAsDouble()
                : null;
        }
        // This point cannot be reached due to the isOptionalType check
        throw new IllegalArgumentException("Unsupported optional type: " + jvmInstance.getClass().getName());
    }

    /// Retrieves all declared fields for a class and all its superclasses (excluding Object).
    @NonNull
    private List<Field> getAllSerializableFields(Class<?> jvmType) {
        return ReflectionUtils.getAllFields(jvmType, f -> f.isAnnotationPresent(DataIgnore.class));
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private <V extends AstNode> N castToNode(V node) {
        return (N) node;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private N createScalar(Object jvmInstance) {
        N scalarNode = (N) astFactory.createScalarNode(
            jvmInstance,
            QuoteStyle.UNDETERMINED, // Only the ScalarAstNode implementation knows what it should be.
            options
        );
        if (scalarNode == null) {
            throw new SerializerException(LangMessage.AST_FACTORY_FAIL_SCALAR);
        }
        return scalarNode;
    }

    private boolean isStandardScalar(Object jvmInstance) {
        return (jvmInstance instanceof String ||
            jvmInstance instanceof Number ||
            jvmInstance instanceof Byte ||
            jvmInstance instanceof Character ||
            jvmInstance instanceof Boolean);
    }

    //
    //
    //

    @Nullable
    private TypeDescriptor<?> getTypeDescriptor(Type jvmType) {
        if (jvmType instanceof Class<?> c) {
            return getTypeDescriptor(c);
        }
        return null;
    }

    @Nullable
    private TypeDescriptor<?> getTypeDescriptor(Class<?> jvmType) {
        // The only two places this is call from ensure jvmType is not null

        if (classesWithNoTypeDescriptors.contains(jvmType.getName())) {
            return null;
        }

        // 1. First check if one has been registered locally
        if (typeDescriptors.containsKey(jvmType)) {
            return typeDescriptors.get(jvmType);
        }

        if (!providerFactoryEnabled) {
            return null;
        }

        // 2. Use service provider layer to get one
        TypeDescriptor<?> descriptor = providerFactory.createTypeDescriptor(jvmType);
        if (descriptor == null) {
            classesWithNoTypeDescriptors.add(jvmType.getName());
            reporter.debug("No type desciptor for " + jvmType.getName());
            return null;
        }
        typeDescriptors.put(jvmType, descriptor);

        return descriptor;
    }

    //
    // Setup
    //

    protected void setupSerializer() {
        depth = 0;
        previousWarnings = new HashSet<>();
    }

    @NonNull
    public T setOptions(LanguageOptions<?> options) {
        this.options = options;
        return self();
    }

    @Override
    @NonNull
    public Properties getServiceProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.set("contentType", contentType);
        }
        return properties;
    }

    @Override
    @NonNull
    public T registerTypeDescriptor(TypeDescriptor<?> typeDescriptor) {
        typeDescriptors.put(typeDescriptor.getJvmType(), typeDescriptor);
        return self();
    }

    //
    // Diagnostics
    //

    protected SerializerException error(Throwable t, Object... details) {
        DiagnosticMessage d = DiagnosticMessage.forException(t);
        if (d == null) {
            return new SerializerException(t, GenericMessage.ERROR, t.getMessage());
        } else {
            return new SerializerException(d, details);
        }
    }

    protected void warn(DiagnosticMessage code, Object... details) {
        reporter.warn(code, details);
    }

    protected void warnInaccessible(Class<?> jvmType, Method method, Throwable t) {
        warnInaccessible(jvmType.getName() + "." + method.getName(), t);
    }

    protected void warnInaccessible(Class<?> jvmType, Field field, Throwable t) {
        warnInaccessible(jvmType.getName() + "." + field.getName(), t);
    }

    private void warnInaccessible(String fullName, Throwable t) {
        if (previousWarnings.contains(fullName)) return;
        previousWarnings.add(fullName);
        warn(LangMessage.FIELD_NOT_ACCESSIBLE_REASON, fullName, t.getMessage());
    }

    protected void debug(String message, Object... details) {
        if (!reporter.reportsDebug()) return;
        report(Level.DEBUG, message, details);
    }

    protected void trace(String message, Object... details) {
        if (!reporter.reportsTrace()) return;
        report(Level.TRACE, message, details);
    }

    protected void report(Level level, String message, Object... details) {
        if (level == Level.TRACE) {
            reporter.trace(message, details);
        } else {
            reporter.debug(message, details);
        }
    }
}
