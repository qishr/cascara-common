package io.github.qishr.cascara.common.data;

/// A class to represent the name and width of a column within a table.
public class Column {
    String name;
    int width;

    /// Constructs a new `Column` object with the specified name.
    /// @param name The name (heading) of the column.
    public Column(String name) {
        this.name = name;
        this.width = name.length();
    }

    public String getName() {
        return name;
    }
}
