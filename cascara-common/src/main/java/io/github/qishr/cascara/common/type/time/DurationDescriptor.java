package io.github.qishr.cascara.common.type.time;

import java.time.Duration;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

// java.time.Duration Descriptor
public class DurationDescriptor extends AbstractScalarDescriptor<Duration> {
    public DurationDescriptor() {
        super(Duration.class, PrimitiveType.STRING);
    }

    @Override
    public Duration toJvmType(String text) {
        return text == null ? null : Duration.parse(text);
    }

    @Override
    public String toPrimitive(Duration jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Duration.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

