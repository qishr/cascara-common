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


package io.github.qishr.cascara.common.property;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.nio.charset.StandardCharsets;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.data.TabularData;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exception.UnexpectedNullValueException;
import io.github.qishr.cascara.common.exception.UnexpectedNullReturnException;
import io.github.qishr.cascara.common.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.i18n.exception.LocalizableIOException;
import io.github.qishr.cascara.common.util.Duplicable;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public class Properties implements TabularData, Duplicable<Properties>, Iterable<Property<?>> {
    private List<Property<?>> propertiesList = new ArrayList<>();

    // TODO: Concurrency
    private Map<String,Property<?>> propertiesMap = new HashMap<>();

    private Class<? extends Property<?>> propertyClass;

    /// For enums that represent property names
    public static interface PropertyName {
        String asString();
    }

    public Properties() {}

    public Properties(Class<? extends Property<?>> propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Property<?> createProperty() {
        if (propertyClass != null) {
            try {
                return ReflectionUtils.createInstance(propertyClass);
            } catch (InstantiationException e) {
            }
        }
        return new Property<>();
    }

    public boolean containsKey(String k) {
        return null != getProperty(k);
    }

    @Override
	public Map<String, Object> getValuesMap() {
		throw new UnimplementedMethodException();
	}

    public List<Property<?>> asList() {
        return propertiesList;
    }

    @Nullable
    public <T> Property<T> getProperty(String name) {
        if (name == null) {
            throw new UnexpectedNullValueException("name");
        }

        List<Property<?>> snapshot = new ArrayList<>(propertiesList);

        for (Property<?> prop : snapshot) {
            String propName = prop.getName();
            if (propName == null) {
                throw new UnexpectedNullReturnException("prop", "getName");
            }
            if (propName.equals(name)) {
                @SuppressWarnings("unchecked")
                Property<T> typed = (Property<T>) prop;
                return typed;
            }
        }

        return null;
    }

    public int size() {
        return propertiesList.size();
    }

    public Object getValue(String name) {
        return get(name);
    }

    @Nullable
    public <T> T get(String name) {
        if (name == null) {
            throw new UnexpectedNullValueException("name");
        }

        List<Property<?>> snapshot = new ArrayList<>(propertiesList);

        for (Property<?> prop : snapshot) {
            String propName = prop.getName();
            if (propName == null) {
                throw new UnexpectedNullReturnException("prop", "getName");
            }
            if (propName.equals(name)) {
                @SuppressWarnings("unchecked")
                T value = (T) prop.getValue();
                return value;
            }
        }

        return null;
    }

    @Nullable
    public String getString(PropertyName name) {
        Property<?> property = getProperty(name.asString());
        if (property == null) {
            return null;
        }
        return property.asString();
    }

    @Nullable
    public String getString(String name) {
        Property<?> property = getProperty(name);
        if (property == null) {
            return null;
        }
        return property.asString();
    }

    public String getString(String name, String defaultValue) {
        Property<?> property = getProperty(name);
        if (property == null) {
            return defaultValue;
        }
        return property.asString();
    }

    public int getInt(String name, int defaultValue) {
        Property<?> property = getProperty(name);
        if (property == null) {
            return defaultValue;
        }
        return property.asInteger(defaultValue);
    }

    public long getLong(String name, long defaultValue) {
        Property<?> property = getProperty(name);
        if (property == null) {
            return defaultValue;
        }
        return property.asLong(defaultValue);
    }

    public double getDouble(String name, double defaultValue) {
        Property<?> property = getProperty(name);
        if (property == null) {
            return defaultValue;
        }
        return property.asDouble(defaultValue);
    }

    /// For consistency with JSONSchema
    public double getNumber(String name, double defaultValue) {
        return getDouble(name, defaultValue);
    }

    /// For consistency with JSONSchema
    public long getInteger(String name, long defaultValue) {
        return getLong(name, defaultValue);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        Property<?> property = getProperty(name);
        if (property == null) {
            return defaultValue;
        }
        return property.asBoolean(defaultValue);
    }

    public Properties set(PropertyName name, Object value) {
        if (name == null) {
            throw new UnexpectedNullValueException("name");
        }
        return set(name.asString(), value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties set(String name, Object value) {
        if (name == null) {
            throw new UnexpectedNullValueException("name");
        }
        Property existing = getProperty(name);
        if (existing != null) {
            existing.setValue(value);
        } else {
            Property property = createProperty();
            property.setName(name);
            property.setValue(value);
            propertiesList.add(property);
            propertiesMap.put(property.getName(), property);
        }
        return this;
    }

    /// Copies
    public Properties addAll(Properties properties) {
        if (properties == null) {
            throw new UnexpectedNullValueException("properties");
        }
        return addAll(properties.asList());
    }

    /// Copies
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties addAll(List<? extends Property<?>> properties) {
        if (properties == null) {
            throw new UnexpectedNullValueException("properties");
        }
        for (Property<?> foreign : properties) {
            Property local = getProperty(foreign.getName());
            if (local == null) {
                local = createProperty();
                local.copyFrom(foreign);
                propertiesList.add(local);
                propertiesMap.put(local.getName(), local);
            } else {
                local.setValue(foreign.getValue());
            }
        }
        return this;
    }

    // Adds without copying
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties add(Property<?> property) {
        Property existing = getProperty(property.getName());
        if (existing == null) {
            propertiesList.add(property);
            propertiesMap.put(property.getName(), property);
        } else {
            existing.copyFrom(property);
        }
        return this;
    }

	@Override
    public Object[] getValues() {
        Object[] r = new Object[propertiesList.size()];
        int i = 0;
        for (Property<?> property : propertiesList) {
            r[i] = property.getValue();
            i++;
        }
        return r;
    }

    public void remove(String k) {
        for (Property<?> property : propertiesList) {
            if (property.getName().equals(k)) {
                propertiesList.remove(property);
                propertiesMap.remove(k);
                return;
            }
        }
    }

    public void remove(Property<?> property) {
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
        for (Property<?> prop : propertiesList) {
            copy.set(prop.getName(), prop.getValue());
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(propertiesList.get(i));
        }
        return sb.toString();
    }

    /// Returns Iterator instance
    @Override
    public Iterator<Property<?>> iterator() {
        return new PropertyIterator(propertiesList);
    }

    public static Properties fromSystemProperties() {
        Properties properties = new Properties();
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String name = String.valueOf(entry.getKey());
            properties.set(name, entry.getValue());
        }
        return properties;
    }

    public static Properties parse(String text) {
        return PropertyParser.parse(text);
    }

    public static Properties fromFile(Path path) throws IOException {
        if (path == null) {
            throw new UnexpectedNullValueException("path");
        }
        if (Files.isRegularFile(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                return PropertyParser.parse(content);
            } catch (IOException e) {
                throw new LocalizableIOException(FileMessage.READ_ERROR, path);
            }
        } else {
            throw new LocalizableIOException(FileMessage.FILE_NOT_FOUND, path);
        }
    }

    public static Properties fromStream(InputStream is) throws IOException {
        if (is == null) {
            throw new UnexpectedNullValueException("is");
        }
        try {
            return parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.STREAM_READ_ERROR);
        }
    }

    public static class PropertyParser {
        public static Properties parse(String content) {
            if (content == null) {
                throw new UnexpectedNullValueException("content");
            }

            Properties properties = new Properties();

            for (String line : content.split("\\R")) {
                line = line.trim();

                // Skip blank lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                    continue;
                }

                // Find either '=' or ':'
                int separator = line.indexOf('=');
                int colon = line.indexOf(':');

                if (separator == -1 || (colon != -1 && colon < separator)) {
                    separator = colon;
                }

                if (separator == -1) {
                    // Treat a line without a separator as a key with an empty value
                    properties.set(line, "");
                } else {
                    String key = line.substring(0, separator).trim();
                    String value = line.substring(separator + 1).trim();

                    properties.set(key, value);
                }
            }

            return properties;
        }
    }

    static class PropertyIterator implements Iterator<Property<?>> {
        List<Property<?>> list;
        int currentIndex = 0;

        // initialize pointer to head of the list for iteration
        public PropertyIterator(List<Property<?>> list) {
            this.list = list;
        }

        // returns false if next element does not exist
        public boolean hasNext() {
            return currentIndex < list.size();
        }

        // return current data and update pointer
        public Property<?> next() {
            Property<?> data = list.get(currentIndex++);
            return data;
        }

        // implement if needed
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
