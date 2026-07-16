package io.github.qishr.cascara.common.lang.type;

import java.net.URI;
import java.util.UUID;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public class UuidTypeDescriptor extends AbstractScalarDescriptor<UUID> {
    public UuidTypeDescriptor() {
        super(UUID.class, "string", "uuid");
    }

    @Override
    public UUID toJvmType(String text) {
        return UUID.fromString(text);
    }

    @Override
    public Object toPrimitive(UUID jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            UUID.fromString(text);
            return true;
        } catch (IllegalArgumentException e) {
            formatError(text, collector);
            return false;
        }
    }
}
