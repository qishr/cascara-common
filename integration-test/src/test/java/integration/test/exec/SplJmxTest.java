package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.diagnostic.log.TimeSequencedAggregatorLogger;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcException;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.logging.log4j.Log4jLogger;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import integration.test.fixtures.beans.SPLStatusMBean;
import integration.test.fixtures.task.JmxTestTask;

import org.junit.jupiter.api.parallel.Isolated;

@Disabled
@Isolated
public class SplJmxTest extends JmxTestBase {
    private static final Reporter REPORTER = Reporter.forClass(SplJmxTest.class);

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    private void displayBeanInfo(ClassLoadingMXBean classLoading) {
        REPORTER.debug("Currently loaded: " + classLoading.getLoadedClassCount());
        REPORTER.debug("Total loaded: " + classLoading.getTotalLoadedClassCount());
        REPORTER.debug("Total unloaded: " + classLoading.getUnloadedClassCount());
    }

    protected Path createModuleA() throws IOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(SplJmxTest.class)
            .setPath(Cascara.instance().getModulePath().resolve("provider-a.jar"))
            .setMinCascaraVersion("0.10.0")
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();
        return testJar.getPath();
    }

    //
    //
    //

    @Test
    void test_1() throws Throwable {
        REPORTER.debug("CASCARA_HOME=" + getPhysicalHomePath());
        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString("hello-isolated-world");

        boolean ipcDebug = false;

        GlobalReporter.globalInstance().setStackTraceEnabled(true);

        Class<?> taskClass = JmxTestTask.class;

        GlobalReporter.forClass(taskClass).setLevel(Level.DEBUG);
        GlobalReporter.forClass(taskClass).setStackTraceEnabled(true);

        GlobalReporter.forClass(SPL.class).setLevel(Level.DEBUG);
        GlobalReporter.forClass(SPL.class).setStackTraceEnabled(true);

        createModuleA();
        syncVfs();

        // IpcServer ipcServer = IpcServer.start(true, ipcDebug);
        @SuppressWarnings("resource")
        IpcServer ipcServer = new IpcServer()
            .setDiagnosticForwarding(true)
            .setIpcDebug(ipcDebug)
            .start();

        int jmxPort = findFreePort();

        JvmConfig config = newJmxTestConfig(jmxPort)
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)

            // .setSystemProperty(IpcClient.PROP_DEBUG, String.valueOf(ipcDebug))

            .setSystemProperty("cascara.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("cascara.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.PROP_IPC_SOCKET, ipcServer.getSocketPath().toString())
            .setEnv("CASCARA_HOME", getPhysicalHomePath());

        AtomicBoolean finished = new AtomicBoolean(false);

        // Run sub-JVM process...
        REPORTER.debug("Starting new JVM in background");
        JvmProcess.forClass(taskClass)
            .setConfig(config)
            .runAsync(json)
            .thenAccept(response -> {
                // Callback executed upon child process completion
                REPORTER.debug("Background process finished with exit code: " + response.exitCode);
                debug(response);
                try {
                    ipcServer.close();
                } catch (IpcException e) {
                    e.printStackTrace();
                }
                finished.set(true);
            });


        AtomicBoolean sawUnload = new AtomicBoolean(false);
        GlobalReporter.globalInstance().setLineConsumer(line -> {
            if (line.contains("unloaded: 1")) {
                sawUnload.set(true);
            }
        });

        String urlString = String.format("service:jmx:rmi:///jndi/rmi://127.0.0.1:%d/jmxrmi", jmxPort);
        JMXServiceURL url = new JMXServiceURL(urlString);

        JMXConnector jmxConnector = connectWithRetry(url, 20, 2000);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        try {

            // @SuppressWarnings("null")
            ClassLoadingMXBean classLoading =
                    ManagementFactory.newPlatformMXBeanProxy(
                            connection,
                            ManagementFactory.CLASS_LOADING_MXBEAN_NAME,
                            ClassLoadingMXBean.class);

            invokeOperation(connection, "test.interfaces.beans:type=CascaraControl", "test", null, null, true);

            displayBeanInfo(classLoading);
            invokeOperation(connection, "test.interfaces.beans:type=CascaraControl", "loadModule", null, null, true);
            Thread.sleep(1000);

            displayBeanInfo(classLoading);
            invokeOperation(connection, "test.interfaces.beans:type=CascaraControl", "unloadModule", null, null, true);
            Thread.sleep(1000);

            displayBeanInfo(classLoading);

        } catch (Exception e) {
            e.printStackTrace();
        }

        invokeOperation(connection, "test.interfaces.beans:type=CascaraControl", "exit", null, null, false);

        while (!finished.get()) {
            Thread.sleep(1500);
        }

        assertTrue(sawUnload.get());
    }

    @Test
    void test_2() throws Exception {
        // TODO: Make this configurable at *TestBase level
        // Configurator.setLevel("integration.test", org.apache.logging.log4j.Level.DEBUG);
        TimeSequencedAggregatorLogger aggregator = new TimeSequencedAggregatorLogger(new Log4jLogger(), 800);
        GlobalReporter.globalInstance().addLogger(aggregator);
        GlobalReporter.globalInstance().setSystemOutputEnabled(false);


        REPORTER.debug("CASCARA_HOME=" + getPhysicalHomePath());
        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString("hello-isolated-world");

        boolean ipcDebug = false;

        GlobalReporter.globalInstance().setStackTraceEnabled(true);

        Class<?> taskClass = JmxTestTask.class;

        createModuleA();
        syncVfs();

        // IpcServer ipcServer = IpcServer.start(true, ipcDebug);
        @SuppressWarnings("resource")
        IpcServer ipcServer = new IpcServer()
            .setDiagnosticForwarding(true)
            .setIpcDebug(ipcDebug)
            .start();

        int jmxPort = findFreePort();

        JvmConfig config = newJmxTestConfig(jmxPort)
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)
            // .setSystemProperty(IpcClient.PROP_DEBUG, String.valueOf(ipcDebug))
            .setSystemProperty(IpcClient.PROP_IPC_SOCKET, ipcServer.getSocketPath().toString())
            .setEnv("CASCARA_HOME", getPhysicalHomePath());

        AtomicBoolean finished = new AtomicBoolean(false);

        // Run sub-JVM process...
        REPORTER.debug("Starting new JVM in background");
        JvmProcess.forClass(taskClass)
            .setConfig(config)
            .runAsync(json)
            .thenAccept(response -> {
                // Callback executed upon child process completion
                REPORTER.debug("Background process finished with exit code: " + response.exitCode);
                debug(response);
                try {
                    ipcServer.close();
                } catch (IpcException e) {
                    e.printStackTrace();
                }
                finished.set(true);
            });


        String urlString = String.format("service:jmx:rmi:///jndi/rmi://127.0.0.1:%d/jmxrmi", jmxPort);
        JMXServiceURL url = new JMXServiceURL(urlString);

        JMXConnector jmxConnector = connectWithRetry(url, 20, 2000);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        try {

            invokeOperation(connection, "test.interfaces.beans:type=SPLStatus", "test", null, null, true);
            aggregator.flush();

            // @SuppressWarnings("null")
            SPLStatusMBean statusProxy = JMX.newMBeanProxy(
                connection,
                new ObjectName("test.interfaces.beans:type=SPLStatus"),
                SPLStatusMBean.class
            );

            List<String> providers = statusProxy.getRegisteredServiceProviders();
            // Thread.sleep(1000); //Tried removing this sleep

            for (String provieder : providers) {
                REPORTER.debug("Provider: " + provieder);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        invokeOperation(connection, "test.interfaces.beans:type=CascaraControl", "exit", null, null, false);

        while (!finished.get()) {
            Thread.sleep(500);
        }
        aggregator.flush();
    }
}
