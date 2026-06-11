package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.util.Properties;

public abstract class AbstractTypeDescriptor<T> implements TypeDescriptor<T> {
    private static final String KEYWORD_TYPE = "type";

    protected Properties properties = new Properties();

    private Class<T> jvmType;
    private String schemaType;

    protected AbstractTypeDescriptor(Class<T> jvmType, String schemaType) {
        this.jvmType = jvmType;
        this.schemaType = schemaType;
        properties.set("javaType", jvmType.getName());
        if (schemaType != null && !schemaType.isEmpty()) {
            properties.set(KEYWORD_TYPE, schemaType);
        }
    }

    @Override
    public Properties getServiceProperties() {
        return properties;
    }

    @Override
    public Class<T> getJvmType() {
        return jvmType;
    }

    @Override
    public String getSchemaType() {
        return schemaType;
    }

    @Override
    public void populateSchema(MapAstNode<?,?> node) {
        // Automatically inject the type property every JSON Schema definition requires.
        node.put(KEYWORD_TYPE, getSchemaType());
    }
}
