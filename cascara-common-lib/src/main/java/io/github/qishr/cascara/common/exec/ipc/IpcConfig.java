package io.github.qishr.cascara.common.exec.ipc;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.exec.ExecutionMessage;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public class IpcConfig {
    public static final String SERIALIZER_CLASS = "io.github.qishr.cascara.lang.json.processor.JsonSerializer";
    public static final String SERIALIZATION_FORMAT = "application/json";

    public static final String HEADER_SEPARATOR = "---";
    public static final String PAYLOAD_SEPARATOR = ":";

    public static final String VERSION = "cascara.version";

    public static final String PROP_IPC_SERVER_PID = "cascara.ipc.server.pid";

    public static final String PROP_DEBUG = "cascara.ipc.debug";

    public static final String PROP_IPC_SERIALIZER = "cascara.ipc.serializer";

    private static final String NL = "\n";

    private SemVer version;
    private long ipcServerPid;
    private boolean ipcDebug;
    private String ipcSerializer;
    private static Class<?> serializerClass;

    @NonNull
    public static Serializer<?> createSerializer() throws IpcException {
        if (serializerClass == null) {
            try {
                Serializer<?> s = new ProcessorFactory().createSerializer(SERIALIZATION_FORMAT);
                if (s != null) {
                    return s;
                }
            } catch (ServiceException e) {
            }
        }

        // If SPL auto-discovery is disabled, this will stil work if the
        // JSON module is on the module path and it doesn't violate
        // the SPL Signature Policy.
        try {
            if (serializerClass == null) {
                serializerClass = Class.forName(IpcConfig.SERIALIZER_CLASS);
            }
            if (serializerClass != null) {
                return (Serializer<?>) ReflectionUtils.createInstance(serializerClass);
            }
        } catch (Throwable e) {
            throw new IpcException(e, ExecutionMessage.NO_DESERIALIZER);
        }
        throw new IpcException(ExecutionMessage.NO_DESERIALIZER);
    }

    public void set(String header) {
        if (header == null) {
            return;
        }
        String[] parts = header.split(":", -1);
        if (parts.length == 2) {
            set(parts[0], parts[1]);
        }
    }

    public void set(String key, String value) {
        switch (key) {
            case VERSION:
                version = new SemVer(value);
                break;
            case PROP_IPC_SERVER_PID:
                ipcServerPid = Long.parseLong(value);
                break;
            case PROP_DEBUG:
                ipcDebug = !("false".equalsIgnoreCase(value));
                break;
            case PROP_IPC_SERIALIZER:
                ipcSerializer = value;
                break;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(VERSION)              .append(":").append(version)      .append(NL);
        sb.append(PROP_IPC_SERVER_PID)  .append(":").append(ipcServerPid) .append(NL);
        sb.append(PROP_DEBUG)           .append(":").append(ipcDebug)     .append(NL);
        sb.append(PROP_IPC_SERIALIZER)  .append(":").append(ipcSerializer).append(NL);
        return sb.toString();
    }

    public SemVer version() {
        return version;
    }

    public void setVersion(SemVer v) {
        version = v;
    }

    public long ipcServerPid() {
        return ipcServerPid;
    }

    public void setIpcServerPid(long v) {
        ipcServerPid = v;
    }

    public boolean ipcDebug() {
        return ipcDebug;
    }

    public void setIpcDebug(boolean v) {
        ipcDebug = v;
    }

    public String ipcSerializer() {
        return ipcSerializer;
    }

    public void setPropIpcSerializer(String v) {
        ipcSerializer = v;
    }
}
