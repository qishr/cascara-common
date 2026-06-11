package io.github.qishr.cascara.common.data;

import java.io.PrintWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.data.reference.ReferenceTableData;
import io.github.qishr.cascara.common.data.reference.ReferenceTreeData;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;

public class TreeTests {
    @Test
    void t1() throws LocalizableIOException {
        ReferenceTreeData root = new ReferenceTreeData("root");

        ReferenceTreeData branch1 = new ReferenceTreeData("branch 1");
        root.getChildren().add(branch1);

        ReferenceTreeData b1leaf1 = new ReferenceTreeData("leaf 1");
        branch1.getChildren().add(b1leaf1);

        ReferenceTableData row1 = new ReferenceTableData();
        row1.put("name1", "val1");
        row1.put("name2", "val2");
        b1leaf1.setValue(List.of(row1));

        Tree<ReferenceTreeData,List<TableData>> tree = new Tree<>();
        tree.setRoot(root);
        tree.setRenderValues(true);

        PrintWriter writer = new PrintWriter(System.out);
        tree.render(writer);
        writer.flush();
    }
}
