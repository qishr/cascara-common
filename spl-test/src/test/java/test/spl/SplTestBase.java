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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.junit.util.TestModulePackager;
import io.github.qishr.cascara.test.common.junit.util.VfsTestBase;

public class SplTestBase extends VfsTestBase {
    protected Path createModuleA() throws IOException {
        Path providerAJar = Cascara.getModulePath().resolve("provider-a.jar");

        // Synthetic Module A with explicit module-info
        TestModulePackager.createSyntheticModuleJar(
            providerAJar,
            "0.10.0",
            Map.of(
                "module-info",
                "module provider.a { " +
                "    requires cascara.common; " +
                "    requires test.interfaces; " +
                "    exports com.example.providera; " +
                "    opens com.example.providera to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.providera.ProviderA;" +
                "}",

                "com.example.providera.ProviderA",
                "package com.example.providera; " +
                "import test.interfaces.TestService; " +
                "import io.github.qishr.cascara.common.property.Properties; " +
                "public class ProviderA implements TestService { " +
                "    public String getName() { return \"ProviderA\"; } " +
                "    public Properties getServiceProperties() { return null; } " +
                "}"
            ),
            List.of("cascara-common", "spl-test-interfaces"),
            "spl-test"
        );
        return providerAJar;
    }

    protected Path createModuleB() throws IOException {
        Path providerBJar = Cascara.getModulePath().resolve("provider-b.jar");
        // Synthetic Module B with explicit module-info
        TestModulePackager.createSyntheticModuleJar(
            providerBJar,
            "0.10.0",
            Map.of(
                "module-info",
                "module provider.b { " +
                "    requires cascara.common; " +
                "    requires test.interfaces; " +
                "    exports com.example.providerb; " +
                "    opens com.example.providerb to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.providerb.ProviderB;" +
                "}",

                "com.example.providerb.ProviderB",
                "package com.example.providerb; " +
                "import test.interfaces.TestService; " +
                "import io.github.qishr.cascara.common.property.Properties; " +
                "public class ProviderB implements TestService { " +
                "    public String getName() { return \"ProviderB\"; } " +
                "    public Properties getServiceProperties() { return null; } " +
                "}"
            ),
            List.of("cascara-common", "spl-test-interfaces"),
            "spl-test"
        );
        return providerBJar;
    }

    protected Path createSingletonModule() throws IOException {
        Path jar = Cascara.getModulePath().resolve("singleton.jar");

        TestModulePackager.createSyntheticModuleJar(
            jar,
            "0.10.0",
            Map.of(
                "module-info",
                "module singleton.demo { " +
                "    requires cascara.common; " +
                "    requires test.interfaces; " +
                "    exports com.example.singleton; " +
                "    opens com.example.singleton to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.singleton.DemoSingletonImpl;" +
                "}",

                "com.example.singleton.DemoSingletonImpl",
                "package com.example.singleton; " +
                "import java.util.UUID; " +
                "import test.interfaces.DemoSingleton; " +
                "import io.github.qishr.cascara.common.annotation.SingletonInitializer; " +
                "public final class DemoSingletonImpl implements DemoSingleton { " +
                "    public static int initCount = 0; " +
                "    public UUID uuid; " +
                "    @SingletonInitializer private void init() { initCount++; uuid = UUID.randomUUID(); } " +
                "    public int getInitCount() { return initCount; } " +
                "    public UUID getUuid() { return uuid; } " +
                "}"
            ),
            List.of("cascara-common", "spl-test-interfaces"),
            "spl-test"
        );

        return jar;
    }
}
