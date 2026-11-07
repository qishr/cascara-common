package io.github.qishr.cascara.common.exec.ipc;

public class IpcException extends Exception {
    public IpcException(Throwable cause, String msg) {
        super(msg, cause);
    }
}
