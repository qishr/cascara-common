package integration.test.fixtures.task;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import integration.test.fixtures.beans.CascaraControl;
import integration.test.fixtures.beans.SPLStatus;
import integration.test.fixtures.payload.JmxTestInput;
import integration.test.fixtures.payload.JmxTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.util.Cascara;


public class JmxTestTask extends AbstractExecutionTask<JmxTestInput, JmxTestOutput> {
    private static final Reporter REPORTER = Reporter.forClass(JmxTestTask.class);

    @Override
    public JmxTestOutput run(JmxTestInput input) throws Exception {
        System.out.println("Hello, outside world!");

        Files.walk(Cascara.get().getHome().getParent())
            .forEach(f -> System.out.println(f));

            REPORTER.debug("Cascara home: " + Cascara.get().getHome());

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();




        REPORTER.debug("Creating CascaraControl MBean");

        // Create the MBean instance
        CascaraControl cascaraControl = new CascaraControl();

        // Define an ObjectName (domain:key=value)
        ObjectName ccName = new ObjectName("test.interfaces.beans:type=CascaraControl");

        // Register the MBean with the MBean Server
        mBeanServer.registerMBean(cascaraControl, ccName);




        REPORTER.debug("Creating SPLStatus MBean");

        // Create the MBean instance
        SPLStatus serverStatus = new SPLStatus();

        // Define an ObjectName (domain:key=value)
        ObjectName ssName = new ObjectName("test.interfaces.beans:type=SPLStatus");

        // Register the MBean with the MBean Server
        mBeanServer.registerMBean(serverStatus, ssName);





        REPORTER.debug("Setup Complete");

        Thread.sleep(60000 * 60 * 24);

        JmxTestOutput response = new JmxTestOutput();
        response.n = 4;
        return response;
    }
}