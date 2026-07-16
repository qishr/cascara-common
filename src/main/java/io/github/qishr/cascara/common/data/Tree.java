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
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.util.Properties;
import io.github.qishr.cascara.common.util.Property;

public class Tree<T extends TreeData<T,V>,V> {
    private static final String NL = "\n";
    private static final int TAB_SIZE = 2;

    private TreeData<T,V> root;
    private boolean renderValues;

    /// Constructs an empty Tree.
    public Tree() {
        // Nothing to do here
    }

    public void setRenderValues(boolean v) {
        renderValues = v;
    }

    public void setRoot(TreeData<T,V> node) {
        root = node;
    }

    public void render(Writer writer) throws LocalizableIOException {
        render(writer, root, 0);
    }

    public void render(Writer writer, TreeData<T,V> node, int indent) throws LocalizableIOException {
        if (node == null) return;
        try {
            writer.write(" ".repeat(TAB_SIZE * indent));
            writer.write(node.getNodeName() == null ? "NULL" : node.getNodeName());
            writer.write(NL);
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }

        if (renderValues && node.getPayload() != null) {
            renderValue(writer, node.getPayload(), indent + 1);
        }

        for (TreeData<T,V> child : node.getChildren()) {
            render(writer, child, indent + 1);
        }
    }

    /// Renders TreeData.getValue()
    private void renderValue(Writer writer, V value, int indent) throws LocalizableIOException {
        if (value instanceof List list && !list.isEmpty()) {
            Object firstElement = list.getFirst();
            if (firstElement instanceof TableData firstRow) {
                @SuppressWarnings("unchecked")
				List<TableData> rows = list;
                int columnCount = firstRow.getValuesMap().values().size();
                renderTable(writer, columnCount, rows, indent);
            } else {
                System.err.println("[Tree] Unhandled list type: " + firstElement.getClass().getSimpleName());
            }
        } else if (value instanceof Properties properties) {
            List<Property> rows = properties.asList();
            int columnCount = 2;
            renderTable(writer, columnCount, rows, indent);
        } else {
            System.err.println("[Tree] Unhandled value type: " + value.getClass().getSimpleName());
        }
    }

    private void renderTable(Writer writer, int columns, List<? extends TableData> rows, int indent) throws LocalizableIOException {
        Table table = new Table();
        table.setShowHeaders(false);

        TableData firstRow = rows.getFirst();
        for (String columnName : firstRow.getValuesMap().keySet()) {
            table.addColumn(columnName);
        }

        // Add the data
        for (TableData row : rows) {
            table.addRow(row);
        }
        table.render(writer, TAB_SIZE * indent);
    }
}
