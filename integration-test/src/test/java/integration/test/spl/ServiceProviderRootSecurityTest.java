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

import io.github.qishr.cascara.common.service.SPL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ServiceProviderRootSecurityTests {

    @Test
    @DisplayName("ServiceProviderRoot identity is idempotent and immutable")
    void testRootIdentityIsStable() {
        SPL root1 = SPL.getRoot();
        SPL root2 = SPL.getRoot();

        assertNotNull(root1);
        assertSame(root1, root2, "Repeated getRoot() calls must always return the exact same instance");
    }

    @Test
    @DisplayName("JPMS prevents external reflection from accessing internal SPLRoot state")
    void testInternalPackageEncapsulation() {
        SPL root = SPL.getRoot();

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