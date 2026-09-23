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


package io.github.qishr.cascara.common.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.lang.type.TypeReference;

public class SerializerTests extends SerializerTestBase {

    @Test
    void test_simpleTypeReference() {

        PlainScalarNode s1 = new PlainScalarNode("one");
        PlainScalarNode s2 = new PlainScalarNode("two");

        PlainSequenceNode swq = new PlainSequenceNode()
            .add(s1)
            .add(s2);

        List<String> list = serializer.fromAst(swq, new TypeReference<List<String>>() {});

        assertNotNull(list);
    }

    public static class SimpleNestedObjects {
        public  Map<String,List<Double>> contributes = new HashMap<>();
        public  String s;
    }

    @Test
    void test_fromAst_nestedGenerics() {
        PlainMapNode map = new PlainMapNode()
            .put("contributes", new PlainMapNode()
                .put("themes", new PlainSequenceNode()
                    .add(
                        new PlainScalarNode(0.1)
                    )
                )
            );

        SimpleNestedObjects sno = serializer.fromAst(map, SimpleNestedObjects.class);

        assertNotNull(sno);
        List<Double> themes =  sno.contributes.get("themes");

        assertNotNull(themes);
        assertEquals(0.1, themes.getFirst());
    }

    public static sealed class Contribution {
    }

    public static final class ThemeContribution extends Contribution {
        public String name;
    }

    public static class PackageJsonFile {
        public  Map<String,List<Contribution>> contributes = new HashMap<>();
        public  String s;
    }

    @Test
    void test_fromAst_nestedGenerics2() {
        PlainMapNode map = new PlainMapNode()
            .put("contributes", new PlainMapNode()
                .put("themes", new PlainSequenceNode()
                    .add(new PlainMapNode()
                        .put("name", "TestName")
                    )
                )
            );

        PackageJsonFile pjf = serializer.fromAst(map, PackageJsonFile.class);

        assertNotNull(pjf);
        List<Contribution> themes =  pjf.contributes.get("themes");

        assertNotNull(themes);
        assertFalse(themes.isEmpty());

        Contribution c = themes.getFirst();
        assertNotNull(c);

        ThemeContribution t = (ThemeContribution) c;

        assertEquals("TestName", t.name);
    }

    @Test
    void test_toAst_nestedGenerics2() {
        ThemeContribution tc = new ThemeContribution();
        tc.name = "TestName";
        List<Contribution> contribs = new ArrayList<>();
        contribs.add(tc);
        PackageJsonFile pjf = new PackageJsonFile();
        pjf.contributes.put("themes", contribs);

        AstNode rootNode = serializer.toAst(pjf);
        assertInstanceOf(MapAstNode.class, rootNode);
        MapAstNode<?,?,?> rootMap = (MapAstNode<?,?,?>) rootNode;

        AstNode contribsNode = rootMap.get("contributes");
        MapAstNode<?,?,?> contribsMap = (MapAstNode<?,?,?>) contribsNode;
        SequenceAstNode<?> seq = (SequenceAstNode<?>) contribsMap.get("themes");
        assertFalse(seq.isEmpty());

        // TODO: SequenceAstNode does not have getMap() ?
        MapAstNode<?,?,?> theme = (MapAstNode<?,?,?>) seq.getFirst();
        assertNotNull(theme);

        ScalarAstNode<?> nameNode = theme.getScalar("name");
        assertNotNull(nameNode);
        assertEquals("TestName", nameNode.asString());

    }
}
