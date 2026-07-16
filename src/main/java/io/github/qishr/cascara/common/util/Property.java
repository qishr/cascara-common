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

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.data.TableData;

public class Property implements TableData {
    Kind kind = Kind.STRING;
    String name;
    String value = null;

    public Property(String k) {
        name = k;
    }

    public Property(String k, String v) {
        name = k;
        value = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String k) {
        name = k;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String v) {
        value = v;
        kind = Kind.STRING;
    }

    public void setValue(boolean v) {
        value = v ? "true" : "false";
        kind = Kind.BOOLEAN;
    }

    public void setValue(int v) {
        value = Long.toString(v);
        kind = Kind.NUMBER;
    }

    public void setValue(double v) {
        value = Double.toString(v);
        kind = Kind.NUMBER;
    }

    public double asDouble() {
        return asDouble(-1);
    }

    public double asDouble(double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int asInt() {
        return asInt(-1);
    }

    public long asLong() {
        return asLong(-1);
    }

    public int asInt(int defaultValue) {
        return (int) asLong(defaultValue);
    }

    public long asLong(int defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
        }
        try {
            return Double.valueOf(value).longValue();
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public boolean asBoolean() {
        return asBoolean(false);
    }

    public boolean asBoolean(boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return (value.equalsIgnoreCase("true") ||
             value.equalsIgnoreCase("yes"));
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    public enum Kind {
        STRING,
        NUMBER,
        BOOLEAN
    }

	@Override
	public Object[] getValues() {
        return new Object[]{name, value};
	}

	@Override
	public Map<String, Object> getValuesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name); // TODO: Make these constants
        map.put("value", value);
        return map;
	}

	@Override
	public Object get(String key) {
        if (key == null) return null;
        if (key.equals("name")) {
            return this.name;
        }
        if (key.equals("value")) {
            return this.value;
        }
        return null;
	}
}
