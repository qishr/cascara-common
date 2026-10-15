package io.github.qishr.cascara.common.type.time;

import java.time.ZoneOffset;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.time.ZoneOffset Descriptor
public class ZoneOffsetDescriptor extends AbstractScalarDescriptor<ZoneOffset> {
    public ZoneOffsetDescriptor() {
        super(ZoneOffset.class, PrimitiveType.STRING);
    }

    @Override
    public ZoneOffset toJvmType(String text) {
        return text == null ? null : ZoneOffset.of(text);
    }

    @Override
    public String toPrimitive(ZoneOffset jvmInstance) {
        return jvmInstance.getId();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            ZoneOffset.of(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}