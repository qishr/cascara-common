package io.github.qishr.cascara.common.type.time;

import java.time.ZoneId;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.time.ZoneId Descriptor
public class ZoneIdDescriptor extends AbstractScalarDescriptor<ZoneId> {
    public ZoneIdDescriptor() {
        super(ZoneId.class, PrimitiveType.STRING);
    }

    @Override
    public ZoneId toJvmType(String text) {
        return text == null ? null : ZoneId.of(text);
    }

    @Override
    public String toPrimitive(ZoneId jvmInstance) {
        return jvmInstance.getId();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            ZoneId.of(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}