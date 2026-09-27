package io.github.qishr.cascara.common.test.spl;

import io.github.qishr.cascara.common.annotation.SingletonInitializer;
import io.github.qishr.cascara.common.service.ServiceProvider;

public class TestServiceProvider implements ServiceProvider {

    private boolean initialized = false;

    // Non-public constructor for neo-singleton reflection testing
    TestServiceProvider() {}

    @SingletonInitializer
    private void init() {
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}