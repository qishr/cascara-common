package io.github.qishr.cascara.common.lang.type;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public class InstantTypeDescriptor extends AbstractScalarDescriptor<Instant> {
    public InstantTypeDescriptor() {
        super(Instant.class, "string", "timestamp"); // TODO: is timestamp correct?
    }

    @Override
    public Instant toJvmType(String text) {
        return Instant.parse(text);
    }

    @Override
    public Object toPrimitive(Instant jvmInstance) {
        return jvmInstance.toEpochMilli();
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
