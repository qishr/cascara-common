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

package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.annotation.PolymorphicType;
import io.github.qishr.cascara.common.annotation.SubType;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;

public class DeserializeAbstractClassAnnotationTests extends SerializerTestBase {

    @PolymorphicType(property = "type")
    public static abstract class BaseTestClass {
        protected BaseTestClass() {}
    }

    @SubType(name = "c0")
    public static class TestClass0 extends BaseTestClass {
        public String field0;
        public String field1;
        public TestClass0() {
            field1 = "TestClass0";
        }
    }

    @SubType(name = "c1")
    public static class TestClass1 extends BaseTestClass {
        public String field0;
        public String field1;
        public TestClass1() {
            field1 = "TestClass1";
        }
    }

    @Test
    void test_subclassDeserializationOfClassWithAnnotation() {
        PlainMapNode map0 = new PlainMapNode()
            .put("type",
                new PlainScalarNode("c0")
            )
            .put("field0",
                new PlainScalarNode("foo")
            );

        PlainMapNode map1 = new PlainMapNode()
            .put("type",
                new PlainScalarNode("c1")
            )
            .put("field0",
                new PlainScalarNode("bar")
            );

        BaseTestClass c0 = serializer.fromAst(map0, BaseTestClass.class);
        assertNotNull(c0);
        TestClass0 tc0 = (TestClass0) c0;
        assertEquals("foo", tc0.field0);

        BaseTestClass c1 = serializer.fromAst(map1, BaseTestClass.class);
        assertNotNull(c1);
        TestClass1 tc1 = (TestClass1) c1;
        assertEquals("bar", tc1.field0);
    }
}
