package io.github.qishr.cascara.common.type.time;

import java.time.OffsetDateTime;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.time.OffsetDateTime Descriptor
public class OffsetDateTimeDescriptor extends AbstractScalarDescriptor<OffsetDateTime> {
    public OffsetDateTimeDescriptor() {
        super(OffsetDateTime.class, PrimitiveType.STRING);
    }

    @Override
    public OffsetDateTime toJvmType(String text) {
        return text == null ? null : OffsetDateTime.parse(text);
    }

    @Override
    public String toPrimitive(OffsetDateTime jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            OffsetDateTime.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}