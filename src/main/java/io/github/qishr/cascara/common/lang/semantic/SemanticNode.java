package io.github.qishr.cascara.common.lang.semantic;

import java.nio.file.Path;
import java.util.List;

public interface SemanticNode {
    String getId();
    String getDisplayName();
    Path getSourcePath();
    int getStartLine();
    int getStartColumn();
    List<? extends SemanticNode> getChildren();
}
