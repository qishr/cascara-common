package io.github.qishr.cascara.common.lang.type;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import io.github.qishr.cascara.common.diagnostic.Reporter;

public class LocalDateTimeTypeDescriptor extends AbstractScalarDescriptor<LocalDateTime> {
    public LocalDateTimeTypeDescriptor() {
        super(LocalDateTime.class, "string", "date-time");
    }

    @Override
    public LocalDateTime toJvmType(String text) {
        return LocalDateTime.parse(text);
    }

    @Override
    public Primitive toPrimitive(LocalDateTime value) {
        return Primitive.of(value.toString());
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            LocalDateTime.parse(text);
            return true;
        } catch (DateTimeParseException e) {
            formatError(text, collector);
            return false;
        }
    }

}
