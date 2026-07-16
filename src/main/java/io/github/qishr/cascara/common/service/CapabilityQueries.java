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