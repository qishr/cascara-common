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

import java.util.*;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.UnimplementedMethodException;
import io.github.qishr.cascara.common.trackable.tracker.ArrayChangeTracker;
import io.github.qishr.cascara.common.trackable.tracker.ArrayTracker;

@Experimental
public class TrackableArray<E> extends AbstractTrackable implements List<E>, RandomAccess {

    private final ArrayList<E> backing = new ArrayList<>();
    private final ArrayList<ArrayTracker<E>> arrayListeners = new ArrayList<>();

    public void addArrayListener(ArrayTracker<E> l) {
        if (!arrayListeners.contains(l)) arrayListeners.add(l);
    }

    public void removeArrayListener(ArrayTracker<E> l) {
        arrayListeners.remove(l);
    }

    private void fireChange(ArrayChangeTracker<E> change) {
        for (ArrayTracker<E> l : arrayListeners)
            l.onChanged(this, change);
        invalidate(); // inherited from AbstractTrackable
    }

    //
    // Query Operations
    //
    @Override public int size() { return backing.size(); }
    @Override public boolean isEmpty() { return backing.isEmpty(); }
    @Override public boolean contains(Object o) { return backing.contains(o); }
    @Override public Object[] toArray() { return backing.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return backing.toArray(a); }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> it = backing.iterator();
            private int index = -1;

            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public E next() {
                index++;
                return it.next();
            }
            @Override public void remove() {
                E old = backing.get(index);
                it.remove();
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, index, old, null));
            }
        };
    }

    //
    // Modification Operations
    //
    @Override
    public boolean add(E e) {
        int idx = backing.size();
        boolean r = backing.add(e);
        if (r) fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.ADD, idx, null, e));
        return r;
    }

    @Override
    public void add(int index, E element) {
        backing.add(index, element);
        fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.ADD, index, null, element));
    }

    @Override
    public boolean remove(Object o) {
        int idx = backing.indexOf(o);
        if (idx < 0) return false;
        E old = backing.remove(idx);
        fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, idx, old, null));
        return true;
    }

    @Override
    public E remove(int index) {
        E old = backing.remove(index);
        fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, index, old, null));
        return old;
    }

    @Override
    public E set(int index, E element) {
        E old = backing.set(index, element);
        fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.SET, index, old, element));
        return old;
    }

    //
    // Bulk Modification Operations
    //
    @Override
    public boolean addAll(Collection<? extends E> c) {
        int start = backing.size();
        boolean r = backing.addAll(c);
        if (r) fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.ADD, start, null, null));
        return r;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean r = backing.addAll(index, c);
        if (r) fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.ADD, index, null, null));
        return r;
    }

    public void setAll(Collection<? extends E> c) {
        backing.clear();
        backing.addAll(c);
        fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.SET, 0, null, null));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean r = backing.removeAll(c);
        if (r) fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, -1, null, null));
        return r;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean r = backing.retainAll(c);
        if (r) fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, -1, null, null));
        return r;
    }

    @Override
    public void clear() {
        if (!backing.isEmpty()) {
            backing.clear();
            fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.CLEAR, -1, null, null));
        }
    }

    //
    // Positional Access
    //
    @Override public E get(int index) { return backing.get(index); }
    @Override public int indexOf(Object o) { return backing.indexOf(o); }
    @Override public int lastIndexOf(Object o) { return backing.lastIndexOf(o); }

    //
    // List Iterators
    //
    @Override
    public ListIterator<E> listIterator() { return listIterator(0); }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListIterator<E>() {
            private final ListIterator<E> it = backing.listIterator(index);

            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public E next() { return it.next(); }
            @Override public boolean hasPrevious() { return it.hasPrevious(); }
            @Override public E previous() { return it.previous(); }
            @Override public int nextIndex() { return it.nextIndex(); }
            @Override public int previousIndex() { return it.previousIndex(); }

            @Override
            public void remove() {
                int idx = previousIndex();
                E old = backing.get(idx);
                it.remove();
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, idx, old, null));
            }

            @Override
            public void set(E e) {
                int idx = previousIndex();
                E old = backing.get(idx);
                it.set(e);
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.SET, idx, old, e));
            }

            @Override
            public void add(E e) {
                int idx = nextIndex();
                it.add(e);
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.ADD, idx, null, e));
            }
        };
    }

    //
    // SubList view
    //
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new AbstractList<E>() {
            @Override public E get(int index) { return backing.get(fromIndex + index); }
            @Override public int size() { return toIndex - fromIndex; }

            @Override
            public E set(int index, E element) {
                int real = fromIndex + index;
                E old = backing.set(real, element);
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.SET, real, old, element));
                return old;
            }

            @Override
            public void add(int index, E element) {
                int real = fromIndex + index;
                backing.add(real, element);
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.ADD, real, null, element));
            }

            @Override
            public E remove(int index) {
                int real = fromIndex + index;
                E old = backing.remove(real);
                fireChange(new ArrayChangeTracker<>(ArrayChangeTracker.Type.REMOVE, real, old, null));
                return old;
            }
        };
    }

    //
    // equals/hashCode
    //
    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TrackableArray<?> other)) return false;
        return backing.equals(other.backing);
    }

    @Override public int hashCode() { return backing.hashCode(); }

    @Override
    public boolean containsAll(Collection<?> c) {
        // TODO Auto-generated method stub
        throw new UnimplementedMethodException();
    }
}
