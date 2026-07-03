package io.github.qishr.cascara.common.service;

import java.util.function.Predicate;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.Properties;

public class CapabilityQueries {

    /// Matches if a property has a specific exact value (matches JSON Schema types like String, Boolean, Number)
    public static Predicate<ServiceMetadata> hasExactValue(String key, Object expectedValue) {
        return meta -> {
            if (meta.getContentType() != null && "contentType".equals(key)) {
                if (meta.getContentType().matches(String.valueOf(expectedValue))) {
                    return true;
                }
            }
            Properties props = meta.getProperties();
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
    public static Predicate<ServiceMetadata> isTrue(String key) {
        return meta -> {
            Properties props = meta.getProperties();
            return props.containsKey(key) && Boolean.TRUE.equals(props.getBoolean(key, false));
        };
    }

    /// Matches if a property is a boolean flag set to true
    public static Predicate<ServiceMetadata> supportsJvmType(Class<?> jvmType) {
        return meta -> {
            Properties props = meta.getProperties();
            String capTypeString = props.getString("javaType");
            Class<?> capabilityType;
            try {
                // TODO: For performance, have Properties be able to return the Class<?> instead of a string
                //    Since we changed Properties predicate to ServiceMetadata, it probably has this
                capabilityType = capTypeString == null ? null : Class.forName(capTypeString);
            } catch (ClassNotFoundException e) {
                return false;
            }
            return (capabilityType == null || capabilityType.isAssignableFrom(jvmType));
        };
    }

    /// Matches if a property is a boolean flag set to true
    @Beta
    public static Predicate<ServiceMetadata> supportsContentType(ContentType contentType) {
        return meta -> {
            return contentType.matches(meta.getContentType());
        };
    }

    /// Combines multiple capability predicates using logical AND (All must match)
    /// @return predicate
    @SafeVarargs
    public static Predicate<ServiceMetadata> allOf(Predicate<ServiceMetadata>... predicates) {
        Predicate<ServiceMetadata> result = props -> true;
        for (Predicate<ServiceMetadata> p : predicates) {
            result = result.and(p);
        }
        return result;
    }

    /// Combines multiple capability predicates using logical OR (At least one must match)
    @SafeVarargs
    public static Predicate<ServiceMetadata> anyOf(Predicate<ServiceMetadata>... predicates) {
        Predicate<ServiceMetadata> result = props -> false;
        for (Predicate<ServiceMetadata> p : predicates) {
            result = result.or(p);
        }
        return result;
    }
}