package io.github.qishr.cascara.common.data;

public interface TreeData<T extends TreeData<T,V>,V> extends TreeNode<T>, TableData {
    String getNodeName();
    V getPayload();
}
