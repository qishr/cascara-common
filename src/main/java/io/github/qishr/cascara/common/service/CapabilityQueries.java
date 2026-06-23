package io.github.qishr.cascara.common.service;

import java.util.function.Predicate;

import io.github.qishr.cascara.common.util.Properties;

public class CapabilityQueries {

    /// Matches if a property has a specific exact value (matches JSON Schema types like String, Boolean, Number)
    public static Predicate<Properties> hasExactValue(String key, Object expectedValue) {
        return props -> {
            if (props.containsKey(key)) {
                if (expectedValue == null) {
                    return props.getString(key) == null;
                } else {
                    return expectedValue.toString().equals(props.getString(key));
                }
            } else {
                return expectedValue == null;
            }
        };
    }

    /// Matches if a property is a boolean flag set to true
    public static Predicate<Properties> isTrue(String key) {
        return props -> props.containsKey(key) && Boolean.TRUE.equals(props.getBoolean(key, false));
    }

    /// Matches if a property is a boolean flag set to true
    public static Predicate<Properties> supportsJvmType(Class<?> jvmType) {
        return props -> {
            String capTypeString = props.getString("javaType");
            Class<?> capabilityType;
            try {
                // TODO: For performance, have Properties be able to return the Class<?> instead of a string
                capabilityType = capTypeString == null ? null : Class.forName(capTypeString);
            } catch (ClassNotFoundException e) {
                return false;
            }
            return (capabilityType == null || capabilityType.isAssignableFrom(jvmType));
        };
    }

    /// Combines multiple capability predicates using logical AND (All must match)
    /// @return predicate
    @SafeVarargs
    public static Predicate<Properties> allOf(Predicate<Properties>... predicates) {
        Predicate<Properties> result = props -> true;
        for (Predicate<Properties> p : predicates) {
            result = result.and(p);
        }
        return result;
    }

    /// Combines multiple capability predicates using logical OR (At least one must match)
    @SafeVarargs
    public static Predicate<Properties> anyOf(Predicate<Properties>... predicates) {
        Predicate<Properties> result = props -> false;
        for (Predicate<Properties> p : predicates) {
            result = result.or(p);
        }
        return result;
    }
}