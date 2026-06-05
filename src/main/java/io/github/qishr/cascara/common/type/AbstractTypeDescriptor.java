package io.github.qishr.cascara.common.type;

import io.github.qishr.cascara.common.util.Properties;

public abstract class AbstractTypeDescriptor implements TypeDescriptor {
    protected static final String TYPE_KEYWORD = "type";
    protected static final String FORMAT_KEYWORD = "format";

    private Properties properties = new Properties();

    protected AbstractTypeDescriptor(Class<?> type, String schemaType, String format) {
        properties.set("javaType", type.getName());
        if (schemaType != null && !schemaType.isEmpty()) {
            properties.set("schemaType", schemaType);
        }
        if (format != null && !format.isEmpty()) {
            properties.set("schemaFormat", format);
        }
    }

    public Properties getServiceProperties() {
        return properties;
    }
}
