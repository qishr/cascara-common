package io.github.qishr.cascara.common.type.util;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

import java.util.SimpleTimeZone;

/// SimpleTimeZone Scalar Descriptor preserving raw offset and time zone ID.
public class SimpleTimeZoneDescriptor extends AbstractScalarDescriptor<SimpleTimeZone> {

    public SimpleTimeZoneDescriptor() {
        super(SimpleTimeZone.class, PrimitiveType.STRING);
    }

    @Override
    public SimpleTimeZone toJvmType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Format expected: "ID:rawOffsetInMillis" or fallback to parsing ID with default offset
        String[] parts = text.split(":");
        String id = parts[0];
        int rawOffset = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        return new SimpleTimeZone(rawOffset, id);
    }

    @Override
    public String toPrimitive(SimpleTimeZone jvmInstance) {
        if (jvmInstance == null) {
            return null;
        }
        // Encode ID and raw offset to ensure precise reconstruction of SimpleTimeZone
        return jvmInstance.getID() + ":" + jvmInstance.getRawOffset();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            String[] parts = text.split(":");
            if (parts.length > 1) {
                Integer.parseInt(parts[1]);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}