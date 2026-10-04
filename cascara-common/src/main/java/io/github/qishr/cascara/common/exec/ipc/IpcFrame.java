package io.github.qishr.cascara.common.exec.ipc;

public class IpcFrame {
    String type;
    Object payload;
    public IpcFrame(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
    public static IpcFrame of(Object payload) {
        return new IpcFrame(payload.getClass().getName(), payload);
    }
    public Object payload() { return payload; }
    public String type() { return type; }
}

// public record IpcFrame(
//     String type,
//     String payload
// ) {
    // public static IpcFrame of(Class<?> type, String payload) {
    //     return new IpcFrame(type.getName(), payload);
    // }
// }