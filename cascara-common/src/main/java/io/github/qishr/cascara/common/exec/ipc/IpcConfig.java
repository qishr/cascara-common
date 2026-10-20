package io.github.qishr.cascara.common.exec.ipc;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.service.SPL;

public class IpcConfig {
    public static final String SERIALIZER_CLASS = "io.github.qishr.cascara.lang.json.processor.JsonSerializer";
    public static final String SERIALIZATION_FORMAT = "application/json";

    public static final String VERSION = "cascara.version";

    public static final String IPC_SERVER_PID = "cascara.ipc.server.pid";

    public static final String DEBUG_PROP = "cascara.ipc.debug";

    public static final String IPC_SERIALIZER = "cascara.ipc.serializer";

    private static final String NL = "\n";

    private SemVer version;
    private long ipcServerPid;
    private boolean ipcDebug;
    private String ipcSerializer;


    @Nullable
    public static Serializer<?> createSerializer() {
        Serializer<?> serializer = new ProcessorFactory().createSerializer(SERIALIZATION_FORMAT);
        if (serializer == null) {
            try {
                Class<?> serializerClass = Class.forName(IpcConfig.SERIALIZER_CLASS);
                SPL layer = SPL.getRoot();
                layer.registerClass(serializerClass);
                serializer = new ProcessorFactory(layer).createSerializer(SERIALIZATION_FORMAT);

            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return serializer;
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
            case IPC_SERVER_PID:
                ipcServerPid = Long.parseLong(value);
                break;
            case DEBUG_PROP:
                ipcDebug = !("false".equalsIgnoreCase(value));
                break;
            case IPC_SERIALIZER:
                ipcSerializer = value;
                break;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(VERSION)       .append(":").append(version)      .append(NL);
        sb.append(IPC_SERVER_PID).append(":").append(ipcServerPid) .append(NL);
        sb.append(DEBUG_PROP)    .append(":").append(ipcDebug)     .append(NL);
        sb.append(IPC_SERIALIZER).append(":").append(ipcSerializer).append(NL);
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

    public void setIpcSerializer(String v) {
        ipcSerializer = v;
    }
}
