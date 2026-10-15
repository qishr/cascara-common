package io.github.qishr.cascara.common.type.net;

import java.net.HttpCookie;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.net.HttpCookie Descriptor (using standard Set-Cookie string header format)
public class HttpCookieDescriptor extends AbstractScalarDescriptor<HttpCookie> {
    public HttpCookieDescriptor() {
        super(HttpCookie.class, PrimitiveType.STRING);
    }

    @Override
    public HttpCookie toJvmType(String text) {
        if (text == null) return null;
        List<HttpCookie> cookies = HttpCookie.parse(text);
        if (cookies.isEmpty()) {
            throw new IllegalArgumentException("Invalid HttpCookie string: " + text);
        }
        return cookies.get(0);
    }

    @Override
    public String toPrimitive(HttpCookie jvmInstance) {
        return jvmInstance.toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            return !HttpCookie.parse(text).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}