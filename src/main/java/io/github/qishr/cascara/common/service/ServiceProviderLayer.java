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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.github.qishr.cascara.common.data.TreeNode;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.service.internal.SPLRoot;
import io.github.qishr.cascara.common.service.internal.SPLUtils;

public interface ServiceProviderLayer extends TreeNode<ServiceProviderLayer> {
    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured with a specified Reporter.
    /// This reporter is used for non-fatal error and warning reporting.
    static ServiceProviderRoot getRoot(Reporter reporter) {
        return SPLRoot.instance(reporter);
    }

    static ServiceProviderRoot getRoot() {
        return SPLRoot.instance();
    }

    static <T> T loadProvider(Class<T> serviceType, ServiceMetadata metadata) {
        return SPLUtils.loadProvider(serviceType, metadata);
    }

    static <T> T loadDefault(Class<T> serviceType) {
        return SPLUtils.loadDefault(serviceType);
    }

    /// Instantiates a service provider
    /// @param providerClass The class of the provider to instantiate.
    static <T> T instantiateProvider(Class<T> providerClass) {
        return SPLUtils.instantiateProvider(providerClass);
    }

    ServiceProviderLayer setReporter(Reporter reporter);

    //
    // Layer metadata, hierarchy, creation and deletion
    //

    String getName();
    Path getModulePath(String name);
    boolean isPublic();
    void setPublic(boolean v);

    ServiceProviderLayer getParent();
    List<ServiceProviderLayer> getChildren();
    ServiceProviderLayer getChild(String name);
    boolean hasChild(String name);

    ServiceProviderLayer create();
    ServiceProviderLayer create(String name);
    void remove(String name);

    //
    // Find in All Layers
    //

    Set<Class<ServiceProvider>> findServiceTypes();
    Set<ServiceMetadata> findServices();
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType);
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType);
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    //
    // Get from Specific Layer
    //

    ServiceMetadata getProvider(String providerName);
    Collection<ServiceMetadata> getProviders();
    Collection<ServiceMetadata> getProvidersByFqcn();
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType);
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);
    boolean hasProvider(String name);

    //
    // Provider Registration in Specific Layer
    //

    void registerModule(Module module);
    void registerClass(Class<?> clazz);
    void registerJar(Path jarPath);
}
