package io.github.qishr.cascara.common.test.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;
import io.github.qishr.cascara.common.lang.type.ScalarDescriptor;
import io.github.qishr.cascara.common.lang.util.SourceBuffer;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProviderFactory;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.service.ServiceProviderRoot;
import io.github.qishr.cascara.common.trackable.tracker.ArrayChangeTracker;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.junit.util.VfsTestBase;
import io.github.qishr.cascara.test.common.junit.service.TestService;
import io.github.qishr.cascara.test.common.junit.service.DemoSingleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SplVfsTests extends SplTestBase {

    @Test
    void loadsPreferredProviderFromVirtualHome() throws IOException {
        Path propsFile = Cascara.getSplPropertiesPath();
        Files.writeString(propsFile,
            "io.github.qishr.cascara.common.io.ResourceProvider=" +
            "io.github.qishr.cascara.common.io.provider.FileResourceProvider\n"
        );

        // Verify SPL reads correctly from NIO Path operations
        Reporter reporter = new StandardReporter().setLevel(Level.DEBUG);
        ServiceProviderRoot spl = ServiceProviderLayer.getRoot(reporter);
        // ServiceProviderRoot root = ServiceProviderLayer.getRoot();
        assertNotNull(spl);

        // Verify SPL works
        SourceBuffer buf = ServiceProviderLayer.loadDefault(SourceBuffer.class);
        assertNotNull(buf);

        // Verify Service Provider factories work
        ServiceProviderFactory spf = new ServiceProviderFactory();
        ScalarDescriptor<?> td = (ScalarDescriptor<?>)spf.createTypeDescriptor(byte[].class);
        assertNotNull(td);
        assertEquals(PrimitiveType.STRING, td.getSchemaType());
        assertEquals("base64", td.getContentEncoding());
    }

    @Test
    void test_loadingAndUnloading() throws IOException {
        // Create a layer
        ServiceProviderLayer layer = spl.create("testLayer");

        List<ArrayChangeTracker<ServiceMetadata>> changes = new ArrayList<>();
        spl.getVisibleProviders().addArrayListener((array, change) -> {
            changes.add(change);
        });

        reporter.info(GenericDiagnosticCode.INFO, "Test registering JAR");
        Path providerAJar = createModuleA();
        layer.registerJar(providerAJar);

        // Verify the provider is registered
        List<ServiceMetadata> providers = spl.findAllProviders(TestService.class);
        assertEquals(1, providers.size());
        ServiceMetadata providerMetadata = providers.getFirst();
        assertEquals("ProviderA", providerMetadata.getProperties().getString("providerName"));

        // Verify the change was tracked
        assertEquals(1, changes.size());
        assertNull(changes.getFirst().getOldValue());

        // Remove the layer
        spl.remove("testLayer");

        // Ensure the provider is unregistered
        providers = spl.findAllProviders(TestService.class);
        assertEquals(0, providers.size());

        // Verify the change was tracked
        assertEquals(2, changes.size());
        assertNull(changes.getLast().getNewValue());
    }

    @Test
    void neoSingletonLifecycle() throws IOException {

        // Track reactive changes
        List<ArrayChangeTracker<ServiceMetadata>> changes = new ArrayList<>();
        spl.getVisibleProviders().addArrayListener((array, change) -> changes.add(change));

        // Create a layer
        ServiceProviderLayer layer = spl.create("singletonLayer");

        // Create synthetic module containing DemoSingleton
        Path singletonJar = createSingletonModule();
        layer.registerJar(singletonJar);

        // Verify singleton is present
        List<ServiceMetadata> providers = spl.findAllProviders(DemoSingleton.class);
        assertEquals(1, providers.size());

        // Verify initializer ran
        DemoSingleton firstInstance = ServiceProviderLayer.loadDefault(DemoSingleton.class);
        assertNotNull(firstInstance);
        assertEquals(1, firstInstance.getInitCount());

        // Reactive ADD event
        assertEquals(1, changes.size());
        assertNull(changes.getFirst().getOldValue());

        // Unload the layer
        spl.remove("singletonLayer");

        // Singleton should be gone
        providers = spl.findAllProviders(DemoSingleton.class);
        assertEquals(0, providers.size());

        // Reactive REMOVE event
        assertEquals(2, changes.size());
        assertNull(changes.getLast().getNewValue());

        // Reload the module
        layer = spl.create("singletonLayer");
        layer.registerJar(singletonJar);

        // New instance should be created
        DemoSingleton secondInstance = ServiceProviderLayer.loadDefault(DemoSingleton.class);
        assertNotNull(secondInstance);
        assertEquals(1, secondInstance.getInitCount());
        assertNotEquals(firstInstance.getUuid(), secondInstance.getUuid());

        // Asking for the same neo-singleton again should return the same instance
        DemoSingleton thirdInstance = ServiceProviderLayer.loadDefault(DemoSingleton.class);
        assertNotNull(thirdInstance);
        assertEquals(1, thirdInstance.getInitCount());
        assertEquals(secondInstance.getUuid(), thirdInstance.getUuid());

        // Reactive ADD event again
        assertEquals(3, changes.size());
    }

}