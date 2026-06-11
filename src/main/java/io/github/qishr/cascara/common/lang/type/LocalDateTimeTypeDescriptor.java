package io.github.qishr.cascara.common.lang.type;

import java.time.LocalDateTime;

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
}
