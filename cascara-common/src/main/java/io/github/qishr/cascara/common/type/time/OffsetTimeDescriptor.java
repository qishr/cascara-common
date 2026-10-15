package io.github.qishr.cascara.common.type.time;

import java.time.OffsetTime;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.time.OffsetTime Descriptor
public class OffsetTimeDescriptor extends AbstractScalarDescriptor<OffsetTime> {
    public OffsetTimeDescriptor() {
        super(OffsetTime.class, PrimitiveType.STRING);
    }

    @Override
    public OffsetTime toJvmType(String text) {
        return text == null ? null : OffsetTime.parse(text);
    }

    @Override
    public String toPrimitive(OffsetTime jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            OffsetTime.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}