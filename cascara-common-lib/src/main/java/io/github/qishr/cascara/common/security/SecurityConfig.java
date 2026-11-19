package io.github.qishr.cascara.common.security;

import java.nio.file.Path;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.util.Config;

@ConfigNamespace("cascara.sec")
public class SecurityConfig extends Config {
    @ConfigSetting(name = "trust-store")
    private Path trustStorePath;

    @ConfigSetting(name = "trust-store.password")
    private String trustStorePassword;

    @ConfigSetting()
    private String jarVerifier;

    @NonNull
    public static SecurityConfig get() {
        return get(SecurityConfig.class);
    }

    @Nullable
    public Path getTrustStorePath() {
        return trustStorePath;
    }

    @NonNull
    public String getTrustStorePassword() {
        return trustStorePassword == null ? "" : trustStorePassword;
    }

    @NonNull
    public String getJarVerifier() {
        return jarVerifier == null ? "" : jarVerifier;
    }
}
