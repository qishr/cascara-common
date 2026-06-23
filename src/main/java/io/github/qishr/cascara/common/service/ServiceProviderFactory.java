package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.lang.processor.Parser;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;

public class ServiceProviderFactory extends AbstractServiceProviderFactory {

    public ServiceProviderFactory() {
        super();
    }

    public ServiceProviderFactory(ServiceProviderLayer layer) {
        super(layer);
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
}
