package io.github.qishr.cascara.common.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleTreeNode implements TreeData<SimpleTreeNode,List<TableData>> {

    private String name;
    private List<SimpleTreeNode> children = new ArrayList<>();
    private SimpleTreeNode parent;
    private List<TableData> value;

    public SimpleTreeNode(String name) {
        this.name = name;
    }

	@Override
    public String getNodeName() {
        return name;
    }

	@Override
	public List<SimpleTreeNode> getChildren() {
        return children;
	}

	@Override
	public SimpleTreeNode getParent() {
        return parent;
	}

	@Override
	public void setParent(SimpleTreeNode parent) {
        this.parent = parent;
	}

	@Override
	public Map<String, Object> getValuesMap() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getValuesMap'");
	}

	@Override
	public Object get(String key) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'get'");
	}

	@Override
	public List<TableData> getPayload() {
        return value;
	}

    public SimpleTreeNode setValue(List<TableData> data) {
        value = data;
        return this;
    }

}
