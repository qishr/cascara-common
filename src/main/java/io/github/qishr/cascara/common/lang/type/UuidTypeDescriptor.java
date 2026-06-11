package io.github.qishr.cascara.common.lang.type;

import java.util.UUID;

public class UuidTypeDescriptor extends AbstractScalarDescriptor<UUID> {
    public UuidTypeDescriptor() {
        super(UUID.class, "string", "uuid");
    }

    @Override
    public UUID toJvmType(String text) {
        return UUID.fromString(text);
    }

    @Override
    public Primitive toPrimitive(UUID value) {
        return Primitive.of(value.toString());
    }
}
