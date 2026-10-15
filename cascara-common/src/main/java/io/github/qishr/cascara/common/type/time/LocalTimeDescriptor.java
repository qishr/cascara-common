package io.github.qishr.cascara.common.type.time;

import java.time.LocalTime;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.time.LocalTime Descriptor
public class LocalTimeDescriptor extends AbstractScalarDescriptor<LocalTime> {
    public LocalTimeDescriptor() {
        super(LocalTime.class, PrimitiveType.STRING);
    }

    @Override
    public LocalTime toJvmType(String text) {
        return text == null ? null : LocalTime.parse(text);
    }

    @Override
    public String toPrimitive(LocalTime jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            LocalTime.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}