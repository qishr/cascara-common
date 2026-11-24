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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.security.policy.SPLSignaturePolicy;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.test.common.junit.base.TestBase;

@Isolated
public class SplTestBase extends TestBase {
    @BeforeEach
    protected void setUp() {
        super.setUp();
        resetSplPolicy();
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
        resetSplPolicy();
    }

    public static void resetSplPolicy() {
        SPLSignaturePolicy policy = SPLSignaturePolicy.builder()
            .requireSigned(false)
            .build();
        SPL.setSignaturePolicy(policy);
        System.setProperty(SP_CASCARA_SPL_VERIFY, "false");
    }
}
