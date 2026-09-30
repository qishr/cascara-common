// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package test.spl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;
import io.github.qishr.cascara.common.lang.type.ScalarDescriptor;
import io.github.qishr.cascara.common.lang.util.SourceBuffer;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProviderFactory;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.trackable.tracker.ArrayChangeTracker;
import io.github.qishr.cascara.common.util.Cascara;
import test.interfaces.DemoSingleton;
import test.interfaces.TestService;

class ModuleRegistrationTests extends SplTestBase {

    @Test
    void loadsPreferredProviderFromVirtualHome() throws IOException {
        Path propsFile = Cascara.getSplPropertiesPath();
        Files.writeString(propsFile,
            "io.github.qishr.cascara.common.io.ResourceProvider=" +
            "io.github.qishr.cascara.common.io.provider.FileResourceProvider\n"
        );

        // Verify SPL reads correctly from NIO Path operations
        // Reporter reporter = new StandardReporter();
        //.setLevel(Level.DEBUG);
        SPL spl = SPL.getRoot();
        // ServiceProviderRoot root = ServiceProviderLayer.getRoot();
        assertNotNull(spl);

        // Verify SPL works
        SourceBuffer buf = SPL.load(SourceBuffer.class);
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
        SPL layer = spl.create("testLayer");

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
        SPL layer = spl.create("singletonLayer");

        // Create synthetic module containing DemoSingleton
        Path singletonJar = createSingletonModule();
        layer.registerJar(singletonJar);

        // Verify singleton is present
        List<ServiceMetadata> providers = spl.findAllProviders(DemoSingleton.class);
        assertEquals(1, providers.size());

        // Verify initializer ran
        DemoSingleton firstInstance = SPL.load(DemoSingleton.class);
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
        DemoSingleton secondInstance = SPL.load(DemoSingleton.class);
        assertNotNull(secondInstance);
        assertEquals(1, secondInstance.getInitCount());
        assertNotEquals(firstInstance.getUuid(), secondInstance.getUuid());

        // Asking for the same neo-singleton again should return the same instance
        DemoSingleton thirdInstance = SPL.load(DemoSingleton.class);
        assertNotNull(thirdInstance);
        assertEquals(1, thirdInstance.getInitCount());
        assertEquals(secondInstance.getUuid(), thirdInstance.getUuid());

        // Reactive ADD event again
        assertEquals(3, changes.size());
    }
}