package io.github.qishr.cascara.common.type.util;

import java.time.Instant;
import java.util.Date;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.util.Date Descriptor (mapping to ISO-8601 string or epoch millis—let's use ISO-8601 string format via Instant for consistency)
public class DateDescriptor extends AbstractScalarDescriptor<Date> {
    public DateDescriptor() {
        super(Date.class, PrimitiveType.STRING);
    }

    @Override
    public Date toJvmType(String text) {
        if (text == null) return null;
        return Date.from(Instant.parse(text));
    }

    @Override
    public String toPrimitive(Date jvmInstance) {
        return jvmInstance.toInstant().toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Instant.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
