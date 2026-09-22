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

package io.github.qishr.cascara.common.service.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.common.trackable.TrackableArray;

public class SPLUtils {

    /// Returns an instance of a specific service provider.
    public static <T> T loadProvider(Class<T> serviceType, ServiceMetadata metadata) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }
        Class<? extends ServiceProvider> clazz = metadata.getType();
        return serviceType.cast(getInstance(clazz, metadata));
    }

    /// Returns an instance of a service provider.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T loadDefault(Class<T> serviceType) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }

        SPLRoot rootLayer = (SPLRoot) SPLRoot.instance();
        List<ServiceMetadata> providers = rootLayer.findAllProviders((Class) serviceType);
        if (providers.isEmpty()) {
            throw new ServiceException(ServiceDiagnosticCode.NO_PROVIDER_REGISTERED, serviceType.getSimpleName());
        }

        String preferredProviderName = rootLayer.getPreferredProviderClassName(serviceType);
        ServiceMetadata serviceMeta = null;

        if (preferredProviderName != null) {
            for(ServiceMetadata candidate : providers) {
                if (candidate.getTypeName().equals(preferredProviderName)) {
                    serviceMeta = candidate;
                    break;
                }
            }
        }

        if (serviceMeta == null) {
            serviceMeta = providers.getFirst();
        }

        Class<T> clazz = (Class<T>) serviceMeta.getType();
        return getInstance(clazz, serviceMeta);
    }

    /// Returns an instantiated service provider.
    /// If the provider is a neo-singleton, the singleton instance is returned.
    /// Otherwise, a new instance of the provider is returned.
    /// @param providerClass The class of the provider to instantiate.
    public static <T> T getInstance(Class<T> providerClass, ServiceMetadata serviceMeta) {
        if (!ServiceProvider.class.isAssignableFrom(providerClass)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, providerClass);
        }

        if (serviceMeta.isSingleton()) {
            SPLBranch layer = (SPLBranch) serviceMeta.getLayer();
            return layer.getOrCreateSingleton(
                serviceMeta,
                () -> instantiate(providerClass)
            );
        } else {
            return instantiate(providerClass);
        }
    }

    /// Instantiates a service provider
    /// @param providerClass The class of the provider to instantiate.
    @SuppressWarnings("unchecked")
    public static <T> T instantiate(Class<T> providerClass) {
        if (!ServiceProvider.class.isAssignableFrom(providerClass)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, providerClass);
        }
        try {
            Constructor<?> constructor = providerClass.getDeclaredConstructor();
            if (constructor == null) {
                throw new ServiceException(ServiceDiagnosticCode.NOARGS_CONSTRUCTOR_REQUIRED, providerClass.getName());
            } else {
                ServiceProvider instance = (ServiceProvider) constructor.newInstance();
                return (T)instance;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new ServiceException(e, ServiceDiagnosticCode.FAILED_TO_INSTANTIATE_CLASS, providerClass.getName(), e.getMessage());
        }
    }

    //
    // Provider Visibility Graph
    //

    public static void recomputeAllVisibleProviders(SPLRoot rootLayer) {
        Set<SPLBranch> allLayers = rootLayer.allLayers();

        // Step 1: compute visibility sets for all layers
        for (SPLBranch layer : allLayers) {
            layer.visibleLayers = computeVisibleLayers(layer);
        }

        // Step 2: merge providers for each layer
        for (SPLBranch layer : allLayers) {
            Set<ServiceMetadata> newProviders = mergeDeclaredProviders(layer.visibleLayers);
            diffAndApply(layer.visibleProviders, newProviders);
        }
    }

    public static Set<SPLBranch> computeVisibleLayers(SPLBranch layer) {
        Set<SPLBranch> result = new HashSet<>();

        // Always include self
        result.add(layer);

        Set<SPLBranch> ancestors = layer.ancestors();

        for (SPLBranch ancestor : ancestors) {
            result.add(ancestor);
            for (SPLBranch descendant : ancestor.publicDescendants()) {
                result.add(descendant);
            }
        }

        for (SPLBranch ancestor : ancestors) {
            for (SPLBranch sibling : ancestor.publicSiblings()) {
                result.add(sibling);
                for (SPLBranch descendant : sibling.publicDescendants()) {
                    result.add(descendant);
                }
            }
        }

        for (SPLBranch x : result) {
            if (!x.isPublic() && !(ancestors.contains(x) || x == layer)) {
                result.remove(x);
            }
        }

        return result;
    }


    public static Set<ServiceMetadata> mergeDeclaredProviders(Set<SPLBranch> visibleLayers) {
        Set<ServiceMetadata> merged = new HashSet<>();

        for (SPLBranch visible : visibleLayers) {
            for (ServiceMetadata provider : visible.providersByFqcn.values()) {
                merged.add(provider);
            }
        }

        return merged;

    }

    public static void diffAndApply(TrackableArray<ServiceMetadata> oldArray, Set<ServiceMetadata> newList) {
        Set<ServiceMetadata> removed = subtract(oldArray, newList);
        Set<ServiceMetadata> added = subtract(newList, oldArray);

        for (ServiceMetadata provider : removed) {
            oldArray.remove(provider);
        }

        for (ServiceMetadata provider : added) {
            oldArray.add(provider);
        }
    }

    public static Set<ServiceMetadata> subtract(TrackableArray<ServiceMetadata> from, Set<ServiceMetadata> items) {
        Set<ServiceMetadata> result = new HashSet<>();
        for (ServiceMetadata item : from) {
            if (!items.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static Set<ServiceMetadata> subtract(Set<ServiceMetadata> from,  TrackableArray<ServiceMetadata> items) {
        Set<ServiceMetadata> result = new HashSet<>();
        for (ServiceMetadata item : from) {
            if (!items.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
