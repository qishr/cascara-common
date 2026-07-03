package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.LangDiagnosticCode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public abstract class AbstractScalarDescriptor<T> extends AbstractTypeDescriptor<T> implements ScalarDescriptor<T> {
    public static final String KEYWORD_FORMAT = "format";
    public static final String KEYWORD_CONTENT_ENCODING = "contentEncoding";

    private final String format;
    private final String contentEncoding;

    protected AbstractScalarDescriptor(Class<T> jvmType, String schemaType, String format) {
        this(jvmType, schemaType, format, null);
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

    protected void formatError(String text, Reporter collector) {
        if (collector != null) {
            collector.error(LangDiagnosticCode.WRONG_FORMAT, text, format);
        }
    }
}
