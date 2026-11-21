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

package io.github.qishr.cascara.common.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.ConfigExtends;
import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Config;

@ConfigExtends(Cascara.class)
@ConfigNamespace("cascara.type")
public class TypeConfig extends Config {
    @ConfigSetting()
    private Map<String,String> mapping = new HashMap<>();

    // TODO: Fix Config so that this can be private
    public TypeConfig() {
        // Core Java Collections & Interfaces Defaults
        addMapping(java.util.List.class, java.util.ArrayList.class);
        addMapping(java.util.Set.class, java.util.HashSet.class);
        addMapping(java.util.Map.class, java.util.LinkedHashMap.class);
        addMapping(java.util.Queue.class, java.util.ArrayDeque.class);
        addMapping(java.util.Deque.class, java.util.ArrayDeque.class);
        addMapping(java.util.AbstractQueue.class, java.util.PriorityQueue.class);
        addMapping(java.util.SortedSet.class, java.util.TreeSet.class);
        addMapping(java.util.NavigableSet.class, java.util.TreeSet.class);
        addMapping(java.util.SortedMap.class, java.util.TreeMap.class);
        addMapping(java.util.NavigableMap.class, java.util.TreeMap.class);
        addMapping(java.util.Calendar.class, java.util.GregorianCalendar.class);
        addMapping(java.util.Dictionary.class, java.util.Hashtable.class);
        addMapping(java.util.SequencedCollection.class, ArrayList.class);
    }

    private void addMapping(Class<?> abstractClass, Class<?> concreteClass) {
        mapping.put(abstractClass.getCanonicalName(), concreteClass.getCanonicalName());
    }

    @NonNull
    public static TypeConfig get() {
        return get(TypeConfig.class);
    }

    /// Returns the mappings of abstract names to concrete names.
    @Nullable
    public Map<String,String> getMappings() {
        return mapping;
    }

    /// Returns the preferred concrete class name for an abstarct class or interface.
    @Nullable
    public String getMapping(String abstractName) {
        return mapping.get(abstractName);
    }
}
