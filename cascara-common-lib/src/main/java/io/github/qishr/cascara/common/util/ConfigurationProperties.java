package io.github.qishr.cascara.common.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.ConfigurationProperty;
import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.PropertiesNamespace;

@Experimental
public abstract class ConfigurationProperties {

    @DataIgnore
    private String configNameSpace;

    @DataIgnore
    private Map<String,ConfigField> configFields;

    private record ConfigField(Field decl, String name) {}

    protected ConfigurationProperties() {

        PropertiesNamespace ns = getClass().getAnnotation(PropertiesNamespace.class);
        if (ns != null) {
            configNameSpace = ns.value();
        }

        List<Field> fields = ReflectionUtils.getAllFields(getClass(), f -> !f.isAnnotationPresent(ConfigurationProperty.class));

        configFields = new HashMap<>();
        for (Field decl : fields) {
            decl.setAccessible(true);
            ConfigurationProperty anno = decl.getAnnotation(ConfigurationProperty.class);
            String name = anno.name().isEmpty()
                ? decl.getName()
                : anno.name();
            configFields.put(decl.getName(), new ConfigField(decl, name));
        }

    }

    public Object getValue(String fieldName) {
        ConfigField field = getField(fieldName);

        String value = System.getProperty(getQualifiedName(fieldName));
        if (value != null) {
            // TODO: Cast to same type as field is declare as
            return parseValue(value, field.decl.getType());
        }

        // TODO: Other sources

        try {
            // The default declared value
            return field.decl.get(this);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // TODO: Better exception handling
        return null;
    }

    public <T> T getMapValue(String fieldName, Class<T> valueClass, String mapKey) {
        Map<String,T> map = getMap(fieldName, valueClass);
        return (T) map.get(mapKey);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String,T> getMap(String fieldName, Class<T> valueClass) {
        ConfigField field = getField(fieldName);
        if (!Map.class.isAssignableFrom(field.decl.getType())) {
            throw new IllegalArgumentException("Not a map: " + fieldName);
        }
        // TODO: This might need to happen once at start for performance


        String qn = getQualifiedName(field.name);

        // Try populating from sources
        Map<String,T> map = new HashMap<>();
        System.getProperties().forEach((k,v) -> {
            if (k instanceof String propKey) {

                System.out.println("propKey: " + propKey);

                if (propKey.startsWith(qn)) {
                    String mapKey = propKey.substring(qn.length() + 1);
                    String valString = v == null ? null : String.valueOf(v);
                    Object val = parseValue(valString, valueClass);
                    map.put(mapKey, (T) val);
                }
            }
        });

        // TODO: Other sources

        return map;

        // // Return declared default value
        // try {
        //     return (Map<String,T>) field.decl.get(this);
        // } catch (IllegalArgumentException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // } catch (IllegalAccessException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
        // // TODO: Better exception handling
        // return null;
    }

    public boolean getBoolean(String fieldName) {
        if (getValue(fieldName) instanceof Boolean v) {
            return v;
        }
        return false;
    }

    //
    //
    //

    private String getQualifiedName(String name) {
        if (configNameSpace == null || configNameSpace.isEmpty() || name.contains(".")) {
            return  name;
        }
        return configNameSpace + "." + name;
    }

    private ConfigField getField(String fieldName) {
        ConfigField field = configFields.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Not a config field: " + fieldName);
        }
        return field;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object parseValue(String text, Class<?> targetType) {
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
