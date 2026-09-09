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

package io.github.qishr.cascara.common.trackable.property;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;
import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.trackable.Trackable;
import io.github.qishr.cascara.common.trackable.tracker.InvalidationTracker;

@Experimental
public class TrackableProperty<T> extends Property<T> implements Trackable {
    private Object owner;
    private String name;
    private T value;

    private PrimitiveType primitiveType;
    private String mediaType;
    private boolean isDeclaredProperty;

    private final List<InvalidationTracker> listeners = new ArrayList<>();

    private InvalidationTracker listener = new InvalidationTracker() {
		@Override
		public void invalidated(Trackable observable) {
            invalidate();
		}
    };

    public TrackableProperty() {
    }

    public TrackableProperty(String name) {
        this(null, name, null);
    }

    // TODO: The 2-parameter constructor should be name/value to match Property.
    public TrackableProperty(Object owner, String name) {
        this(owner, name, null);
    }

    public TrackableProperty(Object owner, String name, T value) {
        this.owner = owner;
        this.name = name;
        this.value = value;
    }

    public TrackableProperty(PrimitiveType schemaType, String mediaType, boolean isDeclaredProperty) {
        this.primitiveType = schemaType;
        this.mediaType = mediaType;
        this.isDeclaredProperty = isDeclaredProperty;
    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object o) {
        owner = o;
    }

    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public void setPrimitiveType(PrimitiveType type) {
        primitiveType = type;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String type) {
        mediaType = type;
    }

    public boolean isDeclaredProperty() {
        return isDeclaredProperty;
    }

    public void setDeclaredProperty(boolean b) {
        isDeclaredProperty = b;
    }

    // TODO: remove generics from this class. rename base get to getValue
    public T getValue() {
        return value;
    }

    public void setValue(T v) {
        T oldValue = value;
        value = v;

        if (oldValue instanceof Trackable t) {
            t.removeTracker(listener);
        }

        if (v instanceof Trackable t) {
            t.addTracker(listener);
        } else {
            // TODO:
            System.out.println("Unhandled type: " + (
                v == null
                    ? "null"
                    : v.getClass().getName()
            ));
        }

        if (oldValue == null && v == null) {
            return;
        }

        if (oldValue == null || v == null) {
            invalidate();
        }
        else if (!oldValue.equals(v)) {
            invalidate();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
        invalidate();
    }

    //
    //
    //

    protected void invalidate() {
        for (InvalidationTracker listener : listeners) {
            listener.invalidated(this);
        }
    }

    @Override
    public void addTracker(InvalidationTracker listener) {
        if (listeners.contains(listener)) return;
        listeners.add(listener);
    }

    @Override
    public void removeTracker(InvalidationTracker listener) {
        listeners.remove(listener);
    }


}
