package io.github.qishr.cascara.common.exec.ipc;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceException;

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

    @Nullable
    public static Serializer<?> createSerializer() {
        Serializer<?> s;
        System.err.println("0");
        if (serializerClass == null) {
            try {
                s = new ProcessorFactory().createSerializer(SERIALIZATION_FORMAT);
                if (s != null) {
                    return s;
                }
            } catch (ServiceException e) {
                e.printStackTrace(); // TODO: Remove this
            }
        }
        System.err.println("2");

        // If SPL auto-discovery is disabled, this will stil work if the
        // JSON module is on the module path and it doesn't violate
        // the SPL Signature Policy.
        try {
            System.err.println("3");
            SPL layer = SPL.getRoot();
            if (serializerClass == null) {
                System.err.println("4");
                serializerClass = Class.forName(IpcConfig.SERIALIZER_CLASS);
                layer.registerClass(serializerClass);
            }
            System.err.println("5");
            s = new ProcessorFactory(layer).createSerializer(SERIALIZATION_FORMAT);
            System.err.println("6:" + s);

            return s;
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // TODO: Remove this
        }

        return null;
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
