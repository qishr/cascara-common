package test.interfaces.beans;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public interface SPLStatusMBean {
    // Attribute: CPU Usage (read-only)
    double getCpuUsage();

    // Attribute: Memory Usage (read-only)
    long getMemoryUsage();

    // Operation: Restart the server
    void hello();

    void exit();

    void setLogLevel(Level level);
}