package io.github.qishr.cascara.common.lang.type;

import java.time.Instant;

public class InstantTypeDescriptor extends AbstractScalarDescriptor<Instant> {
    public InstantTypeDescriptor() {
        super(Instant.class, "string", "timestamp"); // TODO: is timestamp correct?
    }

    @Override
    public Instant toJvmType(String text) {
        return null;
    }

    @Override
    public Primitive toPrimitive(Instant value) {
        return Primitive.of(value.toEpochMilli());
    }
}
