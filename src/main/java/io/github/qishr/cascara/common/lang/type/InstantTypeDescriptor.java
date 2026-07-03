package io.github.qishr.cascara.common.lang.type;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import io.github.qishr.cascara.common.diagnostic.Reporter;

public class InstantTypeDescriptor extends AbstractScalarDescriptor<Instant> {
    public InstantTypeDescriptor() {
        super(Instant.class, "string", "timestamp"); // TODO: is timestamp correct?
    }

    @Override
    public Instant toJvmType(String text) {
        return Instant.parse(text);
    }

    @Override
    public Primitive toPrimitive(Instant value) {
        return Primitive.of(value.toEpochMilli());
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Instant.parse(text);
            return true;
        } catch (DateTimeParseException e) {
            formatError(text, collector);
            return false;
        }
    }
}
