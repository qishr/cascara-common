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

module integration.test {
    requires java.management;
    requires transitive cascara.common;

    // IPC / Serialization
    requires java.rmi;
    requires cascara.lang.json;

    // Logging
    requires cascara.logging.log4j;
    requires org.apache.logging.log4j.core;

    // Test
    requires org.junit.jupiter.api;
    requires cascara.test.common.junit;
    requires transitive integration.test.fixtures;

    // Exports
    exports integration.test.exec;
    exports integration.test.spl;
    exports integration.test.serialization;

    // For JUnit
    opens integration.test.exec to org.junit.platform.commons;
    opens integration.test.spl to org.junit.platform.commons;
    opens integration.test.serialization to org.junit.platform.commons;
}
