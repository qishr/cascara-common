package io.github.qishr.cascara.common.lang.type;

import java.net.URI;

import io.github.qishr.cascara.common.diagnostic.Reporter;

public class UriTypeDescriptor extends AbstractScalarDescriptor<URI> {
    public UriTypeDescriptor() {
        super(URI.class, "string", "uri");
    }

    @Override
    public URI toJvmType(String text) {
        return URI.create(text);
    }

    @Override
    public Primitive toPrimitive(URI value) {
        return Primitive.of(value.toString());
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
