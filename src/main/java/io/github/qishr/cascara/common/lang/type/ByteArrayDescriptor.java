package io.github.qishr.cascara.common.lang.type;

import java.util.Base64;

import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

public class ByteArrayDescriptor extends AbstractScalarDescriptor<byte[]> {
    public ByteArrayDescriptor() {
        super(byte[].class, "string", null, "base64");
    }

    @Override
    public byte[] toJvmType(String text) {
        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            throw new LocalizableRuntimeException(e, GenericDiagnosticCode.MALFORMED_BASE64);
        }
    }

    @Override
    public Object toPrimitive(byte[] jvmInstance) {
        return Base64.getEncoder().encodeToString(jvmInstance);
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            formatError(text, collector);
            return false;
        }
    }
}

