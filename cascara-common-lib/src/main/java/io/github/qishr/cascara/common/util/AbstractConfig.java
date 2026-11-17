package io.github.qishr.cascara.common.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Type;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import io.github.qishr.cascara.common.annotation.ConfigProperty;
import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exception.ConfigException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.annotation.ConfigNamespace;

@Experimental
public abstract class AbstractConfig {

    private static final Map<Class<? extends AbstractConfig>, AbstractConfig> INSTANCES = new ConcurrentHashMap<>();

    private String configNameSpace;

    private static class ConfigField {
        Field decl;
        String propName;
        String envName;
        boolean isMap;
        ConfigField(Field decl, String propName, String envName, boolean isMap) {
            this.decl = decl;
            this.propName = propName;
            this.envName = envName;
            this.isMap = isMap;
        }
    }

    protected AbstractConfig() {}

    @SuppressWarnings("unchecked")
    public static <T extends AbstractConfig> T get(Class<T> configClass) {
        return (T) INSTANCES.computeIfAbsent(configClass, clazz -> {
            try {
                // 1. Run constructor & inline Java field initializers
                T instance = (T) clazz.getDeclaredConstructor().newInstance();
                // 2. Bind sysprops, env vars, and properties files over defaults
                instance.init();
                return instance;
            } catch (ReflectiveOperationException e) {
                // throw new ConfigException("Failed to instantiate config: " + clazz.getName(), e);
                throw new ConfigException(e, GenericMessage.CONFIG_LOAD_FAILED, clazz.getName());
            }
        });
    }

    public static void reset() {
        INSTANCES.clear();
    }

    protected void init() {
        ConfigNamespace ns = getClass().getAnnotation(ConfigNamespace.class);
        if (ns != null) {
            configNameSpace = ns.value();
        }

        // 1. Create map of fields
        Map<String,ConfigField> configFields = new HashMap<>();
        collectConfigFields(configFields);

        // 2. Load from global properties file
        Properties configProperties = new Properties();
        if (getClass() != Cascara.class) {
            Path propertiesPath = Cascara.instance().getCascaraPropertiesPath();
            try {
                Properties cascaraProperties = Properties.fromFile(propertiesPath);
                configProperties.addAll(cascaraProperties);
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
                    configProperties.set(qualifiedName, prop.asString());
                }
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                // Ignore it
            }
        }

        // 4. Load from system properties
        System.getProperties().forEach((k,v) -> {
            if (configNameSpace == null || k.toString().startsWith(configNameSpace)) {
                if (v != null && v instanceof String val && !val.isBlank()) {
                    configProperties.set(k.toString(), val);
                }
            }
        });
        // TODO: Why does this not work instead of the above?
        // configProperties.addAll(Properties.fromSystemProperties());

        // 5. Load from environment variables
        for (Entry<String,ConfigField> entry : configFields.entrySet()) {
            ConfigField field = entry.getValue();
            String val = System.getenv(field.envName);
            if (val != null && !val.isBlank()) {
                configProperties.set(field.propName, val);
                continue;
            }
        }

        // 6. Apply values to fields
        for (Entry<String,ConfigField> entry : configFields.entrySet()) {
            ConfigField field = entry.getValue();
            if (field.isMap) {
                setMapValues(field, configProperties);
            } else {
                String value = configProperties.get(field.propName);
                if (value == null) {
                    continue;
                }
                setValue(field, value);
            }
        }
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

        FileSystem fs = Cascara.instance().getHomePath().getFileSystem();
        return fs.getPath(pathString);
    }

    private void setValue(ConfigField field, String valueString) {
        if (valueString == null) {
            return;
        }

        Object value = parseValue(valueString, field.decl.getType());

        try {
            field.decl.set(this, value);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }

    }

    private void setMapValues(ConfigField field, Properties configProperties) {
        Type valueType = ReflectionUtils.getValueTypeOfMapType(field.decl.getGenericType());

        if (!(valueType instanceof Class valueClass)) {
            throw new IllegalArgumentException("Map value not a class: " + field.decl.getName());
        }




        // TODO: Instead of overwriting the entire map, add to it.





        Map<String,Object> map = new HashMap<>();

        for (Property<?> property : configProperties) {
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
            field.decl.set(this, map);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void collectConfigFields(Map<String,ConfigField> configFields) {
        List<Field> fields = ReflectionUtils.getAllFields(
            getClass(),
            f -> f.isAnnotationPresent(ConfigProperty.class),
            c -> c != AbstractConfig.class
        );

        for (Field decl : fields) {
            // Try to make the field accessible.
            try {
                decl.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                // TODO: Exception?
                continue;
            }

            ConfigProperty anno = decl.getAnnotation(ConfigProperty.class);
            if (anno == null) {
                continue;
            }

            String propName =
                (configNameSpace == null
                    ? ""
                    : configNameSpace + ".") +
                (anno.name().isEmpty()
                    ? StringUtils.toKebabCase(decl.getName())
                    : StringUtils.toKebabCase(anno.name()));

            String envName =
                (configNameSpace == null
                    ? ""
                    : StringUtils.toSnakeCase(configNameSpace).toUpperCase() + "_") +
                (anno.name().isEmpty()
                    ? StringUtils.toSnakeCase(decl.getName())
                    : StringUtils.toSnakeCase(anno.name())).toUpperCase();

            boolean isMap = ReflectionUtils.canAssign(decl.getType(), Map.class);

            // Object rawValue;

            // try {
            //     rawValue = decl.get(this);
            // } catch (IllegalArgumentException | IllegalAccessException e) {
            //     // TODO: Exception?
            //     e.printStackTrace();
            //     continue;
            // }

            // String valueString = rawValue == null
            //     ? null
            //     : String.valueOf(rawValue);

            configFields.put(decl.getName(), new ConfigField(decl, propName, envName, isMap));
            // configFields.put(propName, new ConfigField(decl, propName, envName, isMap, valueString));
        }
    }

    //
    //
    //

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object parseValue(String text, Class<?> targetType) {
        if (text == null) {
            return null;
        }

        // 1. Numeric Narrowing
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(text);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(text);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(text);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(text);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(text);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(text);
        }

        // TODO...
        // if (targetType == byte.class || targetType == Byte.class) {
        //     return Byte.parseInt(text);
        // }

        // 2. Characters
        if (targetType.isAssignableFrom(Character.class) || targetType.isAssignableFrom(char.class)) {
            if (text.length() > 0) {
                return text.charAt(0);
            } else {
                return '\0';
            }
        }

        // 3. String-related Types
        if (targetType.isAssignableFrom(CharSequence.class)) {
            return text;
        }

        // 4. Enums
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, text.toUpperCase());
        }

        return null;
    }
}
