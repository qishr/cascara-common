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


package io.github.qishr.cascara.common.util;

import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
public class StringUtils {

    public static String debugString(String string, int pos) {
        return debugString(string, "", pos);
    }

    public static String debugString(String string) {
        if (string == null) return "␀";
        StringBuilder sb = new StringBuilder();
        for (int codePoint : string.codePoints().toArray()) {
            sb.append(visibleChar(codePoint));
        }
        return sb.toString();
    }

    public static String debugString(String string, String name, int pos) {
        if (string == null) return "␀";
        StringBuilder sb = new StringBuilder().append(debugString(string));
        sb.append('\n');
        for (int i = 0; i < pos; i++) {
            sb.append(' ');
        }
        sb.append("▲\n");
        int nameLength = (name == null || name.isBlank()) ? 0 : name.length();
        int nameIndent = pos - nameLength - (nameLength > 0 ? 1 : 0);
        for (int i = 0; i < nameIndent; i++) {
            sb.append(' ');
        }
        sb.append((name == null || name.isBlank()) ? pos : name + " = " + pos);
        return sb.toString();
    }

    public static String visibleChar(int c) {
        switch (c) {
            case ' ':
                return "␣";
            case '\t':
                return "⇥";
            case '\r':
                return "␍";
            case '\n':
                return "↵";
            default:
                return Character.toString(c);
        }
    }

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
