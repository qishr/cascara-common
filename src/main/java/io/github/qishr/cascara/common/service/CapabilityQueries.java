package io.github.qishr.cascara.common.service;

import java.util.function.Predicate;

import io.github.qishr.cascara.common.util.Properties;

public class CapabilityQueries {

    /// Matches if a property has a specific exact value (matches JSON Schema types like String, Boolean, Number)
    public static Predicate<Properties> hasExactValue(String key, Object expectedValue) {
        return props -> props.containsKey(key) && expectedValue.toString().equals(props.getString(key));
    }

    /// Matches if a property is a boolean flag set to true
    public static Predicate<Properties> isTrue(String key) {
        return props -> props.containsKey(key) && Boolean.TRUE.equals(props.getBoolean(key, false));
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