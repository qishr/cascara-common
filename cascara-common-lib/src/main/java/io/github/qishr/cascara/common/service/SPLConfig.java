package io.github.qishr.cascara.common.service;

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigProperty;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.util.Config;

@ConfigNamespace("cascara.spl")
public class SPLConfig extends Config {
    @ConfigProperty()
    private boolean discover = true;

    @ConfigProperty()
    private boolean bootErrorsToConsole = false;

    @ConfigProperty()
    private boolean verify = false;

    @ConfigProperty(name = "policy.check-expiry")
    private boolean policyCheckExpiry = false;

    @ConfigProperty(name = "policy.require-signed")
    private boolean policyRequireSigned = false;

    @ConfigProperty(name = "policy.require-signed-classes")
    private boolean policyRequireSignedClasses = false;

    @ConfigProperty(name = "provider")
    private Map<String,String> preferredProviders = new HashMap<>();

    @NonNull
    public static SPLConfig get() {
        return get(SPLConfig.class);
    }

    public boolean isDiscoveryEnabled() {
        return discover;
    }

    public boolean bootErrorsToConsole() {
        return bootErrorsToConsole;
    }

    public boolean isVerifyEnabled() {
        return verify;
    }

    public boolean policyCheckExpiry() {
        return policyCheckExpiry;
    }

    public boolean policyRequireSigned() {
        return policyRequireSigned;
    }

    public boolean policyRequireSignedClasses() {
        return policyRequireSignedClasses;
    }

    public String getPreferredProvider(String serviceName) {
        return preferredProviders.get(serviceName);
    }

    public Map<String,String> getPreferredProviders() {
        return preferredProviders;
    }
}
