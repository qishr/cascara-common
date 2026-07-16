package io.github.qishr.cascara.common.lang.type;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import io.github.qishr.cascara.common.diagnostic.Reporter;

public class DateTimeTypeDescriptor extends AbstractScalarDescriptor<ZonedDateTime> {
    public DateTimeTypeDescriptor() {
        super(ZonedDateTime.class, "string", "date-time");
    }

    @Override
    public ZonedDateTime toJvmType(String text) {
        return ZonedDateTime.parse(text);
    }

    @Override
    public Object toPrimitive(ZonedDateTime jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            ZonedDateTime.parse(text);
            return true;
        } catch (DateTimeParseException e) {
            formatError(text, collector);
            return false;
        }
    }

}
