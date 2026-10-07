package test.exec;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.junit.util.TestModulePackager;
import test.interfaces.payload.ReporterTestInput;
import test.interfaces.task.JmxTestTask;

public class SplJmxTest extends JmxTestBase {
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    // TODO: this fails when running in parallel to other jmx tests. port number?
    @Test
    void test_1() throws Exception {
        REPORTER.debug("CASC_HOME=" + getPhysicalHomePath());
        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        Serializer<?> serializer = ProcessorFactory.system().createSerializer("application/json");
        String json = serializer.toString(input);

        boolean ipcDebug = false;

        GlobalReporter.globalInstance().setStackTraceEnabled(true);

        Class<?> taskClass = JmxTestTask.class;

        GlobalReporter.forClass(taskClass).setLevel(Level.DEBUG);
        GlobalReporter.forClass(taskClass).setStackTraceEnabled(true);

        GlobalReporter.forClass(SPL.class).setLevel(Level.DEBUG);
        GlobalReporter.forClass(SPL.class).setStackTraceEnabled(true);

        createModuleA();
        syncVfs();

        IpcServer ipcServer = IpcServer.start(serializer, true, ipcDebug);

        JvmOptions options = newJmxTestOptions()
            .setTimeout(Duration.ofHours(24))
            .setModuleName(taskClass.getModule().getName())
            .setModulePath(getModulePath())
            .setDebug(PROCESS_DEBUG_ENABLED)

            .setSystemProperty(IpcClient.DEBUG_PROP, String.valueOf(ipcDebug))

            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("casc.report.level." + taskClass.getName(), "DEBUG")
            .setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString())
            .setEnv("CASC_HOME", getPhysicalHomePath());

        AtomicBoolean finished = new AtomicBoolean(false);

        // Run sub-JVM process...
        REPORTER.debug("Starting new JVM in background");
        JvmProcess.forClass(taskClass)
            .setOptions(options)
            .runAsync(json)
            .thenAccept(response -> {
                // Callback executed upon child process completion
                debug("Background process finished with exit code: " + response.exitCode);
                debug(response);
                try {
                    ipcServer.close();
                } catch (IOException e) {
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

        String urlString = "service:jmx:rmi:///jndi/rmi://127.0.0.1:9010/jmxrmi";
        JMXServiceURL url = new JMXServiceURL(urlString);

        JMXConnector jmxConnector = connectWithRetry(url, 20, 1000);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        try {

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
            Thread.sleep(500);
        }

        assertTrue(sawUnload.get());
    }

    private void displayBeanInfo(ClassLoadingMXBean classLoading) {
        REPORTER.debug("Currently loaded: " + classLoading.getLoadedClassCount());
        REPORTER.debug("Total loaded: " + classLoading.getTotalLoadedClassCount());
        REPORTER.debug("Total unloaded: " + classLoading.getUnloadedClassCount());
    }

    protected Path createModuleA() throws IOException {
        Path providerAJar = Cascara.getModulePath().resolve("provider-a.jar");

        // Synthetic Module A with explicit module-info
        TestModulePackager.createSyntheticModuleJar(
            providerAJar,
            "0.11.0",
            Map.of(
                "module-info",
                "module provider.a { " +
                "    requires cascara.common; " +
                "    requires test.interfaces; " +
                "    exports com.example.providera; " +
                "    opens com.example.providera to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.providera.ProviderA;" +
                "}",

                "com.example.providera.ProviderA",
                "package com.example.providera; " +
                "import test.interfaces.TestService; " +
                "import io.github.qishr.cascara.common.property.Properties; " +
                "public class ProviderA implements TestService { " +
                "    public String getName() { return \"ProviderA\"; } " +
                "    public Properties getServiceProperties() { return null; } " +
                "}"
            ),
            List.of("cascara-common", "spl-test-interfaces"),
            "spl-test"
        );
        return providerAJar;
    }
}
