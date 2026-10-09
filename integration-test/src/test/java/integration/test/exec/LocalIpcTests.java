package integration.test.exec;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;

public class LocalIpcTests extends ExecTestBase {
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @Test
    void test_IPC_doesntForwardToSamePID() throws IOException {
        Serializer<?> serializer = new ProcessorFactory().createSerializer("application/json");

        IpcServer ipcServer = IpcServer.start(serializer, true, false);

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
