package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.util.Properties;

public class ServiceProviderMetadata {
    private Class<?> type;
    // private Object instance;
    private Properties capabilities;
    private String location;

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public Properties getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Properties capabilities) {
        this.capabilities = capabilities;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCapability(String name) {
        return capabilities.getString(name);
    }
}
