package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.lang.processor.AstParser;
import io.github.qishr.cascara.common.lang.type.TypeDescriptor;

@Beta
public class ServiceProviderFactory extends AbstractServiceProviderFactory {

    public ServiceProviderFactory() {
        super();
    }

    public ServiceProviderFactory(ServiceProviderLayer layer) {
        super(layer);
    }

    public AstParser<?, ?> createParser(String contentType) throws ServiceException {
        return createServiceProvider(
            AstParser.class,
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
