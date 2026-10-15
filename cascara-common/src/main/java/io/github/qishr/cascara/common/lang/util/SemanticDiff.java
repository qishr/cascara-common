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

package io.github.qishr.cascara.common.lang.util;

import java.util.LinkedHashSet;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.util.StringUtils;

@Experimental
public class SemanticDiff {
    public static String astDiff(AstNode expectedDoc, AstNode actualDoc) {
        StringBuilder sb = new StringBuilder();
        findMismatches(expectedDoc, actualDoc, "#/", sb);
        return sb.toString();
    }

    private static boolean findMismatches(AstNode expectedNode, AstNode actualNode, String path, StringBuilder sb) {
        if (expectedNode == null && actualNode == null) {
            return false;
        }

        if (expectedNode == null && actualNode != null) {
            appendMismatch(StringUtils.VISIBLE_NULL, actualNode.getClass().getSimpleName(), path, sb);
            return true;
        }

        if (expectedNode != null && actualNode == null) {
            appendMismatch(expectedNode.getClass().getSimpleName(), StringUtils.VISIBLE_NULL, path, sb);
            return true;
        }

        if (expectedNode instanceof ScalarAstNode expectedScalar) {
            if (actualNode instanceof ScalarAstNode actualScalar) {
                String expectedValue = expectedScalar.asString();
                String actualValue = actualScalar.asString();
                if (expectedValue == null && actualValue == null) {
                    return false;
                }
                if (expectedValue == null && actualValue != null) {
                    appendMismatch(
                        StringUtils.VISIBLE_NULL,
                        actualValue,
                        path, sb
                    );
                    return true;
                }
                if (expectedValue != null && actualValue == null) {
                    appendMismatch(
                        expectedValue,
                        StringUtils.VISIBLE_NULL,
                        path, sb
                    );
                    return true;
                }
                if (!actualValue.equals(expectedValue)) {
                    appendMismatch(
                        expectedValue,
                        actualValue,
                        path, sb
                    );
                    return true;
                }
            } else {
                appendMismatch(
                    expectedNode.getClass().getSimpleName(),
                    actualNode.getClass().getSimpleName(),
                    path, sb
                );
                return true;
            }
            return false;
        }

        boolean isMismatch = false;

        if (expectedNode instanceof SequenceAstNode expectedArray) {
            if (actualNode instanceof SequenceAstNode actualArray) {
                if (actualArray.size() > expectedArray.size()) {
                    appendMessage("extra element", path, sb);
                }
                if (actualArray.size() < expectedArray.size()) {
                    appendMessage("missing element", path, sb);
                }
                for (int i = 0; i < expectedArray.size(); i++) {
                    AstNode actualElement = i < actualArray.size()
                        ? actualArray.get(i)
                        : null;
                    String name = String.format("[%d]", i);
                    isMismatch |= findMismatches(
                        expectedArray.get(i),
                        actualElement,
                        appendToPath(path, name),
                        sb
                    );
                }
            } else {
                appendMismatch(
                    expectedNode.getClass().getSimpleName(),
                    actualNode.getClass().getSimpleName(),
                    path, sb
                );
                isMismatch = true;
            }
            return isMismatch;
        }

        if (expectedNode instanceof MapAstNode<?,?,?> expectedObject) {
            if (actualNode instanceof MapAstNode<?,?,?> actualObject) {
                LinkedHashSet<Object> mergedKeys = new LinkedHashSet<>();
                int expectedSize = expectedObject.size();
                int actualSize = actualObject.size();
                int items = Math.max(expectedSize, actualSize);
                for (int i = 0; i < items; i++) {
                    if (i < expectedSize) {
                        mergedKeys.add(expectedObject.getEntry(i).getKeyString());
                    }
                    if (i < actualSize) {
                        mergedKeys.add(actualObject.getEntry(i).getKeyString());
                    }
                }
                for (Object key : mergedKeys) {
                    boolean keysExist = true;
                    if (!expectedObject.containsKey(key)) {
                        appendExtra(key, path, sb);
                        keysExist = false;
                        isMismatch = true;
                    }
                    if (!actualObject.containsKey(key)) {
                        appendMissing(key, path, sb);
                        keysExist = false;
                        isMismatch = true;
                    }
                    if (keysExist) {
                        isMismatch |= findMismatches(
                            expectedObject.get(key),
                            actualObject.get(key),
                            appendToPath(path, key),
                            sb
                        );
                    }
                }
            } else {
                appendMismatch(
                    expectedNode.getClass().getSimpleName(),
                    actualNode.getClass().getSimpleName(),
                    path, sb
                );
                isMismatch = true;
            }
            return isMismatch;
        }

        appendMessage("Unexpected type: " + expectedNode.getClass().getSimpleName(), path, sb);
        return true;
    }

    private static String appendToPath(String path, Object name) {
        if (path.endsWith("/")) {
            return path + name;
        } else {
            return path + "/" + name;
        }
    }

    private static void appendMismatch(String expected, String actual, String path, StringBuilder sb) {
        appendMissing(expected, path, sb);
        appendExtra(actual, path, sb);
    }

    private static void appendExtra(Object value, String path, StringBuilder sb) {
        sb.append("+ ");
        sb.append(StringUtils.debugString(value == null ? "" : value.toString()));
        sb.append("\n");
    }

    private static void appendMissing(Object value, String path, StringBuilder sb) {
        sb.append(path);
        sb.append("\n");
        sb.append("- ");
        sb.append(StringUtils.debugString(value == null ? "" : value.toString()));
        sb.append("\n");
    }

    private static void appendMessage(String message, String path, StringBuilder sb) {
        sb.append(path);
        sb.append("\n");
        sb.append("* ");
        sb.append(message);
        sb.append("\n");
    }
}
