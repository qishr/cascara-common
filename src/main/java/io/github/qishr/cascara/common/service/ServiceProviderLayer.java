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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.service.internal.SPLRoot;

public interface ServiceProviderLayer {
    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured with a specified Reporter.
    /// This reporter is used for non-fatal error and warning reporting.
    public static ServiceProviderRoot getRoot(Reporter reporter) {
        return SPLRoot.instance(reporter);
    }

    public static ServiceProviderRoot getRoot() {
        return SPLRoot.instance();
    }

    public static <T> T loadProvider(Class<T> serviceType, ServiceMetadata metadata) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }
        Class<? extends ServiceProvider> clazz = metadata.getType();
        return serviceType.cast(ServiceProviderLayer.loadProvider(clazz));
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadProvider(Class<T> providerClass) {
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

    // TODO: Don't just pick the first one, pick one that's declared in a Cascara module
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T loadDefault(Class<T> serviceType) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }
        List<ServiceMetadata> providers = getRoot().findAllProviders((Class)serviceType);
        if (providers.isEmpty()) {
            throw new ServiceException(ServiceDiagnosticCode.NO_PROVIDER_REGISTERED, serviceType.getSimpleName());
        }
        ServiceMetadata meta = providers.getFirst();
        Class<T> clazz = (Class<T> )meta.getType();
        return ServiceProviderLayer.loadProvider(clazz);
    }

    ServiceProviderLayer setReporter(Reporter reporter);

    String getName();
    boolean isPublic();
    void setPublic(boolean v);

    ServiceProviderLayer getParent();
    Collection<ServiceProviderLayer> getChildren();
    ServiceProviderLayer getChild(String name);
    boolean hasChild(String name);

    Path getModulePath(String name);

    boolean hasProvider(String name);
    ServiceMetadata getProvider(String providerName);
    Collection<ServiceMetadata> getProviders();
    Collection<ServiceMetadata> getProvidersByFqcn();
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType);
    List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    Set<Class<ServiceProvider>> findServiceTypes();
    Set<ServiceMetadata> findServices();
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType);
    ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType);
    List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate);

    ServiceProviderLayer create();
    ServiceProviderLayer create(String name);

    void remove(String layerName);
    void registerModule(Module module);
    void registerClass(Class<?> type);
    void registerJar(Path jarPath);
}
