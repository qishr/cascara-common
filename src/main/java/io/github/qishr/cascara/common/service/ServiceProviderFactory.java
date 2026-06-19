package io.github.qishr.cascara.common.service;

import java.util.function.Predicate;

import io.github.qishr.cascara.common.lang.processor.Parser;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;
import io.github.qishr.cascara.common.util.Properties;

public class ServiceProviderFactory {
    private final ServiceProviderLayer layer;

    public ServiceProviderFactory() {
        this(null);
    }

    public ServiceProviderFactory(ServiceProviderLayer layer) {
        this.layer = layer == null ? ServiceProviderLayer.getRootLayer() : layer;
    }

    public Parser<?, ?> createParser(String contentType) throws ServiceException {
        return createServiceProvider(
            Parser.class,
            CapabilityQueries.hasExactValue("contentType", contentType)
        );
    }

    public TypeDescriptor<?> createTypeDescriptor(Class<?> jvmType) throws ServiceException {
        return createServiceProvider(
            TypeDescriptor.class,
            CapabilityQueries.allOf(
                CapabilityQueries.supportsJvmType(jvmType)
            )
        );
    }

    //
    //
    //

    protected <T extends ServiceProvider> T createServiceProvider(Class<T> serviceType, Predicate<Properties> capabilityPredicate) {
        ServiceMetadata metadata = layer.findProvider(
            serviceType,
            capabilityPredicate
        );
        if (metadata != null) {
            return ServiceProviderLayer.loadProvider(serviceType, metadata);
        }
        return null;
    }

}
