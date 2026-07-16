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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.LangDiagnosticCode;
import io.github.qishr.cascara.common.lang.annotation.AnyGetter;
import io.github.qishr.cascara.common.lang.annotation.AnySetter;
import io.github.qishr.cascara.common.lang.annotation.DataField;
import io.github.qishr.cascara.common.lang.annotation.DataIgnore;
import io.github.qishr.cascara.common.lang.annotation.Serializable;
import io.github.qishr.cascara.common.lang.ast.AstNodeFactory;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.exception.SerializerException;
import io.github.qishr.cascara.common.lang.type.ScalarDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeReference;
import io.github.qishr.cascara.common.lang.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.util.LanguageOptions;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;
import io.github.qishr.cascara.common.service.ServiceProviderFactory;
import io.github.qishr.cascara.common.util.Properties;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public abstract class AbstractSerializer<
    T extends Serializer<N>,
    N extends AstNode,
    S extends ScalarAstNode<N>,
    L extends SequenceAstNode<N>,
    M extends MapAstNode<K,N,E>,
    E extends MapEntryAstNode<K,N>,
    K
> implements Serializer<N> {

    protected Reporter reporter = new NoOpReporter();
    private Properties properties;
    private final String contentType;
    private final AstNodeFactory<N,S,L,M,E,K> astFactory;

    private final Map<Class<?>,TypeDescriptor<?>> typeDescriptors = new HashMap<>();
    private final ServiceProviderFactory providerFactory = new ServiceProviderFactory();

    private LanguageOptions<?> options;

    protected AbstractSerializer(String contentType, AstNodeFactory<N,S,L,M,E,K> astFactory, LanguageOptions<?> options) {
        this.contentType = contentType;
        this.astFactory = astFactory;
        this.options = options;
    }

    protected abstract T self();

    protected abstract K serializeKey(Object key);

    public T setOptions(LanguageOptions<?> options) {
        this.options = options;
        return self();
    }

    @Override
    public Properties getServiceProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.set("contentType", contentType);
        }
        return properties;
    }

    @Override
    public T registerTypeDescriptor(TypeDescriptor<?> typeDescriptor) {
        typeDescriptors.put(typeDescriptor.getJvmType(), typeDescriptor);
        return self();
    }

    //
    //
    //

    /// Creates the appropriate AstNode (Scalar, Sequence, or Map) based on the Java value type.
    @SuppressWarnings("unchecked")
    protected N serialize(Object jvmInstance) {

        // JVM primitive → scalar
        if (isScalar(jvmInstance)) {
            return (N) astFactory.createScalarNode(
                jvmInstance,
                QuoteStyle.UNDETERMINED, // Only the ScalarAstNode implementation knows what it should be.
                options
            );
        }

        TypeDescriptor<?> typeDescriptor = getTypeDescriptor(jvmInstance.getClass());

        if (typeDescriptor != null) {

            // Custom serializer
            if (typeDescriptor instanceof TypeSerializer typeSerializer) {
                return castToNode(typeSerializer.serialize(jvmInstance));
            }

            // ScalarDescriptor → ScalarValue → ScalarNode
            if (typeDescriptor instanceof ScalarDescriptor descriptor) {
                Object scalarValue;
                try {
                    scalarValue = descriptor.toPrimitive(jvmInstance);
                } catch (Exception e) {
                    throw new SerializerException(
                        e,
                        LangDiagnosticCode.FAILED_TO_MAP_AST,
                        jvmInstance.getClass().getSimpleName(),
                        e.getMessage()
                    );
                }

                return (N) astFactory.createScalarNode(
                    scalarValue,
                    QuoteStyle.UNDETERMINED,
                    options
                );
            }
        }

        // Lists
        if (jvmInstance instanceof List<?> list) {
            return (N) serializeList(list);
        }

        // Maps
        if (jvmInstance instanceof Map<?, ?> map) {
            return (N) serializeMap(map);
        }

        // TODO: ALL OBJECTS should be handled this way. @Serializable should not be neccesary
        // Serializable objects
        if (jvmInstance.getClass().isAnnotationPresent(Serializable.class)) {
            return (N) serializeObject(jvmInstance);
        }

        throw new SerializerException(LangDiagnosticCode.FAILED_SERIALIZE, jvmInstance.getClass());
    }


    // @Override
    protected M serializeObject(Object jvmInstance) {
        Class<?> jvmType = jvmInstance.getClass();
        M rootMap = astFactory.createMapNode();

        for (Field field : getAllFields(jvmType)) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(DataIgnore.class)) continue;

            if (field.isAnnotationPresent(AnySetter.class)) {
                Map<?, ?> map;
                try {
                    map = (Map<?, ?>) field.get(jvmInstance);
                } catch (IllegalAccessException e) {
                    throw new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, field.getName());
                }
                if (map != null) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        K keyNode = serializeKey(entry.getKey());
                        N valueNode = serialize(entry.getValue());

                        // TODO use factory to create key
                        rootMap.put(keyNode, valueNode);
                    }
                }
                continue;
            }

            Object value;
			try {
				value = field.get(jvmInstance);
			} catch (IllegalAccessException e) {
                throw new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, field.getName());
			}
            if (value != null) {
                String keyName = field.isAnnotationPresent(DataField.class)
                    ? field.getAnnotation(DataField.class).key() : field.getName();
                if (keyName == null || keyName.isEmpty()) keyName = field.getName();

                // N keyNode = castToNode(astFactory.createScalarKeyNode(keyName));
                K keyNode = astFactory.createKey(keyName);

                N valueNode = serialize(value);
                rootMap.put(keyNode, valueNode);
            }
        }

        // 2. Process dynamic settings (@YamlAnyGetter)
        for (Method method : getAllMethods(jvmType)) { //.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AnyGetter.class)) {
                method.setAccessible(true);

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
                        rootMap.put(keyNode, valueNode);
                    }
                }
            }
        }
        return rootMap;
    }

    /// Serializes a List into a YamlSequence.
    protected L serializeList(List<?> list) {
        // TODO: Other SequencedCollection classes; Sets
        L sequence = astFactory.createSequenceNode();
        for (Object item : list) {
            if (item == null) continue;

            // Check if the item is itself serializable (a nested object)
            if (item.getClass().isAnnotationPresent(Serializable.class)) {
                // Recursive call for nested objects (e.g., JsonSchemaAssociation)
                sequence.add(castToNode(serializeObject(item)));
            } else {
                // Assume it's a primitive/string (e.g., List<String>)
                sequence.add(serialize(item));
            }
        }
        return sequence;
    }

    protected M serializeMap(Map<?, ?> map) {
        M yamlMap = astFactory.createMapNode();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;

            // TODO: Key might not be a scalar
            // N keyNode = castToNode(astFactory.createScalarKeyNode(entry.getKey()));
            K keyNode = astFactory.createKey(entry.getKey());

            // TODO: Should this be literal null instead of empty string?
            // Surely JSON treats them differently.
            N valueNode = (entry.getValue() == null)
                ? castToNode(astFactory.createScalarNode(""))
                : serialize(entry.getValue());

            yamlMap.put(keyNode, valueNode);
        }
        return yamlMap;
    }

    //
    // Deserialization Methods
    //

    /// Converts an AST structure back into a Java object of the generic type referenced by typeRef.
    @SuppressWarnings("unchecked")
    protected <C> C deserialize(AstNode node, TypeReference<C> typeRef) throws SerializerException {
        return (C) deserializeWithType(node, typeRef.getType());
    }

    private Object deserializeWithType(AstNode node, Type type) throws SerializerException {
        if (node == null || (node instanceof ScalarAstNode s && s.getPrimitive() == null)) {
            return null;
        }

        // Class<?>: decide between scalar vs POJO
        if (type instanceof Class<?> cls) {

            // scalar types must NOT go through POJO deserialization
            if (isScalarType(cls)) {
                if (node instanceof ScalarAstNode scalar) {
                    return deserializeScalar(scalar, cls);
                }
                throw new SerializerException(node, LangDiagnosticCode.EXPECTED_SEQUENCE,
                    node.getClass().getSimpleName(), cls.getSimpleName());
            }

            // non‑scalar class → existing POJO path
            return deserialize(node, cls);
        }

        if (type instanceof Class<?> cls) {
            // existing Class‑based path
            return deserialize(node, cls);
        }

        if (type instanceof ParameterizedType pt) {
            return deserializeParameterized(node, pt);
        }

        // Wildcards / type variables: fall back to upper bound or Object
        if (type instanceof java.lang.reflect.WildcardType wt) {
            Type[] upper = wt.getUpperBounds();
            return deserializeWithType(node, upper.length > 0 ? upper[0] : Object.class);
        }

        if (type instanceof java.lang.reflect.TypeVariable<?> tv) {
            Type[] bounds = tv.getBounds();
            return deserializeWithType(node, bounds.length > 0 ? bounds[0] : Object.class);
        }

        return deserializeField(node, null, Object.class);
    }

    private Object deserializeParameterized(AstNode node, ParameterizedType pt) throws SerializerException {
        Type raw = pt.getRawType();
        Type[] args = pt.getActualTypeArguments();

        if (!(raw instanceof Class<?> rawClass)) {
            return deserializeField(node, null, Object.class);
        }

        // Collections
        if (Collection.class.isAssignableFrom(rawClass)) {
            Type itemType = args.length > 0 ? args[0] : Object.class;
            Collection<Object> coll = newCollectionInstance(rawClass);
            if (node instanceof SequenceAstNode<?> seq) {
                populateCollectionGeneric(seq, coll, itemType);
            }
            return coll;
        }

        // Maps
        if (Map.class.isAssignableFrom(rawClass)) {
            Type keyType   = args.length > 0 ? args[0] : Object.class;
            Type valueType = args.length > 1 ? args[1] : Object.class;
            Map<Object,Object> map = newMapInstance(rawClass);
            if (node instanceof MapAstNode<?,?,?> m) {
                populateMapGeneric(m, map, keyType, valueType);
            }
            return map;
        }

        // Parameterized POJO: e.g. Box<T>
        Object instance;
        try {
            instance = ((Class<?>) raw).getConstructor().newInstance();
        } catch (Exception e) {
            throw error(e, raw.toString());
        }
        return deserializeObject(node, instance);
    }

    /// Converts an AST structure back into a Java object of the specified type.
    @SuppressWarnings("unchecked")
    protected <C> C deserialize(AstNode node, Class<C> jvmType) throws SerializerException {
        // If the node is null, or it's a scalar representing a null value, we return null immediately.
        if (node == null || (node instanceof ScalarAstNode scalar && scalar.getPrimitive() == null)) {
            return null;
        }

        try {

            // 1. SHORTCUT: If the target is a standard Collection, bypass POJO logic
            if (Map.class.isAssignableFrom(jvmType)) {
                if (node instanceof MapAstNode mapNode) {
                    return (C) convertAstMapToStandardMap(mapNode);
                }
                return (C) new LinkedHashMap<>();
            }

            if (List.class.isAssignableFrom(jvmType)) {
                if (node instanceof SequenceAstNode seqNode) {
                    return (C) convertAstSequenceToStandardList(seqNode);
                }
                return (C) new ArrayList<>();
            }

            C jvmInstance;

            try {
                jvmInstance = jvmType.getConstructor().newInstance();
            } catch (Exception e) {
                throw error(e, jvmType.getConstructor());
            }
            return deserializeObject(node, jvmInstance);

        } catch (NoSuchMethodException e) {
            throw new SerializerException(node, e, LangDiagnosticCode.NO_SUCH_METHOD, jvmType.getSimpleName());
        }

    }

    @SuppressWarnings("unchecked")
    private <C> C deserializeObject(AstNode node, C jvmInstance) {
        Class<?> jvmType = jvmInstance.getClass();
        Set<String> claimedKeys = new HashSet<>();

        // 2. We now check against the generic MapAstNode interface
        if (!(node instanceof MapAstNode mapNode)) {
            throw new SerializerException(node, LangDiagnosticCode.EXPECTED_MAP_STRUCTURE, jvmType.getSimpleName());
        }

        // 3. Process Declared Fields
        for (Field field : getAllFields(jvmType)) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(DataIgnore.class)) continue;

            // Determine the key for this field
            String key = field.getName();
            if (field.isAnnotationPresent(DataField.class)) {
                String annotatedKey = field.getAnnotation(DataField.class).key();
                if (annotatedKey != null && !annotatedKey.isEmpty()) {
                    key = annotatedKey;
                }
            }

            claimedKeys.add(key);

            // Use the new generic 'get' method
            AstNode valueNode = mapNode.get(key);

            if (valueNode != null) {
                // We pass field.getType() so it knows this is a List, a String, etc.
                Object convertedValue = deserializeField(valueNode, field, field.getType());
                if (convertedValue != null) {
                    try {
                        field.set(jvmInstance, convertedValue);
                    } catch (Exception e) {
                        throw error(e, field.getName());
                    }
                }
            }
        }

        // 4. Handle dynamic properties via @YamlAnySetter
        processAnySetter(jvmInstance, (M)node, claimedKeys, jvmType);

        return jvmInstance;
    }

    /// Dispatches a node to the correct deserialization logic.
    /// @param node The AST node to convert.
    /// @param field The field being populated (can be null for nested elements).
    /// @param targetType The class type to convert to.
    /// Dispatches a node to the correct deserialization logic based on target type.
    private Object deserializeField(AstNode node, Field field, Class<?> targetType) {
        if (node == null) return null;

        // 1. High Priority Symmetrical Check: Intercept custom YAML type serializers
        TypeDescriptor<?> typeDescriptor = getTypeDescriptor(targetType);
        if (typeDescriptor instanceof TypeSerializer<?> typeSerializer) {
            return typeSerializer.deserialize(node);
        }

        // // 2. Nested @Serializable objects
        // if (targetType.isAnnotationPresent(Serializable.class)) {
        //     //
        //     // TODO: If the field is generic, we should ideally be calling
        //     // the version of deserialize that takes a TypeReference.
        //     //
        //     return deserialize((AstNode)node, targetType);
        // }

        // 2. Collections
        if (List.class.isAssignableFrom(targetType)) {
            return deserializeList(node, field);
        }
        if (Map.class.isAssignableFrom(targetType)) {
            return deserializeMap(node, field);
        }

        // 3. Scalars (Primitives, Strings, Enums)
        if (node instanceof ScalarAstNode scalar) {

            // ScalarDescriptor
            if (typeDescriptor instanceof ScalarDescriptor descriptor) {
                Object val = scalar.getPrimitive();
                String stringValue = val != null ? val.toString() : "";
                try {
                    Object object = descriptor.toJvmType(stringValue);
                    return object;
                } catch (Exception e) {
                    throw new SerializerException(node, e, LangDiagnosticCode.FAILED_TO_MAP_TYPE, targetType.getSimpleName(), e.getMessage());
                }
            }

            return deserializeScalar(scalar, targetType);
        }

        if (targetType == Object.class) {
            if (node instanceof MapAstNode mapNode) {
                return convertAstMapToStandardMap(mapNode);
            }
            if (node instanceof SequenceAstNode seqNode) {
                return convertAstSequenceToStandardList(seqNode);
            }
            if (node instanceof ScalarAstNode scalar) {
                return scalar.getPrimitive();
            }
            return node;
        }

        return deserialize(node, targetType);

        // // Likely cause of arriving here is that the target type either:
        // //   - Doesn't have the @Serializable annotation
        // //   - Is in a package that's not opened to cascara.lang.yaml
        // //
        // // Strictness: If we got here, the AST structure doesn't match the Java model
        // throw new SerializerException(node, LangDiagnosticCode.INCOMPATIBLE_TYPES,
        //     node.getClass().getSimpleName(), targetType.getSimpleName()
        // );
    }

    private List<?> deserializeList(AstNode node, Field field) {
        if (node == null) return new ArrayList<>();
        Class<?> itemType = ReflectionUtils.getGenericTypeOfListField(field);

        // Fallback for single values in YAML where a list was expected
        if (node instanceof ScalarAstNode scalar) {
            Object val = deserializeScalar(scalar, itemType);
            // If the value is null (like an empty key), return an empty mutable list
            if (val == null) return new ArrayList<>();

            // Otherwise, return a mutable list with the single item
            ArrayList<Object> singleList = new ArrayList<>();
            singleList.add(val);
            return singleList;
        }

        if (!(node instanceof SequenceAstNode sequence)) {
            throw new SerializerException(node, LangDiagnosticCode.EXPECTED_SEQUENCE, field.getName());
        }

        List<Object> result = new ArrayList<>();
        for (AstNode item : sequence.getChildren()) {
            Object val = deserializeField(item, null, itemType);
            // YAML sequences can have null entries (- ), we should decide if we allow them.
            // Usually, for a list of strings/objects, we skip nulls or add them.
            result.add(val);
        }
        return result;
    }

    private Map<?, ?> deserializeMap(AstNode node, Field field) {
        if (!(node instanceof MapAstNode)) return new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
		M mapNode = (M)node;

        Class<?> keyType = ReflectionUtils.getGenericTypeOfMapKey(field);
        Class<?> valType = ReflectionUtils.getGenericTypeOfMapValue(field);
        Map<Object, Object> result = new LinkedHashMap<>();

        for (E entry : mapNode.getEntries()) {
            // Object primitiveKey = (entry.getKey() instanceof ScalarAstNode scalar)
            //         ? scalar.getPrimitive()
            //         : entry.getKey().toString();
            // Object key = deserializeScalar(primitiveKey, keyType);

            Object key;
            if (entry.getKey() instanceof ScalarAstNode scalarKey) {
                key = deserializeScalar(scalarKey, keyType);
            } else {
                // TODO: Implement this...
                throw new SerializerException(node, GenericDiagnosticCode.ERROR, "Non-scalar key not implemented");
            }

            Object val = deserializeField(entry.getValue(), field, valType);
            if (key != null) result.put(key, val != null ? val : ""); // TODO: Is "" okay here?
        }

        return result;
    }

    /// Converts a primitive value (already inferred by the AST) or a raw string into the target Java type.
    @SuppressWarnings({"rawtypes", "unchecked" })
    private Object deserializeScalar(ScalarAstNode scalar, Class<?> targetType) throws SerializerException {
        Object jvmInstance = scalar.getPrimitive();

        if (jvmInstance == null) return null;

        // 1. Exact Match / Wrapper Match
        if (targetType.isInstance(jvmInstance) ||
           (targetType.isPrimitive() && getWrapperClass(targetType).isInstance(jvmInstance))) {
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
                throw new SerializerException(scalar, e, LangDiagnosticCode.FAILED_DESERIALIZE_SCALAR, jvmInstance.getClass(), e.getMessage());
            }
        }

        // 4. String-Based Parsing (Fallback for quoted values or string-only types)
        if (targetType == String.class) return text;

        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, text);
        }

        // TODO:
        // We reach here through non-JPMS tests run through Gradle.
        // Proper solution is black box testing, make the tests their own module.
        // Quick fix might be to let the caller tell the serializer what type descriptors to use.

        throw new SerializerException(scalar, LangDiagnosticCode.UNSUPPORTED_TYPE, targetType.getSimpleName());
    }

    //
    // Deserialization Helpers
    //

    private Collection<Object> newCollectionInstance(Class<?> raw) {
        if (List.class.isAssignableFrom(raw)) return new ArrayList<>();
        if (Set.class.isAssignableFrom(raw))  return new HashSet<>();
        return new ArrayList<>();
    }

    private Map<Object,Object> newMapInstance(Class<?> raw) {
        if (LinkedHashMap.class.isAssignableFrom(raw)) return new LinkedHashMap<>();
        return new LinkedHashMap<>();
    }

    private void populateCollectionGeneric(SequenceAstNode<?> seqNode,
                                        Collection<Object> collection,
                                        Type itemType) {
        for (AstNode child : seqNode.getChildren()) {
            Object val = deserializeWithType(child, itemType);
            collection.add(val);
        }
    }

    private void populateMapGeneric(MapAstNode<?,?,?> mapNode,
                                    Map<Object,Object> map,
                                    Type keyType,
                                    Type valueType) {
        for (MapEntryAstNode<?,?> entry : mapNode.getEntries()) {
            Object key;
            if (entry.getKey() instanceof ScalarAstNode scalarKey) {
                key = deserializeScalar(scalarKey, keyType instanceof Class<?> kc ? kc : String.class);
            } else {
                key = entry.getKey().toString();
            }

            Object value = deserializeWithType(entry.getValue(), valueType);
            map.put(key, value);
        }
    }

    private Map<String, Object> convertAstMapToStandardMap(MapAstNode<?,?,?> mapNode) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (MapEntryAstNode<?,?> entry : mapNode.getEntries()) {
            // Convert key (usually a scalar) to String
            String key = entry.getKey().toString();
            // Recursively convert the value
            Object value = deserializeField(entry.getValue(), null, Object.class);
            result.put(key, value);
        }
        return result;
    }

    private List<Object> convertAstSequenceToStandardList(SequenceAstNode<?> seqNode) {
        List<Object> result = new ArrayList<>();
        for (AstNode child : seqNode.getChildren()) {
            // Recursively convert each item in the list
            result.add(deserializeField(child, null, Object.class));
        }
        return result;
    }

    private Class<?> getWrapperClass(Class<?> jvmType) {
        if (jvmType == int.class) return Integer.class;
        if (jvmType == boolean.class) return Boolean.class;
        if (jvmType == long.class) return Long.class;
        if (jvmType == double.class) return Double.class;
        return jvmType;
    }

    private void processAnySetter(Object instance, M rootMap, Set<String> claimedKeys, Class<?> jvmType) {
        for (Method method : getAllMethods(jvmType)) {
            if (method.isAnnotationPresent(AnySetter.class)) {
                method.setAccessible(true);
                for (E entry : rootMap.getEntries()) {
                    // entry.getKey() returns an Astode.
                    // We use toString() because our ScalarAstNode override returns stringValue.
                    String key = entry.getKey().toString();

                    if (!claimedKeys.contains(key) && !isSchemaOrId(key)) {
                        Object value;
                        AstNode valueNode = entry.getValue();
                        if (valueNode instanceof ScalarAstNode scalar) {
                            value = scalar.getPrimitive();
                        } else if (valueNode instanceof MapAstNode map) {
                            value = convertAstMapToStandardMap(map);
                        } else if (valueNode instanceof SequenceAstNode seq) {
                            value = convertAstSequenceToStandardList(seq);
                        } else {
                            // If it's a complex object (Map/List), for now we pass the AST node
                            // or we'd need a recursive "astToMap" helper.
                            value = valueNode;
                        }
                        try {
                            method.invoke(instance, key, value);
                        } catch (IllegalAccessException e) {
                            throw new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, method.getName());
                        } catch (InvocationTargetException e) {
                            throw new SerializerException(e, LangDiagnosticCode.INVOCATION_TARGET_EXCEPTION, method.getName());
                        }
                    }
                }
            }
        }
    }

    private boolean isSchemaOrId(String key) {
        return "$schema".equals(key) || "$id".equals(key);
    }

    //
    // Serialization Helpers
    //

    /// Retrieves all declared fields for a class and all its superclasses (excluding Object).
    protected List<Field> getAllFields(Class<?> jvmType) {
        List<Field> fields = new ArrayList<>();

        // Start with the current class and move up the hierarchy
        Class<?> currentClass = jvmType;

        // Stop when we reach Object.class, as it has no serializable fields we care about
        while (currentClass != null && currentClass != Object.class) {
            // Add all fields declared in the current class (but not its superclasses)
            for (Field field : currentClass.getDeclaredFields()) {
                fields.add(field);
            }
            // Move up to the superclass for the next iteration
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    protected List<Method> getAllMethods(Class<?> jvmType) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = jvmType;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                methods.add(m);
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    @SuppressWarnings("unchecked")
    private <V extends AstNode> N castToNode(V node) {
        return (N) node;
    }

    //
    //
    //

    private boolean isScalar(Object jvmInstance) {
        return (jvmInstance instanceof String ||
            jvmInstance instanceof Number ||
            jvmInstance instanceof Byte ||
            jvmInstance instanceof Character ||
            jvmInstance instanceof Boolean);
    }

    private boolean isScalarType(Class<?> t) {
        return t.isPrimitive()
            || t == String.class
            || Number.class.isAssignableFrom(t)
            || t == Boolean.class
            || t.isEnum();
    }

    protected TypeDescriptor<?> getTypeDescriptor(Class<?> jvmType) {
        // 1. First check if one has been registered locally
        if (typeDescriptors.containsKey(jvmType)) {
            return typeDescriptors.get(jvmType);
        }

        // 2. Use service provider layer to get one
        TypeDescriptor<?> descriptor = providerFactory.createTypeDescriptor(jvmType);
        typeDescriptors.put(jvmType, descriptor);
        return descriptor;
    }

    protected SerializerException error(Throwable t, Object... details) {
        if (t instanceof InstantiationException e) {
            return new SerializerException(e, LangDiagnosticCode.INSTANTIATION_EXCEPTION, details);
        }
        // IllegalAccessException - if this Method object is enforcing Java language access control and the underlying method is inaccessible.
        else if (t instanceof IllegalAccessException e) {
            return new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, details);
        }
        // IllegalArgumentException - if the method is an instance method and the specified object argument is not an instance of the class or interface declaring the underlying method (or of a subclass or implementor thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
        else if (t instanceof IllegalArgumentException e) {
            return new SerializerException(e, LangDiagnosticCode.ILLEGAL_ARGUMENT_EXCEPTION, details);
        }
        // InvocationTargetException - if the underlying method throws an exception.
        else if (t instanceof InvocationTargetException e) {
            return new SerializerException(e, LangDiagnosticCode.INVOCATION_TARGET_EXCEPTION, details);
        }
        else if (t instanceof NoSuchMethodException e) {
            return new SerializerException(e, LangDiagnosticCode.NO_SUCH_METHOD, details);
        }
        // ExceptionInInitializerError - if the initialization provoked by this method fails.
        else if (t instanceof ExceptionInInitializerError e) {
            return new SerializerException(e, LangDiagnosticCode.EXCEPTION_IN_INITIALIZER, details);
        }
        // NullPointerException - if the specified object is null and the method is an instance method.
        else if (t instanceof NullPointerException e) {
            return new SerializerException(e, GenericDiagnosticCode.NPE, details);
        }
        else {
            return new SerializerException(t, GenericDiagnosticCode.ERROR, t.getMessage());
        }
    }
}
