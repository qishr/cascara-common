package io.github.qishr.cascara.common.type.time;

import java.time.Period;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.time.Period Descriptor
public class PeriodDescriptor extends AbstractScalarDescriptor<Period> {
    public PeriodDescriptor() {
        super(Period.class, PrimitiveType.STRING);
    }

    @Override
    public Period toJvmType(String text) {
        return text == null ? null : Period.parse(text);
    }

    @Override
    public String toPrimitive(Period jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Period.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
