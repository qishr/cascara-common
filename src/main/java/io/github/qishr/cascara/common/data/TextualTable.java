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


package io.github.qishr.cascara.common.data;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.util.StringUtils;
import io.github.qishr.cascara.common.util.TermUtils;

/// A utility class for creating text-based tables.
///
/// This class allows adding columns with headings and rows with data.
/// It automatically calculates column widths to align the table content.
public class TextualTable {
    private static final String NEWLINE = "\n";
    private static final char SPACE = ' ';

    private List<TextualColumn> columns = new ArrayList<>();
    private List<String[]> rows = new ArrayList<>();
    private boolean showHeaders = true;
    private Style style = Style.MARKDOWN;
    private int maxColumnWidth = -1;
    private String borderColor = null;

    /// Constructs an empty Table.
    public TextualTable() {
        // Nothing to do here
    }

    /// @return The table (this) to allow method chaining.
    public TextualTable setShowHeaders(boolean v) {
        showHeaders = v;
        return this;
    }

    /// @return The table (this) to allow method chaining.
    public TextualTable setStyle(Style style) {
        this.style = style;
        return this;
    }

    /// @return The table (this) to allow method chaining.
    public TextualTable setMaxColumnWidth(int n) {
        maxColumnWidth = n;
        return this;
    }

    /// @return The table (this) to allow method chaining.
    @Experimental
    public TextualTable setBorderColor(String s) {
        borderColor = s;
        return this;
    }

    /// Adds a column with the specified heading to the table.
    /// @param headerText String to be used as the column heading.
    /// @return The table (this) to allow method chaining.
    public TextualTable addColumn(String headerText) {
        TextualColumn column = new TextualColumn(headerText);
        column.setIndex(columns.size());
        columns.add(column);
        return this;
    }

    /// @return The table (this) to allow method chaining.
    public TextualTable addRow(TabularData row) {
        String[] rowStrings = getRowStrings(row);
        addRow(rowStrings);
        return this;
    }

    private String[] getRowStrings(TabularData row) {
        String[] strings = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i).getHeaderText();
            Object value = row.get(columnName);
            strings[i] = value == null ? "" : value.toString();
        }
        return strings;
    }

    /// Adds a row of data to the table.
    /// The number of values should match or be less than the number of columns.
    /// @param valueStrings One or more strings representing the data for this row.
    /// @return The table (this) to allow method chaining.
    public TextualTable addRow(String...valueStrings) {
        rows.add(valueStrings);
        // Update column widths if any value is longer than current width
        for (int i = 0; i < valueStrings.length; i++) {
            TextualColumn column = columns.get(i);
            int cellWidth = valueStrings[i].length();
            if (valueStrings[i] != null && cellWidth > column.width) {
                column.width = cellWidth;
            }
        }
        return this;
    }

    /// Renders the table as text without any indentation.
    /// @param writer The Writer to output text to.
    /// @return The table (this) to allow method chaining.
    /// @throws LocalizableRuntimeException If an error occurs during writing.
    public TextualTable render(Writer writer) {
        render(writer, 0);
        return this;
    }

    /// Renders the table as text with a given indentation level (number of spaces).
    /// @param writer The Writer to output text to.
    /// @param indent The number of spaces to indent each line.
    /// @throws LocalizableRuntimeException If an error occurs during writing.
    public TextualTable render(Writer writer, int indent) {
        try {
            // Top border
            if (style.top() != '\0') {
                writer.write(repeatCharacter(SPACE, indent));
                writer.write(border(style.topLeft()));
                for (int i = 0; i < columns.size(); i++) {
                    TextualColumn column = columns.get(i);
                    int columnWidth = columnWidth(column);
                    writer.write(border(repeatCharacter(style.top(), columnWidth + 2)));
                    if (i < columns.size() - 1) {
                        writer.write(border(style.topVertical()));
                    }
                }
                writer.write(border(style.topRight()));
                writer.write(NEWLINE);
            }

            // Headers
            if (showHeaders) {
                writer.write(repeatCharacter(SPACE, indent));
                writer.write(border(style.left()));
                // Write header line with column headings
                for (int i = 0; i < columns.size(); i++) {
                    TextualColumn column = columns.get(i);
                    writer.write(' ');
                    writer.write(headerText(column));
                    if (i < columns.size() - 1) {
                        writer.write(border(style.midVertical()));
                    }
                }
                writer.write(border(style.right()));
                writer.write(NEWLINE);

                writer.write(border(repeatCharacter(SPACE, indent)));
                writer.write(border(style.leftHorizontal()));

                // Header separator
                for (int i = 0; i < columns.size(); i++) {
                    TextualColumn column = columns.get(i);
                    int columnWidth = columnWidth(column);
                    writer.write(border(repeatCharacter(style.midHorizontal(), columnWidth + 2)));
                    if (i < columns.size() - 1) {
                        writer.write(border(style.midIntersect()));
                    }
                }
                writer.write(border(style.rightHorizontal()));
                writer.write(NEWLINE);
            }

            // Data rows
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                writer.write(repeatCharacter(SPACE, indent));
                writer.write(border(style.left()));
                for (int i = 0; i < columns.size(); i++) {
                    TextualColumn column = columns.get(i);
                    writer.write(' ');
                    writer.write(cellText(column, rowIndex));
                    if (i < columns.size() - 1) {
                        writer.write(border(style.midVertical()));
                    }
                }
                writer.write(border(style.right()));
                writer.write(NEWLINE);
            }

            // Bottom border
            if (style.bot() != '\0') {
                writer.write(repeatCharacter(SPACE, indent));
                writer.write(border(style.botLeft()));
                for (int i = 0; i < columns.size(); i++) {
                    TextualColumn column = columns.get(i);
                    int columnWidth = columnWidth(column);
                    writer.write(border(repeatCharacter(style.bot(), columnWidth + 2)));
                    if (i < columns.size() - 1) {
                        writer.write(border(style.botVertical()));
                    }
                }
                writer.write(border(style.botRight()));
                writer.write(NEWLINE);
            }

            writer.flush();
            return this;
        } catch (IOException e) {
            throw new LocalizableRuntimeException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }
    }

    private String border(char c) {
        if (borderColor == null) {
            return ""+c;
        } else {
            return borderColor + c + TermUtils.ANSI_RESET;
        }
    }

    private String border(String s) {
        if (borderColor == null) {
            return s;
        } else {
            return borderColor + s + TermUtils.ANSI_RESET;
        }
    }

    private int columnWidth(TextualColumn column) {
        return maxColumnWidth == -1
            ? column.width
            : Math.min(column.width, maxColumnWidth);
    }

    private String headerText(TextualColumn column) {
        return pad(column.headerText, columnWidth(column) + 1);
    }

    private String cellText(TextualColumn column, int rowIndex) {
        return pad(rows.get(rowIndex)[column.index], columnWidth(column) + 1);
    }

    private String pad(String text, int maxLength) {
        int textLength = text.length();
        if (textLength <= maxLength) {
            return text + repeatCharacter(SPACE, maxLength - textLength);
        } else {
            return text.substring(0, maxLength - 1) + StringUtils.ELLIPSIS;
        }
    }

    private String repeatCharacter(char c, int times) {
        return Character.toString(c).repeat(times);
    }

    public static interface Style {
        public static final Style MARKDOWN = new MarkdownStyle();
        public static final Style ROUNDED = new RoundedStyle();
        public static final Style SQUARED = new SquaredStyle();

        char top();
        char topRight();
        char right();
        char botRight();
        char bot();
        char botLeft();
        char left();
        char topLeft();

        char topVertical();
        char botVertical();
        char leftHorizontal();
        char rightHorizontal();

        char midVertical();
        char midHorizontal();
        char midIntersect();
    }

    static class MarkdownStyle implements Style {
        public char top() { return '\0'; }
        public char topRight() { return '\0'; }
        public char right() { return '|'; }
        public char botRight() { return '\0'; }
        public char bot() { return '\0'; }
        public char botLeft() { return '\0'; }
        public char left() { return '|'; }
        public char topLeft() { return '\0'; }

        public char topVertical() { return '\0'; }
        public char botVertical() { return '\0'; }
        public char leftHorizontal() { return '|'; }
        public char rightHorizontal() { return '|'; }

        public char midVertical() { return '|'; }
        public char midHorizontal() { return '-'; }
        public char midIntersect() { return '|'; }
    }

    static class RoundedStyle implements Style {
        public char top() { return '\u2500'; }
        public char topRight() { return '\u256E'; }
        public char right() { return '\u2502'; }
        public char botRight() { return '\u256F'; }
        public char bot() { return '\u2500'; }
        public char botLeft() { return '\u2570'; }
        public char left() { return '\u2502'; }
        public char topLeft() { return '\u256D'; }

        public char topVertical() { return '\u252C'; }
        public char botVertical() { return '\u2534'; }
        public char leftHorizontal() { return '\u251C'; }
        public char rightHorizontal() { return '\u2524'; }

        public char midVertical() { return '\u2502'; }
        public char midHorizontal() { return '\u2500'; }
        public char midIntersect() { return '\u253C'; }
    }

    static class SquaredStyle implements Style {
        public char top() { return '\u2501'; }
        public char topRight() { return '\u2510'; }
        public char right() { return '\u2503'; }
        public char botRight() { return '\u2518'; }
        public char bot() { return '\u2501'; }
        public char botLeft() { return '\u2514'; }
        public char left() { return '\u2503'; }
        public char topLeft() { return '\u250C'; }

        public char topVertical() { return '\u252F'; }
        public char botVertical() { return '\u2537'; }
        public char leftHorizontal() { return '\u2520'; }
        public char rightHorizontal() { return '\u2528'; }

        public char midVertical() { return '\u2502'; }
        public char midHorizontal() { return '\u2504'; }
        public char midIntersect() { return '\u253C'; }
    }
}