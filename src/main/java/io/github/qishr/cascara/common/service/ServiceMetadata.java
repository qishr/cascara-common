package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.util.Properties;

public class ServiceMetadata {
    private final Class<? extends ServiceProvider> type;
    private Class<?> capabilityType;
    private final Properties properties;

    public ServiceMetadata(Class<? extends ServiceProvider> type, Properties properties) {
        this.type = type;
        this.properties = properties;
        String capTypeString = properties.getString("javaType");
        try {
            capabilityType = capTypeString == null ? null : Class.forName(capTypeString);
        } catch (Exception e) {
        }
    }

    public boolean getBooleanCapability(String capName) {
        if (capName == null || capName.isEmpty()) return false;
        return properties.getBoolean(capName, false);
    }

    public String getModuleName() {
        return type.getModule().getName();
    }

    // TODO: This should be getProviderTypeName or getProviderClassName
    public String getTypeName() {
        return type.getName();
    }

    // TODO: This should be getProviderType or getProviderClass
    public Class<? extends ServiceProvider> getType() {
        return type;
    }

    public Class<?> getCapabilityType() {
        return capabilityType;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String name) {
        if (properties == null) return null;
        return properties.getString(name);
    }

    public String getJarPath() {
        return getProperty("jarPath");
    }

    public String getTitle() {
        return getProperty("title");
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceMetadata honesty = (ServiceMetadata) o;
        return java.util.Objects.equals(type, honesty.type);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type);
    }

    @Override
    public String toString() {
        return type.getSimpleName();
    }
}
