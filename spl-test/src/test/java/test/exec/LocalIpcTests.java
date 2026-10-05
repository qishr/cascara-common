package test.exec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.exec.JvmProcess;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;
import io.github.qishr.cascara.common.service.SPL;
import test.interfaces.ReporterTestInput;
import test.task.GlobalReporterTestTask;

public class LocalIpcTests extends JvmProcessTestBase {
    @BeforeEach
    protected void setUp() {
        reporter = GlobalReporter.forClass(getClass());
        super.setUp();
    }

    @Test
    void test_IPC_doesntForwardToSamePID() throws IOException {
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");

        IpcServer ipcServer = IpcServer.start(serializer, true);

        System.setProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString());

        GlobalReporter reporter = GlobalReporter.forClass(LocalIpcTests.class);
        reporter.setLevel(Level.DEBUG);
        reporter.debug("local");

        GlobalReporter.globalInstance().closeIpc();

        ipcServer.close();

        List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);
        assertTrue(diagnostics.isEmpty());
    }
}
