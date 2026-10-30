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

package integration.test.spl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import integration.test.util.SourceCode;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.test.common.junit.util.ArtifactResolver;
import io.github.qishr.cascara.test.common.junit.util.TestModulePackager;
import io.github.qishr.cascara.test.common.junit.util.VfsTestBase;

public class SplTestBase extends VfsTestBase implements SourceCode {
    @BeforeEach
    protected void setUp() throws IOException {
        // GlobalReporter.globalInstance().setLevel(Level.TRACE);
        super.setUp();
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
    }

    // protected String getModulePath() {
    //     Pair<String,String> mp = ArtifactResolver.buildModulePath(
    //         List.of("cascara-common", "integration-test-fixtures", "cascara-lang-json"),
    //         "integration-test"
    //     );
    //     String modulePath = mp.getL();
    //     return modulePath;
    // }

    // protected Path createModuleA() throws IOException {
    //     Path providerAJar = Cascara.getModulePath().resolve("provider-a.jar");

    //     // Synthetic Module A with explicit module-info
    //     TestModulePackager.packageModuleJar(
    //         providerAJar,
    //         "0.10.0",
    //         sourceCodeA(),
    //         List.of("cascara-common", "integration-test-fixtures"),
    //         "integration-test"
    //     );
    //     return providerAJar;
    // }

    // protected Path createModuleB() throws IOException {
    //     Path providerBJar = Cascara.getModulePath().resolve("provider-b.jar");
    //     // Synthetic Module B with explicit module-info
    //     TestModulePackager.packageModuleJar(
    //         providerBJar,
    //         "0.10.0",
    //         sourceCodeB(),
    //         List.of("cascara-common", "integration-test-fixtures"),
    //         "integration-test"
    //     );
    //     return providerBJar;
    // }

    // protected Path createSingletonModule() throws IOException {
    //     Path jar = Cascara.getModulePath().resolve("singleton.jar");

    //     TestModulePackager.packageModuleJar(
    //         jar,
    //         "0.10.0",
    //         sourceCodeS(),
    //         List.of("cascara-common", "integration-test-fixtures"),
    //         "integration-test"
    //     );

    //     return jar;
    // }
}
