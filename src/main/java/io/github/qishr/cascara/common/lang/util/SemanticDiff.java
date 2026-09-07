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
