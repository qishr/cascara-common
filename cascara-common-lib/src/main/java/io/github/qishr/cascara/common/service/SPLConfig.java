package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigProperty;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.util.AbstractConfig;

@ConfigNamespace("cascara.spl")
public class SPLConfig extends AbstractConfig {
    @ConfigProperty()
    private boolean discover;

    @ConfigProperty()
    private boolean verify;

    @ConfigProperty(name = "policy.check-expiry")
    private boolean policyCheckExpiry;

    @ConfigProperty(name = "policy.require-signed")
    private boolean policyRequireSigned;

    @ConfigProperty(name = "policy.require-signed-classes")
    private boolean policyRequireSignedClasses;

    @NonNull
    public static SPLConfig get() {
        return get(SPLConfig.class);
    }

    public boolean isDiscoveryEnabled() {
        return discover;
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
}
