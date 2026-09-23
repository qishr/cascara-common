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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.data.TabularData;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.UnexpectedNullParameterException;
import io.github.qishr.cascara.common.diagnostic.UnexpectedNullReturnException;
import io.github.qishr.cascara.common.diagnostic.UnimplementedMethodException;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.util.Duplicable;

public class Properties implements TabularData, Duplicable<Properties> {
    List<Property<?>> propertiesList = new ArrayList<>();
    Map<String,Property<?>> propertiesMap = new HashMap<>();

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
            throw new UnexpectedNullParameterException("name");
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

    public Object getValue(String name) {
        return get(name);
    }

    @Nullable
    public <T> T get(String name) {
        if (name == null) {
            throw new UnexpectedNullParameterException("name");
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties set(String name, Object value) {
        Property prop = getProperty(name);
        if (prop == null) {
            prop = new Property<>(name);
            add(prop);
        }
        prop.setValue(value);
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties set(String k, double v) {
        Property prop = getProperty(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties set(String k, int v) {
        Property prop = getProperty(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Properties set(String k, boolean v) {
        Property prop = getProperty(k);
        if (prop == null) {
            prop = new Property(k);
            add(prop);
        }
        prop.setValue(v);
        return this;
    }

    public void addAll(Properties properties) {
        for (Property<?> prop : properties.asList()) {
            set(prop.getName(), prop.getValue());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void add(Property<?> property) {
        Property existing = getProperty(property.getName());
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
        for (Property<?> prop : propertiesList) {
            sb.append(prop.getName());
            sb.append(" = ");
            sb.append(prop.getValue());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static Properties parse(String text) {
        return PropertyParser.parse(text);
    }

    public static Properties load(Path path) throws LocalizableIOException {
        if (Files.isRegularFile(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                return PropertyParser.parse(content);
            } catch (IOException e) {
                throw new LocalizableIOException(FileDiagnosticCode.READ_ERROR, path);
            }
        } else {
            throw new LocalizableIOException(FileDiagnosticCode.FILE_NOT_FOUND, path);
        }
    }

    public static class PropertyParser {
        public static Properties parse(String content) {
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
}
