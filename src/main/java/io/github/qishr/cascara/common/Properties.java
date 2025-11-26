package io.github.qishr.cascara.common;

import java.util.ArrayList;
import java.util.List;

public class Properties {
    List<Property> propertiesList = new ArrayList<>();

    public Property get(String k) {
        for (Property prop : propertiesList) {
            if (prop.getKey().equals(k)) {
                return prop;
            }
        }
        return null;
    }

    public String getString(String k) {
        Property property = get(k);
        if (property == null) {
            return null;
        }
        return property.getValue();
    }

    public boolean getBoolean(String k, boolean defaultValue) {
        Property property = get(k);
        if (property == null) {
            return defaultValue;
        }
        return property.getBoolean(defaultValue);
    }

    public List<Property> getAll() {
        return propertiesList;
    }

    public void set(String k, String v) {
        Property prop = get(k);
        if (prop == null) {
            prop = new Property(k);
            propertiesList.add(prop);
        }
        prop.setValue(v);
    }

    public void clear() {
        propertiesList.clear();
    }

    public boolean isEmpty() {
        return propertiesList.isEmpty();
    }

    public Properties duplicate() {
        Properties copy = new Properties();
        for (Property prop : propertiesList) {
            copy.set(prop.getKey(), prop.getValue());
        }
        return copy;
    }

    public void addAll(Properties properties) {
        for (Property prop : properties.getAll()) {
            set(prop.getKey(), prop.getValue());
        }
    }

    public void add(Property property) {
        Property existing = get(property.getKey());
        if (existing != null) {
            existing.setValue(property.getValue());
        } else {
            propertiesList.add(property);
        }
    }
}
