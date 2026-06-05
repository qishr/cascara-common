package io.github.qishr.cascara.common.type;

import java.nio.file.Path;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public class PathTypeDescriptor extends AbstractTypeDescriptor {

    public static final String FORMAT = "path";
    public static final String SCHEMA_TYPE = "string";

    public PathTypeDescriptor() {
        super(Path.class, SCHEMA_TYPE, FORMAT);
    }

	@Override
	public void toSchema(MapAstNode<?,?> node) {
        node.put(TYPE_KEYWORD, SCHEMA_TYPE);
        node.put(FORMAT_KEYWORD, FORMAT);
	}

    @Override
    public Object toType(String text) {
        return Path.of(text);
    }

    @Override
    public String toText(Object object) {
        if (object instanceof Path uri) {
            return uri.toString();
        }
        return null;
    }
}
