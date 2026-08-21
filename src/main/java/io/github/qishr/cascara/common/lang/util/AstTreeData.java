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


package io.github.qishr.cascara.common.lang.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.data.TreeData;
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
