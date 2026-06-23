package io.github.qishr.cascara.common.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import io.github.qishr.cascara.common.util.Properties;

public class AbstractServiceProviderFactory {
    private final ServiceProviderLayer layer;
    private Map<String,Map<Predicate<Properties>,ServiceMetadata>> cache = new HashMap<>();

    public AbstractServiceProviderFactory() {
        this(null);
    }

    public AbstractServiceProviderFactory(ServiceProviderLayer layer) {
        this.layer = layer == null ? ServiceProviderLayer.getRootLayer() : layer;
    }

    protected <T extends ServiceProvider> T createServiceProvider(Class<T> serviceType, Predicate<Properties> capabilityPredicate) {
        String serviceName = serviceType.getName();

        Map<Predicate<Properties>,ServiceMetadata> providerMap = cache.get(serviceType.getName());
        if (providerMap == null) {
            providerMap = new HashMap<>();
            cache.put(serviceName, providerMap);
        }

        ServiceMetadata provider;
        if (providerMap.containsKey(capabilityPredicate)) {
            provider = providerMap.get(capabilityPredicate);
        } else {
            provider = layer.findProvider(
                serviceType,
                capabilityPredicate
            );
            providerMap.put(capabilityPredicate, provider);
        }

        if (provider == null) return null;

        return ServiceProviderLayer.loadProvider(serviceType, provider);
    }
}
