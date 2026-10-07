package test.interfaces.task;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.util.Cascara;
import test.interfaces.beans.CascaraControl;
import test.interfaces.payload.JmxTestInput;
import test.interfaces.payload.JmxTestOutput;


public class JmxTestTask extends AbstractExecutionTask<JmxTestInput, JmxTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(JmxTestTask.class);

    @Override
    public JmxTestOutput run(JmxTestInput input) throws Exception {
        System.out.println("Hello, outside world!");

        Files.walk(Cascara.getHomePath().getParent())
            .forEach(f -> System.out.println(f));

        reporter.debug("Cascara home: " + Cascara.getHomePath());

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();




        reporter.debug("Creating CascaraControl MBean");

        // Create the MBean instance
        CascaraControl cascaraControl = new CascaraControl();

        // Define an ObjectName (domain:key=value)
        ObjectName ccName = new ObjectName("test.interfaces.beans:type=CascaraControl");

        // Register the MBean with the MBean Server
        mBeanServer.registerMBean(cascaraControl, ccName);




        // reporter.debug("Creating SPLStatus MBean");

        // // Create the MBean instance
        // SPLStatus serverStatus = new SPLStatus();

        // // Define an ObjectName (domain:key=value)
        // ObjectName ssName = new ObjectName("test.interfaces.beans:type=SPLStatus");

        // // Register the MBean with the MBean Server
        // mBeanServer.registerMBean(serverStatus, ssName);





        reporter.debug("Setup Complete");

        Thread.sleep(60000 * 60 * 24);

        JmxTestOutput response = new JmxTestOutput();
        response.n = 4;
        return response;
    }
}