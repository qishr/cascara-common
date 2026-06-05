package io.github.qishr.cascara.common.type;

import java.time.LocalDateTime;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public class LocalDateTimeDescriptor extends AbstractTypeDescriptor {

    public static final String FORMAT = "date-time";
    public static final String SCHEMA_TYPE = "string";

    public LocalDateTimeDescriptor() {
        super(LocalDateTime.class, SCHEMA_TYPE, FORMAT);
    }

	@Override
	public void toSchema(MapAstNode<?,?> node) {
        node.put(TYPE_KEYWORD, SCHEMA_TYPE);
        node.put(FORMAT_KEYWORD, FORMAT);
	}

    @Override
    public Object toType(String text) {
        return LocalDateTime.parse(text);
    }

    @Override
    public String toText(Object object) {
        if (object instanceof LocalDateTime dateTime) {
            return dateTime.toString();
        }
        return null;
    }
}
