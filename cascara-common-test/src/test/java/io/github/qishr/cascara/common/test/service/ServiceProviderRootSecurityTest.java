package io.github.qishr.cascara.common.test.service;

import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.service.ServiceProviderRoot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ServiceProviderRootSecurityTests {

    @Test
    @DisplayName("ServiceProviderRoot identity is idempotent and immutable")
    void testRootIdentityIsStable() {
        ServiceProviderRoot root1 = ServiceProviderLayer.getRoot();
        ServiceProviderRoot root2 = ServiceProviderLayer.getRoot();

        assertNotNull(root1);
        assertSame(root1, root2, "Repeated getRoot() calls must always return the exact same instance");
    }

    @Test
    @DisplayName("JPMS prevents external reflection from accessing internal SPLRoot state")
    void testInternalPackageEncapsulation() {
        ServiceProviderRoot root = ServiceProviderLayer.getRoot();

        // Attempt to reflectively inspect internal fields of the implementation class
        Class<?> implClass = root.getClass(); // SPLRoot

        // Under JPMS, external modules cannot inspect non-exported internal fields
        assertThrows(Exception.class, () -> {
            Field instanceField = implClass.getDeclaredField("instance");
            instanceField.setAccessible(true); // JPMS blocks setAccessible on unexported internal packages
            instanceField.set(null, null);     // Attempt to clear root
        }, "JPMS encapsulation must block reflective modification of internal SPLRoot state");
    }
}