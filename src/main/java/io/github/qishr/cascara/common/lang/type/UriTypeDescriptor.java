package io.github.qishr.cascara.common.lang.type;

import java.net.URI;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public class UriTypeDescriptor extends AbstractScalarDescriptor<URI> {
    public UriTypeDescriptor() {
        super(URI.class, "string", "uri");
    }

    @Override
    public URI toJvmType(String text) {
        return URI.create(text);
    }

    @Override
    public Object toPrimitive(URI jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            URI.create(text);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            formatError(text, collector);
            return false;
        }
    }
}
