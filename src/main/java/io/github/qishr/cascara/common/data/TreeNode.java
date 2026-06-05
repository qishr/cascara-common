package io.github.qishr.cascara.common.data;

import java.util.List;

public interface TreeNode<T extends TreeNode<T>> {
    List<T> getChildren();
    T getParent();
    void setParent(T parent);
}
