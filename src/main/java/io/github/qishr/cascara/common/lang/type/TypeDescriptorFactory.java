package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.service.AbstractServiceProviderFactory;
import io.github.qishr.cascara.common.service.CapabilityQueries;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;

@Beta
public class TypeDescriptorFactory extends AbstractServiceProviderFactory {

    public TypeDescriptorFactory() {
        super();
    }

    public TypeDescriptorFactory(ServiceProviderLayer layer) {
        super(layer);
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
