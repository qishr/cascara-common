package io.github.qishr.cascara.common.test.spl;

import io.github.qishr.cascara.common.service.ServiceProviderLayer;

public class SPLTestMain {
    public static void main(String[] args) {
        try {
            ServiceProviderLayer root = ServiceProviderLayer.getRoot();
            root.getProviders().forEach(provider ->
                System.out.println("REGISTERED: " + provider.getTypeName())
            );
        } catch (Throwable t) {
            System.err.println("SPL_INIT_FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}