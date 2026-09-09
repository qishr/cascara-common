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

import java.util.HashMap;
import java.util.Map;


import io.github.qishr.cascara.common.data.TabularData;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;

public class Property<T> implements TabularData {
    private static final String TABULAR_NAME_FIELD = "name";
    private static final String TABULAR_VALUE_FIELD = "value";

    PrimitiveType type = PrimitiveType.ANY;
    String name;
    T value = null;

    public Property() {
    }

    public Property(String k) {
        name = k;
    }

    public Property(String k, T v) {
        name = k;
        value = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String k) {
        name = k;
    }

    public PrimitiveType getType() {
        return type;
    }

    public void setType(PrimitiveType kind) {
        this.type = kind;
    }

    public String asString() {
        return value == null
            ? null
            : value.toString();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T v) {
        value = v;
        type = PrimitiveType.of(v);
    }

    public double asDouble() {
        return asDouble(-1);
    }

    public double asDouble(double defaultValue) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(asString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int asInteger() {
        return asInteger(-1);
    }

    public long asLong() {
        return asLong(-1);
    }

    public int asInteger(int defaultValue) {
        return (int) asLong(defaultValue);
    }

    public long asLong(int defaultValue) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(asString());
        } catch (NumberFormatException e) {
        }
        try {
            return Double.valueOf(asString()).longValue();
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public boolean asBoolean() {
        return asBoolean(false);
    }

    public boolean asBoolean(boolean defaultValue) {
        if (value instanceof Boolean v) {
            return v;
        }
        String stringValue = asString();
        if (stringValue == null || stringValue.isBlank()) {
            return defaultValue;
        }
        return (stringValue.equalsIgnoreCase("true") ||
        stringValue.equalsIgnoreCase("yes"));
    }

    public boolean isEmpty() {
        return value == null || asString().isEmpty();
    }

    //
    // TabularData Implementation
    //

	@Override
	public Object[] getValues() {
        return new Object[]{name, value};
	}

	@Override
	public Map<String, Object> getValuesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TABULAR_NAME_FIELD, name);
        map.put(TABULAR_VALUE_FIELD, value);
        return map;
	}

	@Override
	public Object getValue(String key) {
        if (key == null) return null;
        if (key.equals(TABULAR_NAME_FIELD)) {
            return this.name;
        }
        if (key.equals(TABULAR_VALUE_FIELD)) {
            return this.value;
        }
        return null;
	}
}
