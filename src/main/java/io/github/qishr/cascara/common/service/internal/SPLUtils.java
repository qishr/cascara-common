package io.github.qishr.cascara.common.service.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProvider;

public class SPLUtils {

    public static <T> T loadProvider(Class<T> serviceType, ServiceMetadata metadata) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }
        Class<? extends ServiceProvider> clazz = metadata.getType();
        return serviceType.cast(instantiateProvider(clazz));
    }

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

        if (serviceMeta.isSingleton()) {
            return rootLayer.getOrCreateSingleton(
                serviceMeta,
                () -> instantiateProvider(clazz)
            );
        }

        return instantiateProvider(clazz);
    }

    /// Instantiates a service provider
    /// @param providerClass The class of the provider to instantiate.
    @SuppressWarnings("unchecked")
    public static <T> T instantiateProvider(Class<T> providerClass) {
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
}
