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
    public static final String ELLIPSIS = "\u2026";

    public static final String VISIBLE_NULL = "\u2400";
    public static final String VISIBLE_SPACE = "\u2423";
    public static final String VISIBLE_TAB = "\u21E5";
    public static final String VISIBLE_CR = "\u240D";
    public static final String VISIBLE_LF = "\u23CE";

    public static final String FILLED_UP_POINTING_TRIANGLE = "\u25B2";

    public static String unescapeUnicode(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        int len = s.length();

        while (i < len) {
            char c = s.charAt(i);

            if (c != '\\' || i + 1 >= len) {
                out.append(c);
                i++;
                continue;
            }

            char esc = s.charAt(i + 1);

            // --- \\uXXXX ---
            if (esc == 'u' && i + 5 < len) {
                int code = 0;
                boolean ok = true;
                for (int j = i + 2; j < i + 6; j++) {
                    int d = Character.digit(s.charAt(j), 16);
                    if (d < 0) { ok = false; break; }
                    code = (code << 4) | d;
                }
                if (ok) {
                    out.append((char) code);
                    i += 6;
                    continue;
                }
            }

            // Fallback: keep the backslash literally
            out.append('\\');
            i++;
        }
        return out.toString();
    }

    public static String unescapeHex(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        int len = s.length();

        while (i < len) {
            char c = s.charAt(i);

            if (c != '\\' || i + 1 >= len) {
                out.append(c);
                i++;
                continue;
            }

            char esc = s.charAt(i + 1);

            // --- \xXX ---
            if (esc == 'x' && i + 3 < len) {
                int d1 = Character.digit(s.charAt(i + 2), 16);
                int d2 = Character.digit(s.charAt(i + 3), 16);
                if (d1 >= 0 && d2 >= 0) {
                    out.append((char) ((d1 << 4) | d2));
                    i += 4;
                    continue;
                }
            }

            // Fallback: keep the backslash literally
            out.append('\\');
            i++;
        }
        return out.toString();
    }

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
        sb.append(FILLED_UP_POINTING_TRIANGLE);
        sb.append("\n");
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
            case '\0':
                return VISIBLE_NULL;
            case ' ':
                return VISIBLE_SPACE;
            case '\t':
                return VISIBLE_TAB;
            case '\r':
                return VISIBLE_CR;
            case '\n':
                return VISIBLE_LF;
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
