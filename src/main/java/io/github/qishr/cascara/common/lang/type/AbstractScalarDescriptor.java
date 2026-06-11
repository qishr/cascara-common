package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public abstract class AbstractScalarDescriptor<T> extends AbstractTypeDescriptor<T> implements ScalarDescriptor<T> {
    private static final String KEYWORD_FORMAT = "format";
    private static final String KEYWORD_CONTENT_ENCODING = "contentEncoding";

    private String format;
    private String contentEncoding;

    protected AbstractScalarDescriptor(Class<T> jvmType, String type, String format) {
        this(jvmType, type, format, null);
    }

    protected AbstractScalarDescriptor(Class<T> jvmType, String schemaType, String format, String contentEncoding) {
        super(jvmType, schemaType);

        this.format = format;
        this.contentEncoding = contentEncoding;

        if (format != null && !format.isEmpty()) {
            properties.set(KEYWORD_FORMAT, format);
        }
        if (contentEncoding != null && !contentEncoding.isEmpty()) {
            properties.set(KEYWORD_CONTENT_ENCODING, contentEncoding);
        }
    }

    @Override
    public String getFormat() {
        return properties.getString(KEYWORD_FORMAT);
    }

    @Override
    public String getContentEncoding() {
        return properties.getString(KEYWORD_CONTENT_ENCODING);
    }

    @Override
    public void populateSchema(MapAstNode<?,?> node) {
        super.populateSchema(node);

        // Inject other properties scalars might declare.

        if (format != null && !format.isEmpty()) {
            node.put(KEYWORD_FORMAT, format);
        }

        if (contentEncoding != null && !contentEncoding.isEmpty()) {
            node.put(KEYWORD_CONTENT_ENCODING, contentEncoding);
        }
    }
}
