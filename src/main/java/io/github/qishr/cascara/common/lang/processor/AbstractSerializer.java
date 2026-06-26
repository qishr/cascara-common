package io.github.qishr.cascara.common.lang.processor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import io.github.qishr.cascara.common.lang.type.Primitive;
import io.github.qishr.cascara.common.lang.type.PrimitiveDelegate;
import io.github.qishr.cascara.common.lang.type.ScalarDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;
import io.github.qishr.cascara.common.lang.type.TypeSerializer;
import io.github.qishr.cascara.common.service.ServiceProviderFactory;
import io.github.qishr.cascara.common.util.Properties;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public abstract class AbstractSerializer<
    T extends Serializer<N>,
    N extends AstNode,
    S extends ScalarAstNode<N>,
    L extends SequenceAstNode<N>,
    M extends MapAstNode<N,E>,
    E extends MapEntryAstNode<N>
> implements Serializer<N> {
    protected Reporter reporter = new NoOpReporter();
    private Properties properties;
    private final String contentType;
    private final AstNodeFactory<N,S,L,M,E> astFactory;

    private final Map<Class<?>,TypeDescriptor<?>> typeDescriptors = new HashMap<>();
    private final ServiceProviderFactory providerFactory = new ServiceProviderFactory();

    private final PrimitiveDelegate primitiveDelegate;

    protected AbstractSerializer(String contentType, AstNodeFactory<N,S,L,M,E> astFactory, PrimitiveDelegate primitiveDelegate) {
        this.contentType = contentType;
        this.astFactory = astFactory;
        this.primitiveDelegate = primitiveDelegate;
    }

    protected abstract T self();

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
        if (isPrimitive(jvmInstance)) {
            return (N)astFactory.createScalarNode(jvmInstance);
        }

        TypeDescriptor<?> typeDescriptor = getTypeDescriptor(jvmInstance.getClass());

        if (typeDescriptor != null) {
            if (typeDescriptor instanceof TypeSerializer typeSerializer) {
				return castToNode(typeSerializer.serialize(jvmInstance));
            }

            if (typeDescriptor instanceof ScalarDescriptor descriptor) {
                Primitive primitive;
                try {
                    // If you're wondering why this is not a String,
                    // an Instant is not a String - it's a Long
                    primitive = descriptor.toPrimitive(jvmInstance).setDelegate(primitiveDelegate);
                } catch (Exception e) {
                    throw new SerializerException(e, LangDiagnosticCode.FAILED_TO_MAP_AST, jvmInstance.getClass().getSimpleName(), e.getMessage());
                }
                return (N)astFactory.createScalarNode(primitive);
            }
        }

        if (jvmInstance instanceof List<?> list) { // TODO: Other SequencedCollection classes; Sets
            return (N)serializeList(list);
        }

        if (jvmInstance instanceof Map<?, ?> map) {
            return (N)serializeMap(map);
        }

        // TODO: ALL OBJECTS should be handled this way. @Serializable should not be neccesary
        if (jvmInstance.getClass().isAnnotationPresent(Serializable.class)) {
            return (N)serializeObject(jvmInstance);
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
                        N keyNode = serialize(entry.getKey());
                        N valueNode = serialize(entry.getValue());
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

                N keyNode = castToNode(astFactory.createScalarKeyNode(keyName));

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

                    // TODO: Handle these gracefully

                    // IllegalAccessException - if this Method object is enforcing Java language access control and the underlying method is inaccessible.
                    // IllegalArgumentException - if the method is an instance method and the specified object argument is not an instance of the class or interface declaring the underlying method (or of a subclass or implementor thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
                    // InvocationTargetException - if the underlying method throws an exception.
                    // NullPointerException - if the specified object is null and the method is an instance method.
                    // ExceptionInInitializerError - if the initialization provoked by this method fails.

                } catch (IllegalAccessException e) {
                    throw new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, method.getName());
				} catch (InvocationTargetException e) {
                    throw new SerializerException(e, LangDiagnosticCode.INVOCATION_TARGET_EXCEPTION, method.getName());
				}

                if (result instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        N keyNode = serialize(entry.getKey());
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
        L sequence = astFactory.createSequenceNode();
        // L sequence = new YamlSequenceNode();
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
        // M yamlMap = new YamlMapNode();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;

            // TODO: Key might not be a scalar
            N keyNode = castToNode(astFactory.createScalarKeyNode(entry.getKey()));

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

    /// Converts a Yaml AST structure back into a Java object of the specified type.
    @SuppressWarnings("unchecked")
    protected <C> C deserialize(AstNode yaml, Class<C> jvmType) throws SerializerException {
        // If the YAML node is null, or it's a scalar representing a null value,
        // we return null immediately. This allows 'security: ' to map to a null Object.
        if (yaml == null || (yaml instanceof ScalarAstNode scalar && scalar.getPrimitive() == null)) {
            return null;
        }

        Set<String> claimedKeys = new HashSet<>();
        try {

            // 1. SHORTCUT: If the target is a standard Collection, bypass POJO logic
            if (Map.class.isAssignableFrom(jvmType)) {
                if (yaml instanceof MapAstNode mapNode) {
                    return (C) convertYamlMapToStandardMap(mapNode);
                }
                return (C) new LinkedHashMap<>();
            }

            if (List.class.isAssignableFrom(jvmType)) {
                if (yaml instanceof SequenceAstNode seqNode) {
                    return (C) convertYamlSequenceToStandardList(seqNode);
                }
                return (C) new ArrayList<>();
            }

            // 1. Validation
            if (!jvmType.isAnnotationPresent(Serializable.class)) {
                throw new SerializerException(yaml, LangDiagnosticCode.CLASS_NOT_SERIALIZABLE, jvmType.getSimpleName());
            }

            C instance;
            try {
                instance = jvmType.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new SerializerException(e, LangDiagnosticCode.INSTANTIATION_EXCEPTION, jvmType.getConstructor());
            } catch (IllegalAccessException e) {
                throw new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, jvmType.getConstructor());
            } catch (IllegalArgumentException e) {
                throw new SerializerException(e, LangDiagnosticCode.ILLEGAL_ARGUMENT_EXCEPTION, jvmType.getConstructor());
            } catch (InvocationTargetException e) {
                throw new SerializerException(e, LangDiagnosticCode.INVOCATION_TARGET_EXCEPTION, jvmType.getConstructor());
            }

            // 2. We now check against the generic MapAstNode interface
            if (!(yaml instanceof MapAstNode mapNode)) {
                throw new SerializerException(yaml, LangDiagnosticCode.EXPECTED_MAP_STRUCTURE, jvmType.getSimpleName());
            }

            // 3. Process Declared Fields
            for (Field field : getAllFields(jvmType)) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(DataIgnore.class)) continue;

                // Determine the YAML key for this field
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
                    Object convertedValue = deserializeNode(valueNode, field, field.getType());
                    if (convertedValue != null) {
                        try {
                            field.set(instance, convertedValue);
                        } catch (IllegalArgumentException e) {
                            throw new SerializerException(e, LangDiagnosticCode.ILLEGAL_ARGUMENT_EXCEPTION, field.getName());
                        } catch (IllegalAccessException e) {
                            throw new SerializerException(e, LangDiagnosticCode.FIELD_NOT_ACCESSIBLE, field.getName());
                        }
                    }
                }
            }

            // 4. Handle dynamic properties via @YamlAnySetter
            processAnySetter(instance, (M)yaml, claimedKeys, jvmType);

            return instance;
        } catch (NoSuchMethodException e) {
            throw new SerializerException(yaml, e, LangDiagnosticCode.NO_SUCH_METHOD, jvmType.getSimpleName());
        }
    }

    /// Dispatches a node to the correct deserialization logic.
    /// @param node The AST node to convert.
    /// @param field The field being populated (can be null for nested elements).
    /// @param targetType The class type to convert to.
    /// Dispatches a node to the correct deserialization logic based on target type.
    private Object deserializeNode(AstNode node, Field field, Class<?> targetType) {
        if (node == null) return null;

        // 1. High Priority Symmetrical Check: Intercept custom YAML type serializers
        TypeDescriptor<?> typeDescriptor = getTypeDescriptor(targetType);
        if (typeDescriptor instanceof TypeSerializer<?> typeSerializer) {
            return typeSerializer.deserialize(node);
        }

        // 2. Nested @Serializable objects
        if (targetType.isAnnotationPresent(Serializable.class)) {
            return deserialize((AstNode)node, targetType);
        }

        // 3. Collections
        if (List.class.isAssignableFrom(targetType)) {
            return deserializeList(node, field);
        }
        if (Map.class.isAssignableFrom(targetType)) {
            return deserializeMap(node, field);
        }

        // 4. Scalars (Primitives, Strings, Enums)
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
                return convertYamlMapToStandardMap(mapNode);
            }
            if (node instanceof SequenceAstNode seqNode) {
                return convertYamlSequenceToStandardList(seqNode);
            }
            if (node instanceof ScalarAstNode scalar) {
                return scalar.getPrimitive();
            }
            return node;
        }

        // Likely cause of arriving here is that the target type either:
        //   - Doesn't have the @Serializable annotation
        //   - Is in a package that's not opened to cascara.lang.yaml
        //
        // Strictness: If we got here, the AST structure doesn't match the Java model
        throw new SerializerException(node, LangDiagnosticCode.INCOMPATIBLE_TYPES,
            node.getClass().getSimpleName(), targetType.getSimpleName()
        );
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
            Object val = deserializeNode(item, null, itemType);
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

            Object val = deserializeNode(entry.getValue(), field, valType);
            if (key != null) result.put(key, val != null ? val : ""); // TODO: Is "" okay here?
        }

        return result;
    }

    /// Converts a primitive value (already inferred by the AST) or a raw string into the target Java type.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object deserializeScalar(ScalarAstNode scalar, Class<?> targetType) throws SerializerException {
        Object jvmInstance = scalar.getPrimitive();
        if (jvmInstance == null) return null;

        // 1. Exact Match / Wrapper Match
        if (targetType.isInstance(jvmInstance) ||
           (targetType.isPrimitive() && getWrapperClass(targetType).isInstance(jvmInstance))) {
            return jvmInstance;
        }

        // TODO: Might want to do this after type descriptor check

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

        Primitive yamlPrimitive = Primitive.of(jvmInstance);
        yamlPrimitive.setDelegate(primitiveDelegate);
        if (targetType == boolean.class || targetType == Boolean.class) {
            return yamlPrimitive.asBoolean(false);
        } else if (targetType == int.class || targetType == Integer.class) {
            return yamlPrimitive.asInteger(0);
        } else if (targetType == long.class || targetType == Long.class) {
            return (long) yamlPrimitive.asInteger(0); // TODO: add asLong to YamlPrimitive
        } else if (targetType == float.class || targetType == Float.class) {
            return (float) yamlPrimitive.asDouble(0); // TODO: add asFloat to YamlPrimitive
        } else if (targetType == double.class || targetType == Double.class) {
            return yamlPrimitive.asDouble(0);
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

    private Map<String, Object> convertYamlMapToStandardMap(MapAstNode<?,?> mapNode) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (MapEntryAstNode<?> entry : mapNode.getEntries()) {
            // Convert key (usually a scalar) to String
            String key = entry.getKey().toString();
            // Recursively convert the value
            Object value = deserializeNode(entry.getValue(), null, Object.class);
            result.put(key, value);
        }
        return result;
    }

    private List<Object> convertYamlSequenceToStandardList(SequenceAstNode<?> seqNode) {
        List<Object> result = new ArrayList<>();
        for (AstNode child : seqNode.getChildren()) {
            // Recursively convert each item in the list
            result.add(deserializeNode(child, null, Object.class));
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
                            value = convertYamlMapToStandardMap(map);
                        } else if (valueNode instanceof SequenceAstNode seq) {
                            value = convertYamlSequenceToStandardList(seq);
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

    private boolean isPrimitive(Object jvmInstance) {
        return (jvmInstance instanceof String ||
            jvmInstance instanceof Number ||
            jvmInstance instanceof Byte ||
            jvmInstance instanceof Character ||
            jvmInstance instanceof Boolean);
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
}
