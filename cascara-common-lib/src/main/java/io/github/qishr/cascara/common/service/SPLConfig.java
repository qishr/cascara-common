package io.github.qishr.cascara.common.service;

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.util.Config;

@ConfigNamespace("cascara.spl")
public class SPLConfig extends Config {
    @ConfigSetting()
    private boolean discover = true;

    @ConfigSetting()
    private boolean bootErrorsToConsole = false;

    @ConfigSetting()
    private boolean verify = false;

    @ConfigSetting(name = "policy.check-expiry")
    private boolean policyCheckExpiry = false;

    @ConfigSetting(name = "policy.require-signed")
    private boolean policyRequireSigned = false;

    @ConfigSetting(name = "policy.require-signed-classes")
    private boolean policyRequireSignedClasses = false;

    @ConfigSetting(name = "provider")
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
