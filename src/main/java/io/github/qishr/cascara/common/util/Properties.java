package io.github.qishr.cascara.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.data.TableData;

public class Properties implements TableData, Duplicable<Properties> {
    List<Property> propertiesList = new ArrayList<>();
    Map<String,Property> propertiesMap = new HashMap<>();

    public boolean containsKey(String k) {
        return null != get(k);
    }

    @Override
	public Map<String, Object> getValuesMap() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getValuesMap'");
	}

    public List<Property> asList() {
        return propertiesList;
    }

    public Property get(String k) {
        List<Property> copy = new ArrayList<>(propertiesList);
        for (Property prop : copy) {
            if (prop.getName() == null) {
                System.out.println("shoud not be null");
            } else {
                if (prop.getName().equals(k)) {
                    return prop;
                }
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

    public String getString(String k, String defaultValue) {
        Property property = get(k);
        if (property == null) {
            return defaultValue;
        }
        return property.getValue();
    }

    public int getInt(String k, int defaultValue) {
        Property property = get(k);
        if (property == null) {
            return defaultValue;
        }
        return property.asInt(defaultValue);
    }

    public long getLong(String k, int defaultValue) {
        Property property = get(k);
        if (property == null) {
            return defaultValue;
        }
        return property.asLong(defaultValue);
    }

    public double getDouble(String k, double defaultValue) {
        Property property = get(k);
        if (property == null) {
            return defaultValue;
        }
        return property.asDouble(defaultValue);
    }

    public boolean getBoolean(String k, boolean defaultValue) {
        Property property = get(k);
        if (property == null) {
            return defaultValue;
        }
        return property.asBoolean(defaultValue);
    }

    public Properties set(String k, String v) {
        Property prop = get(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    public Properties set(String k, double v) {
        Property prop = get(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    public Properties set(String k, int v) {
        Property prop = get(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    public Properties set(String k, boolean v) {
        Property prop = get(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    public void addAll(Properties properties) {
        for (Property prop : properties.asList()) {
            set(prop.getName(), prop.getValue());
        }
    }

    public void add(Property property) {
        Property existing = get(property.getName());
        if (existing != null) {
            existing.setValue(property.getValue());
        } else {
            propertiesList.add(property);
            propertiesMap.put(property.getName(), property);
        }
    }

	@Override
    public Object[] getValues() {
        Object[] r = new Object[propertiesList.size()];
        int i = 0;
        for (Property property : propertiesList) {
            r[i] = property.getValue();
            i++;
        }
        return r;
    }

    public void remove(String k) {
        for (Property property : propertiesList) {
            if (property.getName().equals(k)) {
                propertiesList.remove(property);
                propertiesMap.remove(k);
                return;
            }
        }
    }

    public void remove(Property property) {
        remove(property.getName());
    }

    public void clear() {
        propertiesList.clear();
        propertiesMap.clear();
    }

    public boolean isEmpty() {
        return propertiesList.isEmpty();
    }

    @Override
    public Properties duplicate() {
        Properties copy = new Properties();
        for (Property prop : propertiesList) {
            copy.set(prop.getName(), prop.getValue());
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Property prop : propertiesList) {
            sb.append(prop.getName());
            sb.append(" = ");
            sb.append(prop.getValue());
            sb.append("\n");
        }
        return sb.toString();
    }
}
