package io.github.qishr.cascara.common.type;

import java.net.URI;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public class UriTypeDescriptor extends AbstractTypeDescriptor {

    public static final String FORMAT = "uri";
    public static final String SCHEMA_TYPE = "string";

    public UriTypeDescriptor() {
        super(URI.class, SCHEMA_TYPE, FORMAT);
    }

	@Override
	public void toSchema(MapAstNode<?,?> node) {
        node.put(TYPE_KEYWORD, SCHEMA_TYPE);
        node.put(FORMAT_KEYWORD, FORMAT);
	}

    @Override
    public Object toType(String text) {
        return URI.create(text);
    }

    @Override
    public String toText(Object object) {
        if (object instanceof URI uri) {
            return uri.toString();
        }
        return null;
    }
}
