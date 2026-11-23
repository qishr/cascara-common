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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.exception.UnexpectedNullValueException;

@Experimental
public class StringUtils {
    public static final String ELLIPSIS = "\u2026";

    public static final String VISIBLE_NULL = "\u2400";
    public static final String VISIBLE_BELL = "\u2407";
    public static final String VISIBLE_BACKSPACE = "\u2408";
    public static final String VISIBLE_TAB = "\u2409";
    public static final String VISIBLE_LF = "\u240A";
    public static final String VISIBLE_CR = "\u240D";
    public static final String VISIBLE_ESCAPE = "\u241B";
    public static final String VISIBLE_SPACE = "\u2423";

    // Return/Enter symbol: "\u21B5";
    // Tab synbol: "\u21E5"
    // Backspace symbol: "\u232B"

    public static final String VISIBLE_DELETE = "\u2421";

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
        return debugString(-1, string);
    }

    public static String debugString(int limit, String string) {
        if (string == null) return "␀";
        StringBuilder sb = new StringBuilder();
        int countdown = limit;
        for (int codePoint : string.codePoints().toArray()) {
            sb.append(visibleChar(codePoint));
            if (--countdown == 0) {
                break;
            }
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
            case '\b':
                return VISIBLE_BACKSPACE;
            case 7:
                return VISIBLE_BELL;
            case '\n':
                return VISIBLE_LF;
            case '\r':
                return VISIBLE_CR;
            case '\t':
                return VISIBLE_TAB;
            case 27:
                return VISIBLE_ESCAPE;
            default:
                return Character.toString(c);
        }
    }

    public static List<String> parseStringList(String string) {
        return parseStringList(string, ",");
    }

    public static List<String> parseStringList(String string, String delimiter) {
        if (delimiter == null) {
            throw new UnexpectedNullValueException("delimiter");
        }
        List<String> list = new ArrayList<>();
        String[] items = string.split(delimiter);
        for (int i = 0; i < items.length; i++) {
            list.add(items[i].trim());
        }
        return list;
    }

    public static Set<String> parseStringSet(String string) {
        return parseStringSet(string, ",");
    }

    public static Set<String> parseStringSet(String string, String delimiter) {
        if (delimiter == null) {
            throw new UnexpectedNullValueException("delimiter");
        }
        Set<String> list = new HashSet<>();
        String[] items = string.split(delimiter);
        for (int i = 0; i < items.length; i++) {
            list.add(items[i].trim());
        }
        return list;
    }

    public static List<Number> parseNumberList(String numbers) {
        return parseNumberList(numbers, ",");
    }

    public static List<Number> parseNumberList(String numbers, String delimiter) {
        if (delimiter == null) {
            throw new UnexpectedNullValueException("delimiter");
        }
        List<Number> list = new ArrayList<>();
        String[] items = numbers.split(delimiter);
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            if (item.contains(".")) {
                list.add(Double.parseDouble(item.trim()));
            } else {
                list.add(Integer.parseInt(item.trim()));
            }
        }
        return list;
    }

    public static String fromDouble(double d, int fractionalPartLen) {
        String full = Double.toString(d);
        int dot = full.indexOf(".");
        for (int i = dot; i < full.length(); i++) {
            if (i - dot > fractionalPartLen) {
                return full.substring(0, i);
            }
        }
        return full;
    }

    public static String toCamelCase(String string) {
        String[] segments;
        if (string.contains(".")) {
            segments = lowerCaseSplitOnDot(string);
        } else {
            Case fromCase = Case.of(string);
            if (fromCase == Case.CAMEL) {
                return string;
            }
            segments = fromCase.split(string);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            sb.append(Character.toChars(Character.toUpperCase(segment.codePointAt(0))));
            sb.append(segment.substring(1));
        }
        return sb.toString();
    }

    public static String toKebabCase(String string) {
        String[] segments;
        if (string.contains(".")) {
            segments = lowerCaseSplitOnDot(string);
        } else {
            Case fromCase = Case.of(string);
            if (fromCase == Case.KEBAB) {
                return string;
            }
            segments = fromCase.split(string);
        }
        return String.join("-", segments);
    }

    public static String toPascalCase(String string) {
        String[] segments;
        if (string.contains(".")) {
            segments = lowerCaseSplitOnDot(string);
        } else {
            Case fromCase = Case.of(string);
            if (fromCase == Case.PASCAL) {
                return string;
            }
            segments = fromCase.split(string);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            sb.append(Character.toChars(Character.toUpperCase(segment.codePointAt(0))));
            sb.append(segment.substring(1));
        }
        return sb.toString();
    }

    public static String toSnakeCase(String string) {
        String[] segments;
        if (string.contains(".")) {
            segments = lowerCaseSplitOnDot(string);
        } else {
            Case fromCase = Case.of(string);
            if (fromCase == Case.SNAKE) {
                return string;
            }
            segments = fromCase.split(string);
        }
        return String.join("_", segments);
    }

    private static String[] lowerCaseSplitOnDot(String string) {
        String[] split = string.split("\\.", -1);
        String[] segments = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            segments[i] = split[i].toLowerCase();
        }
        return segments;
    }

    public enum Case {
        CAMEL,
        KEBAB,
        PASCAL,
        SNAKE,
        UNKNOWN;

        public static Case of(String string) {
            if (string == null || string.isBlank() || string.length() < 2) {
                return UNKNOWN;
            }
            int[] codePoints = string.codePoints().toArray();
            if (Character.isUpperCase(codePoints[0])) {
                if (!Character.isUpperCase(codePoints[1])) {
                    return PASCAL;
                } else {
                    return UNKNOWN;
                }
            }
            if (string.contains("-")) {
                return KEBAB;
            }
            if (string.contains("_")) {
                return SNAKE;
            }
            return CAMEL;
        }

        public String[] split(String string) {
            if (this == CAMEL || this == PASCAL) {
                return splitOnUppercase(string);
            } else if (this == KEBAB) {
                return string.split("-", -1);
            } else if (this == SNAKE) {
                return string.split("_", -1);
            } else {
                return new String[]{string};
            }
        }

        private String[] splitOnUppercase(String string) {
            List<String> segments = new ArrayList<>();
            int[] codePoints = string.codePoints().toArray();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < codePoints.length; i++) {
                int codePoint = codePoints[i];
                if (Character.isUpperCase(codePoint)) {
                    if (i > 0 && Character.isLowerCase(codePoints[i-1])) {
                        // split
                        segments.add(sb.toString());
                        sb.setLength(0);
                    }
                }
                sb.append(Character.toChars(Character.toLowerCase(codePoint)));
            }
            if (!sb.isEmpty()) {
                segments.add(sb.toString());
            }
            return segments.toArray(new String[]{});
        }
    }
}
