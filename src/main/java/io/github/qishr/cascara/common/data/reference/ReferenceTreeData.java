package io.github.qishr.cascara.common.data.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.data.TableData;
import io.github.qishr.cascara.common.data.TreeData;

/// A reference implementation of TreeData
public class ReferenceTreeData implements TreeData<ReferenceTreeData,List<TableData>> {

    private String name;
    private List<ReferenceTreeData> children = new ArrayList<>();
    private ReferenceTreeData parent;
    private List<TableData> value;

    public ReferenceTreeData(String name) {
        this.name = name;
    }

	@Override
    public String getNodeName() {
        return name;
    }

	@Override
	public List<ReferenceTreeData> getChildren() {
        return children;
	}

	@Override
	public ReferenceTreeData getParent() {
        return parent;
	}

	@Override
	public void setParent(ReferenceTreeData parent) {
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
	public List<TableData> getPayload() {
        return value;
	}

    public ReferenceTreeData setValue(List<TableData> data) {
        value = data;
        return this;
    }
}
