package io.github.qishr.cascara.common.util;

public class CaseConverter {
    public static String kebabCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        int[] codePoints = camelCase.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            int codePoint = codePoints[i];
            if (Character.isUpperCase(codePoint)) {
                if (i + 1 < codePoints.length && Character.isLowerCase(codePoints[i+1])) {
                    sb.append("-");
                }
                sb.append(Character.toLowerCase((char)codePoint));
            } else {
                sb.append(Character.toChars(codePoint));
            }
        }
        return sb.toString();
    }

    public static String pascalCase(String kebabCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true; // Start with uppercase
        int[] codePoints = kebabCase.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            int codePoint = codePoints[i];
            if (codePoint == '-') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toChars(Character.toUpperCase(codePoint)));
                    nextUpper = false;
                } else {
                    sb.append(Character.toChars(Character.toLowerCase(codePoint)));
                }
            }
        }
        return sb.toString();
    }

    public static String camelCase(String kebabCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        int[] codePoints = kebabCase.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            int codePoint = codePoints[i];
            if (codePoint == '-') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toChars(Character.toUpperCase(codePoint)));
                    nextUpper = false;
                } else {
                    sb.append(Character.toChars(codePoint));
                }
            }
        }
        return sb.toString();
    }
}
