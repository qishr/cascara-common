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
import io.github.qishr.cascara.common.i18n.exception.LocalizableClassCastException;
import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.trackable.Trackable;
import io.github.qishr.cascara.common.trackable.TrackableValue;
import io.github.qishr.cascara.common.trackable.tracker.InvalidationTracker;
import io.github.qishr.cascara.common.trackable.tracker.ValueTracker;
import io.github.qishr.cascara.common.type.PrimitiveType;

@Experimental
public class TrackableProperty<T> extends Property<T> implements TrackableValue<T> {
    private Object owner;
    private String mediaType;
    private boolean isDeclaredProperty;
    private final List<ValueTracker<T>> trackers = new ArrayList<>();
    private final List<InvalidationTracker> invalidationTrackers = new ArrayList<>();

    private InvalidationTracker invalidationTracker = new InvalidationTracker() {
		@Override
		public void invalidated(Trackable observable) {
            invalidate(getValue(), getValue());
		}
    };

    public TrackableProperty() {
    }

    public TrackableProperty(String name) {
        this(null, name, null);
    }

    public TrackableProperty(String name, T value) {
        super(name, value);
    }

    public TrackableProperty(Object owner, String name, T value) {
        super(name, value);
        this.owner = owner;
    }

    public TrackableProperty(PrimitiveType primitiveType, String mediaType, boolean isDeclaredProperty) {
        super.setPrimitiveType(primitiveType);
        this.mediaType = mediaType;
        this.isDeclaredProperty = isDeclaredProperty;
    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object o) {
        owner = o;
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

    @Override
    public void setValue(T v) {
        T oldValue = super.getValue();
        try {
            super.setValue(v);

            if (oldValue instanceof Trackable t) {
                t.removeInvalidationTracker(invalidationTracker);
            }

            if (v instanceof Trackable t) {
                t.addInvalidationTracker(invalidationTracker);
            } else {
                // TODO: This seems mad. To be removed soon.
                // System.out.println("Unhandled type: " + (
                //     v == null
                //         ? "null"
                //         : v.getClass().getName()
                // ));
            }

            if (oldValue == null && v == null) {
                return;
            }

            if (oldValue == null || v == null) {
                invalidate(oldValue, v);
            }
            else if (!oldValue.equals(v)) {
                invalidate(oldValue, v);
            }
        } catch (java.lang.ClassCastException e) {
            // TODO: LocalizableRuntimeException.forException(e)
            // DiagnosticMessage dm = DiagnosticMessage.forException(e);
            throw new LocalizableClassCastException(e);
        }
    }

    public void setName(String name) {
        super.setName(name);
        invalidate(getValue(), getValue());
    }

    //
    //
    //

    protected void invalidate(T oldValue, T newValue) {
        for (ValueTracker<T> tracker : trackers) {

            // TODO: oldValue, newValue

            tracker.invalidated(oldValue, newValue);
        }
        for (InvalidationTracker tracker : invalidationTrackers) {
            tracker.invalidated(this);
        }
    }

    @Override
    public void addTracker(ValueTracker<T> tracker) {
        if (trackers.contains(tracker)) return;
        trackers.add(tracker);
    }

    @Override
    public void removeTracker(ValueTracker<T> tracker) {
        trackers.remove(tracker);
    }

    @Override
    public void addInvalidationTracker(InvalidationTracker tracker) {
        if (invalidationTrackers.contains(tracker)) {
            return;
        }
        invalidationTrackers.add(tracker);
    }

    @Override
    public void removeInvalidationTracker(InvalidationTracker tracker) {
        invalidationTrackers.remove(tracker);
    }
}
