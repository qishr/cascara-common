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
    private Map<String,Field> configFields;

    protected ConfigurationProperties() {

        PropertiesNamespace ns = getClass().getAnnotation(PropertiesNamespace.class);
        if (ns != null) {
            configNameSpace = ns.value();
        }

        List<Field> fields = ReflectionUtils.getAllFields(getClass(), f -> !f.isAnnotationPresent(ConfigurationProperty.class));

        configFields = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            configFields.put(field.getName(), field);
        }

    }

    public Object getValue(String fieldName) {
        Field field = getField(fieldName);
        String value = System.getProperty(getQualifiedName(fieldName));
        if (value != null) {
            // TODO: Cast to same type as field is declare as
            return parseValue(value, field.getType());
        }
        try {
            // The default declared value
            return field.get(this);
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
        Field field = getField(fieldName);
        if (!Map.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Not a map: " + fieldName);
        }
        try {
            return (Map<String,T>) field.get(this);
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

    private Field getField(String fieldName) {
        Field field = configFields.get(fieldName);
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
