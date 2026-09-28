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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.github.qishr.cascara.common.data.TreeNode;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.service.internal.SPLRoot;
import io.github.qishr.cascara.common.service.internal.SPLUtils;
import io.github.qishr.cascara.common.trackable.TrackableArray;

public interface SPL extends TreeNode<SPL> {
    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured with a specified Reporter.
    /// This reporter is used for non-fatal error and warning reporting.
    static SPL getRoot(Reporter reporter) {
        return SPLRoot.instance(reporter);
    }

    static SPL getRoot() {
        return SPLRoot.instance();
    }

    static <T> T load(Class<T> serviceType, ServiceMetadata metadata) {
        return SPLUtils.loadProvider(serviceType, metadata);
    }

    static <T> T load(Class<T> serviceType) {
        return SPLUtils.loadDefault(serviceType);
    }

    void setReporter(Reporter reporter);

    //
    // Layer info, hierarchy, creation and deletion
    //

    String getName();
    TrackableArray<String> getModules();
    TrackableArray<ServiceMetadata> getDeclaredProviders();
    TrackableArray<ServiceMetadata> getVisibleProviders();
    boolean isPublic();

    SPL getParent();
    List<SPL> getChildren();
    SPL getChild(String name);
    boolean hasChild(String name);

    SPL create(String name);
    SPL createPrivate(String name);
    void remove(String name);

    //
    // Find provider metadata from all layers starting at this one
    //

    Set<Class<ServiceProvider>> findServiceTypes();
    Set<ServiceMetadata> findServices();
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType);
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType);
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    //
    // Get provider metadata from this specific layer
    //

    ServiceMetadata getProvider(String providerName);
    List<ServiceMetadata> getProviders();
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType);
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);
    boolean hasProvider(String name);

    //
    // Provider registration in this specific layer
    //

    void registerModule(Module module);
    void registerClass(Class<?> clazz);
    void registerJar(Path jarPath);
}
