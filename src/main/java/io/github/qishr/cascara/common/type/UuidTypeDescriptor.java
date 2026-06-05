package io.github.qishr.cascara.common.type;

import java.util.UUID;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public class UuidTypeDescriptor extends AbstractTypeDescriptor {

    public static final String FORMAT = "uuid";
    public static final String SCHEMA_TYPE = "string";

    public UuidTypeDescriptor() {
        super(UUID.class, SCHEMA_TYPE, FORMAT);
    }

	@Override
	public void toSchema(MapAstNode<?,?> node) {
        node.put(TYPE_KEYWORD, SCHEMA_TYPE);
        node.put(FORMAT_KEYWORD, FORMAT);
	}

    @Override
    public Object toType(String text) {
        return UUID.fromString(text);
    }

    @Override
    public String toText(Object object) {
        if (object instanceof UUID uri) {
            return uri.toString();
        }
        return null;
    }
}
