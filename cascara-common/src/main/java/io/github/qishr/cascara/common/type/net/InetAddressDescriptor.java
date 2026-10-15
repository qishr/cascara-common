package io.github.qishr.cascara.common.type.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.net.InetAddress Descriptor (handles Inet4Address and Inet6Address via host address/name)
public class InetAddressDescriptor extends AbstractScalarDescriptor<InetAddress> {
    public InetAddressDescriptor() {
        super(InetAddress.class, PrimitiveType.STRING);
    }

    @Override
    public InetAddress toJvmType(String text) {
        if (text == null) return null;
        try {
            return InetAddress.getByName(text);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host address: " + text, e);
        }
    }

    @Override
    public String toPrimitive(InetAddress jvmInstance) {
        return jvmInstance.getHostAddress();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            InetAddress.getByName(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}