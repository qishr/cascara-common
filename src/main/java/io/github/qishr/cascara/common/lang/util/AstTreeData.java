package io.github.qishr.cascara.common.lang.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.data.TreeData;
import io.github.qishr.cascara.common.lang.annotation.Experimental;
import io.github.qishr.cascara.common.lang.ast.AstNode;

@Experimental
public class AstTreeData implements TreeData<AstTreeData,AstNode> {

    private String name;
    private List<AstTreeData> children = new ArrayList<>();
    private AstTreeData parent;
    private AstNode payload;

    public AstTreeData(String name) {
        this.name = name;
    }

    public AstTreeData(AstNode root) {
		this.name = root.toString();
		mirror(root, this);
	}

	private void mirror(AstNode astNode, AstTreeData treeNode) {
		for (AstNode childAstNode : astNode.getChildren()) {
			AstTreeData childNode = new AstTreeData(childAstNode.asString());
			childNode.setPayload(childAstNode);
			children.add(childNode);
			mirror(childAstNode, childNode);
		}
	}

	@Override
    public String getNodeName() {
        return name;
    }

	@Override
	public List<AstTreeData> getChildren() {
        return children;
	}

	@Override
	public AstTreeData getParent() {
        return parent;
	}

	@Override
	public void setParent(AstTreeData parent) {
        this.parent = parent;
	}

	@Override
	public Object[] getValues() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getValues'");
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
	public AstNode getPayload() {
        return payload;
	}

    public AstTreeData setPayload(AstNode data) {
        payload = data;
        return this;
    }
}
