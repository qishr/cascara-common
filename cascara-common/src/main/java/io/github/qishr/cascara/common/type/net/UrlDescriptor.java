package io.github.qishr.cascara.common.type.net;

import java.net.MalformedURLException;
import java.net.URL;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.net.URL Descriptor
public class UrlDescriptor extends AbstractScalarDescriptor<URL> {
    public UrlDescriptor() {
        super(URL.class, PrimitiveType.STRING);
    }

    @SuppressWarnings("deprecation")
    @Override
    public URL toJvmType(String text) {
        if (text == null) return null;
        try {
            return new URL(text);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + text, e);
        }
    }

    @Override
    public String toPrimitive(URL jvmInstance) {
        return jvmInstance.toString();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            new URL(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

