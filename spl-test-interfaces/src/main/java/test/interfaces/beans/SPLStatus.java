package test.interfaces.beans;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.service.SPL;

public class SPLStatus implements SPLStatusMBean {
    private static final GlobalReporter REPORTER = GlobalReporter.forClass(SPLStatus.class);
    private double cpuUsage;
    private long memoryUsage;

    public SPLStatus() {
        // TODO: Because this is not a daemon thread, it blocks return
        // Simulate dynamic updates (e.g., from a background thread)
        new Thread(() -> {
            while (true) {
                cpuUsage = Math.random() * 100; // Simulate 0-100% CPU
                memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
        }).start();
    }

    @Override
    public double getCpuUsage() {
        return cpuUsage;
    }

    @Override
    public long getMemoryUsage() {
        return memoryUsage;
    }

    @Override
    public void hello() {
        REPORTER.info(GenericMessage.INFO,"Hello from SPLStatusMBean");
        // Add restart logic here (e.g., close connections, reset state)
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public void setLogLevel(Level level) {
        GlobalReporter.forClass(SPL.class).setLevel(level);
    }
}