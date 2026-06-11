package io.github.qishr.cascara.common.lang.type;

import java.net.URI;

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
}
