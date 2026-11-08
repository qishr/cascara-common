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
import io.github.qishr.cascara.common.exec.ipc.IpcException;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;

public class LocalIpcTests extends ExecTestBase {
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @Test
    void test_IPC_doesntForwardToSamePID() throws Throwable {

        try (IpcServer ipcServer = new IpcServer()) {
            ipcServer.setDiagnosticForwarding(true)
                     .setIpcDebug(false)
                     .start();

            System.setProperty(IpcClient.PROP_IPC_SOCKET, ipcServer.getSocketPath().toString());

            GlobalReporter.globalInstance().setSystemOutputEnabled(false);
            GlobalReporter reporter = GlobalReporter.forClass(LocalIpcTests.class);
            reporter.setLevel(Level.DEBUG);
            reporter.debug("local");

            GlobalReporter.globalInstance().closeIpc();

            // ipcServer.close();

            List<Diagnostic> diagnostics = ipcServer.getMessages(Diagnostic.class);
            assertTrue(diagnostics.isEmpty());
        } catch (IpcException e) {

        }
    }
}
