package test.interfaces.beans;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.management.JMException;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import test.interfaces.TestService;

public class CascaraControl implements CascaraControlMBean {
    private static final GlobalReporter REPORTER = GlobalReporter.forClass(CascaraControl.class);

    public CascaraControl() {
    }

    // @Override
    // public void loadModule() {
    //     REPORTER.info(GenericMessage.INFO, "loadModule");
    //     SPL spl = SPL.getRoot();
    //     Path jarPath = Cascara.getModulePath().resolve("provider-a.jar");

    //     SPL layer = spl.create("testLayer");
    //     layer.registerJar(jarPath);
    // }
    @Override
    public void loadModule() throws JMException {
        REPORTER.info(GenericMessage.INFO, "loadModule");
        try {
            SPL spl = SPL.getRoot();
            Path jarPath = Cascara.getModulePath().resolve("provider-a.jar");
            SPL layer = spl.create("testLayer");
            layer.registerJar(jarPath);
            // Class<?> cls = Class.forName("com.example.providera.ProviderA");
            SPL.load(TestService.class);
        } catch (Exception e) {
            REPORTER.error(e, GenericMessage.ERROR, "Failed to load module", e.getMessage());
            // Strip non-serializable ZipPath references before crossing RMI boundary
            throw new JMException(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void unloadModule() throws JMException {
        REPORTER.info(GenericMessage.INFO, "unloadModule");
        try {
            SPL spl = SPL.getRoot();
            spl.remove("testLayer");
            System.gc();
        } catch (Exception e) {
            REPORTER.error(e, GenericMessage.ERROR, "Failed to load module", e.getMessage());
            // Strip non-serializable ZipPath references before crossing RMI boundary
            throw new JMException(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void test() {
        SPL.getRoot();
        REPORTER.info(GenericMessage.INFO, "test successful");
    }

    @Override
    public void exit() {
        REPORTER.info(GenericMessage.INFO, "exit");
        System.exit(0);
    }

    @Override
    public void setLogLevel(Level level) {
        REPORTER.info(GenericMessage.INFO, "setLogLevel");
        GlobalReporter.forClass(SPL.class).setLevel(level);
    }
}