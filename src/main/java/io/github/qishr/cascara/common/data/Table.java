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

import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

/// A utility class for creating tables .
///
/// This class allows adding columns with headings and rows with data.
/// It automatically calculates column widths to align the table content.
public class Table {
    List<Column> columns = new ArrayList<>();
    List<String[]> rows = new ArrayList<>();
    boolean showBorder = true;
    boolean showHeaders = true;

    /// Constructs an empty Table.
    public Table() {
        // Nothing to do here
    }

    public void setShowBorder(boolean v) {
        showBorder = v;
    }

    public void setShowHeaders(boolean v) {
        showHeaders = v;
    }

    /// Adds a column with the specified heading to the table.
    /// @param heading String to be used as the column heading.
    /// @return The table (this) to allow method chaining.
    public Table addColumn(String heading) {
        columns.add(new Column(heading));
        return this;
    }


    public Table addRow(TableData row) {
        String[] rowStrings = getRowStrings(row);
        addRow(rowStrings);
        return this;
    }


    private String[] getRowStrings(TableData row) {
        String[] strings = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i).getName();
            Object value = row.get(columnName);
            strings[i] = value == null ? "" : value.toString();
        }
        return strings;
    }


    /// Adds a row of data to the table.
    /// The number of values should match or be less than the number of columns.
    /// @param valueStrings One or more strings representing the data for this row.
    /// @return The table (this) to allow method chaining.
    public Table addRow(String...valueStrings) {
        rows.add(valueStrings);
        // Update column widths if any value is longer than current width
        for (int i = 0; i < valueStrings.length; i++) {
            Column column = columns.get(i);
            if (valueStrings[i] != null && valueStrings[i].length() > column.width) {
                column.width = valueStrings[i].length();
            }
        }
        return this;
    }

    /// Renders the table as text without any indentation.
    /// @param writer The Writer to output text to.
    /// @throws LocalizableRuntimeException If an error occurs during writing.
    public void render(Writer writer) {
        render(writer, 0);
    }

    /// Renders the table as text with a given indentation level (number of spaces).
    /// @param writer The Writer to output text to.
    /// @param indent The number of spaces to indent each line.
    /// @throws LocalizableRuntimeException If an error occurs during writing.
    public void render(Writer writer, int indent) {
        try {
        if (showHeaders) {
            writer.write(" ".repeat(indent));
            // Write header line with column headings
            for (Column column : columns) {
                writer.write("| ");
                writer.write(column.name);
                // Padding spaces to align to column width + 1 trailing space
                writer.write(" ".repeat(column.width - column.name.length() + 1));
            }
            writer.write("|\n");

            writer.write(" ".repeat(indent));
            // Write separator line with dashes for table header separator
            for (Column column : columns) {
                writer.write("|");
                writer.write("-".repeat(column.width + 2));
            }
            writer.write("|\n");
        }

        // Write data rows
        for (String[] data : rows) {
            writer.write(" ".repeat(indent));
            for (int i = 0; i < columns.size(); i++) {
                writer.write("| ");
                writer.write(data[i]);
                // Padding spaces for alignment + 1 trailing space
                writer.write(" ".repeat(columns.get(i).width - data[i].length() + 1));
            }
            writer.write("|\n");
        }
        writer.flush();
    } catch (IOException e) {
        throw new LocalizableRuntimeException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
    }
    }
}