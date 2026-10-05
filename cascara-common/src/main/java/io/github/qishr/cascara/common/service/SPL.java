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
import io.github.qishr.cascara.common.service.internal.SPLRoot;
import io.github.qishr.cascara.common.service.internal.SPLUtils;
import io.github.qishr.cascara.common.trackable.TrackableArray;

public interface SPL extends TreeNode<SPL> {
    public static final String SERVICE_NAME = "serviceName";
    public static final String JAR_PATH = "jarPath";
    public static final String PROVIDER_NAME = "providerName";
    public static final String MODULE_NAME = "moduleName";
    public static final String MODULE_VERSION = "moduleVersion";
    public static final String JVM_TYPE = "jvmType";
    public static final String TITLE = "title";
    public static final String CONTENT_TYPE = "contentType";

    /// Retrieves the root Service Provider Layer.
    static SPL getRoot() {
        return SPLRoot.instance();
    }

    static boolean isBooting() {
        return SPLRoot.isBooting();
    }

    /// Returns an instance of a specific service provider.
    static <T> T load(Class<T> serviceType, ServiceMetadata metadata) {
        return SPLUtils.loadProvider(serviceType, metadata);
    }

    /// Returns an instance of a service provider.
    static <T> T load(Class<T> serviceType) {
        return SPLUtils.loadDefault(serviceType);
    }

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

    /// Creates a service provider layer with the specified name.
    SPL create(String name);

    /// Creates private a service provider layer with the specified name.
    /// Providers in a private layer can only be found by searching the
    /// private layer or its descendants.
    SPL createPrivate(String name);

    void remove(String name);

    //
    // Find provider metadata from all layers starting at this one
    //

    /// Returns a list of all known service types.
    Set<Class<ServiceProvider>> findServiceTypes();

    /// Returns a list of metadata for all known service types.
    Set<ServiceMetadata> findServices();

    /// Retrieves metadata of the nearest known provider of the specified service type.
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType);

    /// Retrieves metadata of the nearest known provider whose capabilities satisfy the given predicate.
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    /// Retrieves metadata of all known providers of the specified service type.
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType);

    /// Retrieves metadata of all known providers whose capabilities satisfy the given predicate.
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    //
    // Get provider metadata from this specific layer
    //

    /// Retrieves metadata of the specified provider if it exists in this layer, otherwise `null` is returne.
    ServiceMetadata getProvider(String providerName);

    /// Retrieves metadata of providers in this layer.
    List<ServiceMetadata> getProviders();

    /// Retrieves metadata of providers of the specified service type in this layer.
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType);

    /// Retrieves metadata of providers in this layer whose capabilities satisfy the given predicate.
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    boolean hasProvider(String name);

    //
    // Provider registration in this specific layer
    //

    void registerClass(Class<?> clazz);
    void registerJar(Path jarPath);
}
