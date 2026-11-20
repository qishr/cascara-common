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
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.ConfigSetting.Source;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exception.ConfigException;
import io.github.qishr.cascara.common.i18n.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.Property;

public abstract class Config {

    private static final Map<Class<? extends Config>, Config> INSTANCES = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private String configNameSpace;
    private Map<String,ConfigField> configFields;
    private Properties settings;

    protected Path suppliedProperties;
    protected InputStream suppliedStream;

    // Virtual Reference Path
    private static Path vrp;

    protected Config() {}

    protected static <T extends Config> T get(Class<T> configClass) {
        return get(configClass, null, null, null);
    }

    protected static <T extends Config> T get(Class<T> configClass, Path properties) {
        return get(configClass, properties, null, null);
    }

    protected static <T extends Config> T get(Class<T> configClass, InputStream properties) {
        return get(configClass, null, properties, null);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Config> T get(Class<T> configClass, Path file, InputStream stream, Path vrp) {
        T instance = (T) INSTANCES.computeIfAbsent(configClass, clazz -> {
            try {
                // 1. Run constructor & inline Java field initializers
                T inst = (T) clazz.getDeclaredConstructor().newInstance();
                inst.suppliedProperties = file;
                inst.suppliedStream = stream;

                // 2. Virtual Refrence Path
                if (vrp != null) {
                    Config.vrp = vrp;
                }

                return inst;
            } catch (ReflectiveOperationException e) {
                throw new ConfigException(e, GenericMessage.CONFIG_LOAD_FAILED, clazz.getName());
            }
        });
        instance.ensureInitialized();
        return instance;
    }

    protected void ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
            init();
        }
    }

    public static void resetAll() {
        INSTANCES.clear();
        vrp = null;
    }

    public List<Setting> getSettings() {
        return settings.asList().stream().map(e -> (Setting)e).toList();
    }

    protected void init() {
        ConfigNamespace ns = getClass().getAnnotation(ConfigNamespace.class);
        if (ns != null) {
            configNameSpace = ns.value();
        }

        settings = new Properties(Setting.class);
        configFields = new HashMap<>();

        // 1. Create map of fields
        collectConfigFields(configFields);

        // 2. Load from global Cascara properties file
        if (getClass() != Cascara.class) {
            Path propertiesPath = Cascara.get().getCascaraPropertiesPath();
            mergePropertiesFile(propertiesPath, null);
        }

        // 3. Load from namespace properties supplie via API
        if (suppliedProperties != null) {
            mergePropertiesFile(suppliedProperties, configNameSpace);
        }

        if (suppliedStream != null) {
            mergePropertiesStream(suppliedStream, configNameSpace);
        }

        // 4. Load from namespace properties file
        Path propertiesPath = getPropertiesPath();
        if (propertiesPath != null) {
            mergePropertiesFile(propertiesPath, configNameSpace);
        }

        // 5. Load from system properties
        System.getProperties().forEach((k,v) -> {
            if (configNameSpace == null || k.toString().startsWith(configNameSpace)) {
                if (v != null && v instanceof String value) {
                    String name = k.toString();
                    if (include(name, Source.SYSTEM_PROPERTY)) {
                        Setting setting = new Setting(name, value, Source.SYSTEM_PROPERTY, null);
                        settings.add(setting);
                    }
                }
            }
        });

        // 6. Load from environment variables
        for (Entry<String,ConfigField> entry : configFields.entrySet()) {
            ConfigField field = entry.getValue();
            String value = System.getenv(field.envName);
            if (value != null) {
                if (include(field.propName, Source.ENVIRONMENT_VARIABLE)) {
                    Setting setting = new Setting(field.propName, value, Source.ENVIRONMENT_VARIABLE, null);
                    settings.add(setting);
                }
            }
        }

        // 7. Apply values to fields
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

    private void mergePropertiesFile(Path propertiesPath, String namespace) {
        try {
            Properties namespaceProperties = Properties.fromFile(propertiesPath);
            mergeProperties(namespaceProperties, namespace, Source.FILE, propertiesPath);
        } catch (IOException ignored) {
            // Ignore it
        }
    }

    private void mergePropertiesStream(InputStream stream, String namespace) {
        try {
            Properties namespaceProperties = Properties.fromStream(stream);
            mergeProperties(namespaceProperties, namespace, Source.STREAM, null);
        } catch (IOException ignored) {
            // Ignore it
        }
    }

    private void mergeProperties(Properties properties, String namespace, Source source, Path propertiesPath) {
        for (Property<?> prop : properties) {
            String qualifiedName = namespace == null
                ? prop.getName()
                : namespace + "." + prop.getName();
            if (include(qualifiedName, source)) {
                Setting setting = new Setting(qualifiedName, prop.asString(), source, propertiesPath);
                settings.add(setting);
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
                throw LocalizableRuntimeException.of(e);
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
            ConfigField configField = new ConfigField(field, anno, propName, envName, isMap);
            configFields.put(field.getName(), configField);

            try {
                Object value = field.get(this);
                if (value != null) {
                    if (isMap) {
                        Type keyType = ReflectionUtils.getKeyTypeOfMapType(field.getGenericType());
                        if (!ReflectionUtils.canAssign(keyType, String.class)) {
                            throw new LocalizableRuntimeException(LangMessage.NON_STRING_KEY, keyType);
                        }
                        settings.addAll(mapToSettings(configField, value));
                    } else {
                        Setting setting = new Setting(propName, value.toString(), Source.DEFAULT, null);
                        settings.add(setting);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw LocalizableRuntimeException.of(e);
            }
        }
    }

    private void setValue(ConfigField field, String valueString) {
        Object value = parseValue(valueString, field.field.getType());
        try {
            field.field.set(this, value);
        } catch (Exception e) {
            DiagnosticMessage dm = DiagnosticMessage.forException(e);
            throw new LocalizableRuntimeException(dm, dm.getDetails());
        }
    }

    private void setMapValues(ConfigField field) {
        Type valueType = ReflectionUtils.getValueTypeOfMapType(field.field.getGenericType());

        if (!(valueType instanceof Class valueClass)) {
            throw new IllegalArgumentException("Map value not a class: " + field.field.getName());
        }

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
            throw LocalizableRuntimeException.of(e);
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

        // 2. Bytes
        if (targetType == byte.class || targetType == Byte.class) {
            if (value.length() > 0) {
                return (byte) value.charAt(0);
            } else {
                return 0x00;
            }
        }

        // 3. Characters
        if (ReflectionUtils.canAssign(targetType, Character.class) || targetType.isAssignableFrom(char.class)) {
            if (value.length() > 0) {
                return value.charAt(0);
            } else {
                return '\0';
            }
        }

        // 4. String-related Types
        if (ReflectionUtils.canAssign(targetType, CharSequence.class)) {
            return value;
        }

        // 5. Enums
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
        }

        return null;
    }

    // When this is called, mapObject is known to be a Map and its key is known to be a String.
    private List<Setting> mapToSettings(ConfigField field, Object mapObject) {
        Map<?,?> map = (Map<?,?>)  mapObject;
        List<Setting> settings = new ArrayList<>();
        for (Entry<?,?> entry : map.entrySet()) {
            String stringValue = entry.getValue() == null
                ? null
                : entry.getValue().toString();
            String name = field.propName + "." + entry.getKey().toString();
            settings.add(new Setting(name, stringValue, Source.DEFAULT, null));
        }
        return settings;
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

        if (vrp == null) {
            // This needs to go via Cascara.getPath() because the Cascara instance
            // might be using a virtual fileystem that was specified by environment
            // variable rather than API, so Config's VRP won't be set.
            return Cascara.get().getPath(pathString);
        } else {
            return vrp.getFileSystem().getPath(pathString);
        }
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
