package test.interfaces;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

// Test Fixtures?

public class JmxTestTask extends AbstractExecutionTask<JmxTestInput, JmxTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(JmxTestTask.class);

    @Override
    public JmxTestOutput run(JmxTestInput input) throws Exception {
        reporter.debug("JmxTest");


        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        // Create the MBean instance
        SPLStatus serverStatus = new SPLStatus();

        // Define an ObjectName (domain:key=value)
        ObjectName objectName = new ObjectName("test.interfaces:type=SPLStatus");

        // Register the MBean with the MBean Server
        mBeanServer.registerMBean(serverStatus, objectName);

        // System.out.println("JMX example running. Press Enter to exit...");
        // try { System.in.read(); } catch (Exception e) {}

        Thread.sleep(60000 * 60 * 24);


        JmxTestOutput response = new JmxTestOutput();
        response.n = 4;
        return response;
    }
}