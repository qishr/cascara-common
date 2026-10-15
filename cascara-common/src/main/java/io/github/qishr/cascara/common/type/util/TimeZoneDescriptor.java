package io.github.qishr.cascara.common.type.util;

import java.util.TimeZone;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.util.TimeZone Descriptor (mapping via ZoneId string ID)
public class TimeZoneDescriptor extends AbstractScalarDescriptor<TimeZone> {
    public TimeZoneDescriptor() {
        super(TimeZone.class, PrimitiveType.STRING);
    }

    @Override
    public TimeZone toJvmType(String text) {
        if (text == null) return null;
        return TimeZone.getTimeZone(text);
    }

    @Override
    public String toPrimitive(TimeZone jvmInstance) {
        return jvmInstance.getID();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return text != null && !text.isEmpty();
    }
}

