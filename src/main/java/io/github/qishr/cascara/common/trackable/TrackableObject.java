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

package io.github.qishr.cascara.common.trackable;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.trackable.diagnostic.TrackingDiagnosticCode;
import io.github.qishr.cascara.common.trackable.diagnostic.TrackingException;
import io.github.qishr.cascara.common.trackable.property.TrackableProperty;

@Experimental
public class TrackableObject extends AbstractTrackable implements TrackableTabularData {
    // DO NOT use GlobalReporter in this class. Since this class is used in the
    // logger, it would get into an infinite loop.

    private final Map<String,Trackable> map = new HashMap<>();

    private final Map<String, Object> userData = new HashMap<>();

    private boolean additionalPropertiesAllowed;

    public TrackableObject(boolean additionalPropertiesAllowed) {
        this.additionalPropertiesAllowed = additionalPropertiesAllowed;
    }

    public TrackableObject() {

    }

    protected boolean additionalPropertiesAllowed() {
        return additionalPropertiesAllowed;
    }

    public void setAdditionalPropertiesAllowed(boolean b) {
        additionalPropertiesAllowed = b;
    }

    //
    // TrackableTabularData Implementation
    //

    @Override
    public final Map<String,Object> getValuesMap() {
        Map<String,Object> values = new HashMap<>();
        for (Map.Entry<String,Trackable> entry : map.entrySet()) {

            if (entry.getValue() instanceof TrackableArray list) {
                values.put(entry.getKey(), list);
            } else if (entry.getValue() instanceof TrackableProperty property) {
                values.put(entry.getKey(), property.getValue());
            } else {
                warn("getValuesMap: non-TrackableProperty");
            }

        }
        return values;
    }

    @Override
    public Object[] getValues() {
        Object[] r = new Object[map.size()];
        int i = 0;
        for (Trackable trackable : map.values()) {

            if (trackable instanceof TrackableArray) {
                // For TrackbleList, we simply return it
                r[i] = trackable;
            } else if (trackable instanceof TrackableProperty property) {
                // For TrackableProperty we extract the value
                r[i] = property.getValue();
            } else {
                // For anything else, we simply return it
                r[i] = trackable;
                warn("getValues: non-TrackableProperty");
            }

            i++;
        }
        return r;
    }

    @Override
    public final Trackable[] getTrackables() {
        Trackable[] r = new Trackable[map.size()];
        int i = 0;
        for (Trackable trackable : map.values()) {
            r[i++] = trackable;
        }
        return r;
    }

    @Override
    public final Map<String,Trackable> getTrackablesMap() {
        return map;
    }

    @Override
    public final Trackable getTrackable(String key) {
        if (key.equals("self")) {
            return this;
        }
        return map.get(key);
    }

    //
    // Values
    //

    public final TrackableArray<?> getTrackableArray(String key) {
        if (map.get(key) instanceof TrackableArray list) {
            return list;
        }
        return null;
    }

    // @SuppressWarnings("rawtypes") // Needed by getPaletteColors in ThemeModule
    public final AbstractTrackable getObjectProperty(String key) {
        if (map.get(key) instanceof AbstractTrackable property) {
            return property;
        }
        return null;
    }

    public Map<String,TrackableProperty<?>> getDataContext() {
        Map<String,TrackableProperty<?>> contextMap = new HashMap<>();
        for (Entry<String,Trackable> entry : map.entrySet()) {
            if (entry.getValue() instanceof TrackableProperty<?> prop) {
                contextMap.put(entry.getKey(), prop);
            }
        }
        return contextMap;
    }

    public Object get(String key) {
        if (key.equals("self")) {
            return this;
        }
        return internalGet(key);
    }

    private Object internalGet(String key) {
        Trackable trackable = getTrackable(key);

        if (trackable instanceof TrackableArray list) {
            return list;
        } else if (trackable instanceof TrackableProperty property) {
            return property.getValue();
        } else {
            warn("internalGet: non-TrackableProperty");
            return trackable;
        }

    }

    public final Boolean getBoolean(String key) {
        Object value = internalGet(key);
        return value == null ? null : (Boolean)value;
    }

    public final Integer getInteger(String key) {
        Object value = internalGet(key);
        return value == null ? null : (Integer)value;
    }

    public final Long getLong(String key) {
        Object value = internalGet(key);
        return value == null ? null : (Long)value;
    }

    public final Path getPath(String key) {
        Object value = internalGet(key);
        return value == null ? null : Path.of(value.toString());
    }

    public final String getString(String key) {
        Object value = internalGet(key);
        return value == null ? null : value.toString();
    }

    public final URI getUri(String key) {
        Object value = internalGet(key);
        return value == null ? null : (URI)value;
    }

    @SuppressWarnings("unchecked")
    public void set(String key, Object value) {
        Trackable trackable = map.get(key);

        if (trackable == null && additionalPropertiesAllowed) {
            // TODO: create and add to map
        }

        if(trackable instanceof TrackableArray<?> list) {
            if (value instanceof Collection collection) {
                list.setAll(collection);
            } else {
                // TODO: Exception
                warn("set: unexpected type: " + trackable);
            }
        } else if (trackable instanceof TrackableProperty property) {
            property.setValue(value);
        } else {
            throw new TrackingException(TrackingDiagnosticCode.PROPERTY_NOT_RECOGNIZED, key);
        }

        invalidate();
    }

    public void putUserData(String key, Object value) { userData.put(key, value); }
    public Object getUserData(String key) { return userData.get(key); }

    private void warn(String string) {
        System.out.println(string);
    }
}
