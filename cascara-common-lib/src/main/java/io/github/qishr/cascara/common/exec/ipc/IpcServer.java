// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Phaser;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.UnexpectedNullParameterException;
import io.github.qishr.cascara.common.diagnostic.exception.UnexpectedNullReturnException;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.ReportWriter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.ExecutionException;
import io.github.qishr.cascara.common.exec.ExecutionMessage;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.util.Cascara;

public class IpcServer implements AutoCloseable {
    public static final String SERIALIZATION_FORMAT = "application/json";

    private static final Reporter REPORTER = Reporter.forClass(IpcServer.class);

    private Path socketPath;
    private Serializer<?> serializer;
    private ServerSocketChannel serverChannel;
    private final ConcurrentHashMap<String, List<Object>> payloadsByType = new ConcurrentHashMap<>();
    private final Phaser activeConnections = new Phaser(1); // 1 registered for the server itself
    private CompletableFuture<Void> listenerFuture;
    private boolean diagnosticForwarding;
    private boolean filterDiagnosticsByLevel;
    private boolean ipcDebugEnabled;

    private IpcServer(Path socketPath, boolean diagnosticForwarding, boolean filterDiagnosticsByLevel, boolean ipcDebugEnabled) throws IOException {
        if (socketPath == null) {
            throw new UnexpectedNullParameterException("socketPath");
        }
        this.socketPath = socketPath;
        this.serializer = IpcConfig.createSerializer();
        this.diagnosticForwarding = diagnosticForwarding;
        this.filterDiagnosticsByLevel = filterDiagnosticsByLevel;
        this.ipcDebugEnabled = ipcDebugEnabled;

        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        this.serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        this.serverChannel.bind(address);
    }

    // @NonNull
    // public static IpcServer start(boolean diagnosticForwarding, boolean ipcDebugEnabled) throws IOException {
    //     return start(diagnosticForwarding, false, ipcDebugEnabled);
    // }

    // @NonNull
    // public static IpcServer start(boolean diagnosticForwarding, boolean filterDiagnosticsByLevel, boolean ipcDebugEnabled) throws IOException {
    //     Path tempSocket = Files.createTempFile("cascara-ipc-", ".sock");
    //     Files.deleteIfExists(tempSocket);

    //     IpcServer server = new IpcServer(tempSocket, diagnosticForwarding, filterDiagnosticsByLevel, ipcDebugEnabled);
    //     server.listenAsync();
    //     return server;
    // }

    public IpcServer() throws IOException {

        // IpcServer server = new IpcServer(tempSocket, diagnosticForwarding, filterDiagnosticsByLevel, ipcDebugEnabled);
        // if (socketPath == null) {
        //     throw new UnexpectedNullParameterException("socketPath");
        // }
        // this.socketPath = socketPath;
    }

    public IpcServer setDiagnosticForwarding(boolean b) {
        this.diagnosticForwarding = b;
        return this;
    }

    public IpcServer setFilterDiagnosticsByLevel(boolean b) {
        this.filterDiagnosticsByLevel = b;
        return this;
    }

    public IpcServer setIpcDebug(boolean b) {
        this.ipcDebugEnabled = b;
        return this;
    }

    public IpcServer start() throws IOException {
        // this.socketPath = socketPath;
        // this.diagnosticForwarding = diagnosticForwarding;
        // this.filterDiagnosticsByLevel = filterDiagnosticsByLevel;
        // this.ipcDebugEnabled = ipcDebugEnabled;

        socketPath = Files.createTempFile("cascara-ipc-", ".sock");
        // if (socketPath == null) {
        //     throw new UnexpectedNullReturnException("Files", "createTempFile");
        // }
        Files.deleteIfExists(socketPath);
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        this.serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        this.serverChannel.bind(address);

        this.serializer = IpcConfig.createSerializer();

        listenAsync();
        return this;
    }

    @NonNull
    public Path getSocketPath() {
        return socketPath;
    }

    @SuppressWarnings("null")
    @NonNull
    public <T> List<T> getMessages(Class<T> type) {
        List<Object> rawList = payloadsByType.getOrDefault(type.getName(), List.of());
        if (rawList.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            if (type.isInstance(item)) {
                result.add(type.cast(item));
            }
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            if (listenerFuture != null) {
                listenerFuture.join();
            }
        } finally {
            try {
                if (socketPath != null) {
                    Files.deleteIfExists(socketPath);
                }
            } finally {
                // Guarantees the server deregisters even if socket cleanup throws
                activeConnections.arriveAndAwaitAdvance();
            }
        }
    }

    private synchronized void listenAsync() {
        if (listenerFuture != null && !listenerFuture.isDone()) {
            return;
        }
        this.listenerFuture = CompletableFuture.runAsync(this::listenLoop);
    }

    private void listenLoop() {
        while (serverChannel.isOpen()) {
            try {
                SocketChannel clientChannel = serverChannel.accept();
                activeConnections.register(); // Track active task
                Thread.ofVirtual().start(() -> {
                    try {
                        handleClientConnection(clientChannel);
                    } finally {
                        activeConnections.arriveAndDeregister();
                    }
                });
            } catch (ClosedChannelException e) {
                break;
            } catch (IOException e) {
                if (!serverChannel.isOpen()) break;
            }
        }
    }

    private void handleClientConnection(SocketChannel clientChannel) {
        try (clientChannel;
             BufferedReader reader = new BufferedReader(
                 Channels.newReader(clientChannel, StandardCharsets.UTF_8))) {

            // Send server PID etc in the IPC handshake header
            IpcConfig config = new IpcConfig();
            config.setVersion(Cascara.instance().getVersion());
            config.setIpcServerPid(ProcessHandle.current().pid());
            config.setIpcDebug(ipcDebugEnabled);
            config.setIpcSerializer(serializer.getClass().getName());
            String header = config.toString() + IpcConfig.HEADER_SEPARATOR + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                if (ipcDebugEnabled) {
                    System.out.println("SERVER: " + line);
                }

                int delimiterIdx = line.indexOf(IpcConfig.PAYLOAD_SEPARATOR);
                if (delimiterIdx == -1) continue;

                String className = line.substring(0, delimiterIdx).trim();
                String payloadJson = line.substring(delimiterIdx + 1).trim();

                try {
                    Class<?> targetClass = Class.forName(className);

                    Object payload = null;
                    try {
                        payload = serializer.fromString(payloadJson, targetClass);
                    } catch (SerializerException e) {
                        REPORTER.error(e, GenericMessage.ERROR, "Failed to deserialize payload: " + e.getMessage());
                        ReportWriter writer = REPORTER.getWriter(Level.ERROR);
                        writer.write(2, payloadJson);

                        // This will close the connection and the client will
                        // output future diagnostics to System.err.
                        // Should this be optional?
                        return;
                    }

                    // Forward diagnostics from the child JVM process
                    if (diagnosticForwarding && payload instanceof Diagnostic diagnostic) {
                        // TODO: Make the following two ways of doing this configurable.

                        if (filterDiagnosticsByLevel) {
                            // This reports forwarded diagnostics that are included in the source's level:
                            GlobalReporter reporter = GlobalReporter.forSource(diagnostic.getSource());
                            if (reporter != null) {
                                reporter.report(diagnostic);
                            }
                        } else {
                            // This reports all forwarded diagnostics:
                            GlobalReporter reporter = GlobalReporter.globalInstance();
                            reporter.report(diagnostic);
                        }
                    }

                    payloadsByType
                        .computeIfAbsent(className, k -> new CopyOnWriteArrayList<>())
                        .add(payload);
                } catch (ClassNotFoundException e) {
                    throw new ExecutionException(e, GenericMessage.NO_CLASS_DEF_FOUND_ERROR, className);
                } finally {}
            }
        } catch (IOException e) {
            throw new ExecutionException(e, ExecutionMessage.IPC_RECV_FAILED, e.getMessage());
        }
    }
}
