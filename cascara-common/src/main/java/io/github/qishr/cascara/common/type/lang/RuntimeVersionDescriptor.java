package io.github.qishr.cascara.common.type.lang;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

public class RuntimeVersionDescriptor extends AbstractScalarDescriptor<Runtime.Version> {
    public RuntimeVersionDescriptor() {
        super(Runtime.Version.class, PrimitiveType.STRING);
    }

    @Override
    public Runtime.Version toJvmType(String text) {
        if (text == null) return null;
        return Runtime.Version.parse(text);
    }

    @Override
    public String toPrimitive(Runtime.Version jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Runtime.Version.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}