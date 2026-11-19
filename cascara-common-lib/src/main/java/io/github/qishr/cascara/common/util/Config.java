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

package io.github.qishr.cascara.common.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.ConfigSetting.Source;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exception.ConfigException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.Property;

public abstract class Config {

    private static final Map<Class<? extends Config>, Config> INSTANCES = new ConcurrentHashMap<>();

    private String configNameSpace;
    private Map<String,ConfigField> configFields;
    private Properties settings;

    protected Config() {}

    @SuppressWarnings("unchecked")
    public static <T extends Config> T get(Class<T> configClass) {
        return (T) INSTANCES.computeIfAbsent(configClass, clazz -> {
            try {
                // 1. Run constructor & inline Java field initializers
                T instance = (T) clazz.getDeclaredConstructor().newInstance();
                // 2. Bind sysprops, env vars, and properties files over defaults
                instance.init();
                return instance;
            } catch (ReflectiveOperationException e) {
                throw new ConfigException(e, GenericMessage.CONFIG_LOAD_FAILED, clazz.getName());
            }
        });
    }

    public static void reset() {
        INSTANCES.clear();
    }

    public List<Setting> getSettings() {
        return settings.asList().stream().map(e -> (Setting)e).toList();
    }

    protected void init() {
        ConfigNamespace ns = getClass().getAnnotation(ConfigNamespace.class);
        if (ns != null) {
            configNameSpace = ns.value();
        }

        // 1. Create map of fields
        settings = new Properties(Setting.class);
        configFields = new HashMap<>();
        collectConfigFields(configFields);

        // 2. Load from global properties file
        if (getClass() != Cascara.class) {
            Path propertiesPath = Cascara.instance().getCascaraPropertiesPath();
            try {
                Properties cascaraProperties = Properties.fromFile(propertiesPath);
                for (Property<?> prop : cascaraProperties) {
                    if (include(prop.getName(), Source.FILE)) {
                        Setting setting = new Setting(prop.getName(), prop.asString(), Source.FILE, propertiesPath);
                        settings.add(setting);
                    }
                }
            } catch (IOException e) {
                // Ignore it
            }
        }

        // 3. Load from namespace properties file
        Path propertiesPath = getPropertiesPath();
        if (propertiesPath != null) {
            try {
                Properties namespaceProperties = Properties.fromFile(propertiesPath);
                for (Property<?> prop : namespaceProperties) {
                    String qualifiedName = configNameSpace == null
                        ? prop.getName()
                        : configNameSpace + "." + prop.getName();
                    if (include(qualifiedName, Source.FILE)) {
                        Setting setting = new Setting(qualifiedName, prop.asString(), Source.FILE, propertiesPath);
                        settings.add(setting);
                    }
                }
            } catch (IOException e) {
                // Ignore it
            }
        }

        // 4. Load from system properties
        System.getProperties().forEach((k,v) -> {
            if (configNameSpace == null || k.toString().startsWith(configNameSpace)) {
                if (v != null && v instanceof String val && !val.isBlank()) {
                    String name = k.toString();
                    if (include(name, Source.SYSTEM_PROPERTY)) {
                        Setting setting = new Setting(name, val, Source.SYSTEM_PROPERTY, null);
                        settings.add(setting);
                    }
                }
            }
        });

        // 5. Load from environment variables
        for (Entry<String,ConfigField> entry : configFields.entrySet()) {
            ConfigField field = entry.getValue();
            String val = System.getenv(field.envName);
            if (val != null && !val.isBlank()) {
                if (include(field.propName, Source.ENVIRONMENT_VARIABLE)) {
                    Setting setting = new Setting(field.propName, val, Source.ENVIRONMENT_VARIABLE, null);
                    settings.add(setting);
                }
            }
        }

        // 6. Apply values to fields
        for (Entry<String,ConfigField> entry : configFields.entrySet()) {
            ConfigField field = entry.getValue();
            if (field.isMap) {
                setMapValues(field);
            } else {
                String value = settings.get(field.propName);
                if (value == null) {
                    continue;
                }
                setValue(field, value);
            }
        }
    }

    private void collectConfigFields(Map<String,ConfigField> configFields) {
        List<Field> fields = ReflectionUtils.getFields(
            getClass(),
            f -> f.isAnnotationPresent(ConfigSetting.class),
            c -> c != Config.class
        );

        for (Field field : fields) {
            // Try to make the field accessible.
            try {
                field.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                // TODO: Exception?
                continue;
            }

            ConfigSetting anno = field.getAnnotation(ConfigSetting.class);

            String propName =
                (configNameSpace == null
                    ? ""
                    : configNameSpace + ".") +
                (anno.name().isEmpty()
                    ? StringUtils.toKebabCase(field.getName())
                    : anno.name());

            String envName =
                (configNameSpace == null
                    ? ""
                    : StringUtils.toSnakeCase(configNameSpace).toUpperCase() + "_") +
                (anno.name().isEmpty()
                    ? StringUtils.toSnakeCase(field.getName())
                    : StringUtils.toSnakeCase(anno.name())).toUpperCase();

            boolean isMap = ReflectionUtils.canAssign(field.getType(), Map.class);

            try {
                Object value = field.get(this);
                if (value != null) {
                    if (isMap) {
                        // TODO: Populate settings
                    } else {
                        Setting setting = new Setting(propName, value.toString(), Source.DEFAULT, null);
                        settings.add(setting);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            configFields.put(field.getName(), new ConfigField(field, anno, propName, envName, isMap));
        }
    }

    private void setValue(ConfigField field, String valueString) {
        Object value = parseValue(valueString, field.field.getType());
        try {
            field.field.set(this, value);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void setMapValues(ConfigField field) {
        Type valueType = ReflectionUtils.getValueTypeOfMapType(field.field.getGenericType());

        if (!(valueType instanceof Class valueClass)) {
            throw new IllegalArgumentException("Map value not a class: " + field.field.getName());
        }

        // TODO: Instead of overwriting the entire map, add to it?

        Map<String,Object> map = new HashMap<>();

        for (Property<?> property : settings) {
            String name = property.getName();
            if (name.startsWith(field.propName) && name.length() > field.propName.length()) {
                String valString = property.asString();
                Object val = parseValue(valString, valueClass);
                if (val == null) {
                    continue;
                }
                String simpleName =  name.substring(field.propName.length() + 1);
                map.put(simpleName, val);
            }
        }

        try {
            field.field.set(this, map);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object parseValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 1. Numeric Types
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        }

        // TODO...
        // if (targetType == byte.class || targetType == Byte.class) {
        //     return Byte.parseInt(text);
        // }

        // 2. Characters
        if (ReflectionUtils.canAssign(targetType, Character.class) || targetType.isAssignableFrom(char.class)) {
            if (value.length() > 0) {
                return value.charAt(0);
            } else {
                return '\0';
            }
        }

        // 3. String-related Types
        if (ReflectionUtils.canAssign(targetType, CharSequence.class)) {
            return value;
        }

        // 4. Enums
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
        }

        return null;
    }

    private boolean include(String name, Source source) {
        ConfigField field = null;
        for (ConfigField f : configFields.values()) {
            if (f.isMap && name.startsWith(f.propName + ".")) {
                field = f;
                break;
            } else if (f.propName.equals(name)) {
                field = f;
                break;
            }
        }
        if (field == null) {
            return false;
        }
        if (field.anno.include().length > 0) {
            boolean included = false;
            for (Source s : field.anno.include()) {
                if (s == source) {
                    included = true;
                    break;
                }
            }
            if (!included) return false;
        }
        if (field.anno.exclude().length > 0) {
            for (Source s : field.anno.exclude()) {
                if (s == source) {
                    return false;
                }
            }
        }
        return true;
    }

    private Path getPropertiesPath() {
        String fileName = configNameSpace == null
            ? "cascara"
            : configNameSpace;
        String envName = StringUtils.toSnakeCase(fileName).toUpperCase() + "_PROPERTIES";
        String propName = fileName + ".properties";

        String pathString = System.getenv(envName);
        if (pathString == null) {
            pathString = System.getProperty(propName);
        }

        if (pathString == null) {
            return null;
        }

        return Cascara.instance().getPath(pathString);
    }

    private static record ConfigField(
        Field field,
        ConfigSetting anno,
        String propName,
        String envName,
        boolean isMap
    ) {}

    public static class Setting extends Property<String> {
        private Source source;
        private Path path;

        public Setting(String name, String value, Source source, Path path) {
            super(name, value);
            this.source = source;
            this.path = path;
        }

        @Override
        public Setting copyFrom(Property<?> property) {
            super.copyFrom(property);
            if (property instanceof Setting s) {
                source = s.source;
                path = s.path;
            }
            return this;
        }

        @Override
        public Setting duplicate() {
            return new Setting(getName(), getValue(), source, path);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                .append(getName())
                .append(" = ")
                .append(getValue())
                .append(" (")
                .append(source);

            if (path != null) {
                sb.append(" ").append(path);
            }

            sb.append(")");
            return sb.toString();
        }

        public Source getSource() { return source; }
        public Path getPath() { return path; }
    }
}
