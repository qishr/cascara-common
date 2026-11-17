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
import java.util.concurrent.atomic.AtomicReference;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.ClassReporter;
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
    // private Serializer<?> serializer;
    private ServerSocketChannel serverChannel;
    private final ConcurrentHashMap<String, List<Object>> payloadsByType = new ConcurrentHashMap<>();
    private final Phaser activeConnections = new Phaser(1); // 1 registered for the server itself
    private CompletableFuture<Void> listenerFuture;
    private boolean exceptionForwarding;
    private boolean diagnosticForwarding;
    private boolean filterDiagnosticsByLevel;
    private boolean ipcDebug;
    private AtomicReference<Throwable> clientException = new AtomicReference<>();

    public IpcServer() {}

    public IpcServer setDiagnosticForwarding(boolean enabled) {
        this.diagnosticForwarding = enabled;
        return this;
    }

    public IpcServer setFilterDiagnosticsByLevel(boolean enabled) {
        this.filterDiagnosticsByLevel = enabled;
        return this;
    }

    public IpcServer setExceptionForwarding(boolean enabled) {
        exceptionForwarding = enabled;
        return this;
    }

    public IpcServer setIpcDebug(boolean enabled) {
        this.ipcDebug = enabled;
        return this;
    }

    /// Start listening for client connections.
    ///
    /// @throws IpcSxception for either of:
    ///         - Failed to create temporary file for socket
    ///         - Failed to open socket channel
    ///         - Failed to bind address
    public IpcServer start() throws IpcException {
        try {
            socketPath = Files.createTempFile("cascara-ipc-", ".sock");
            Files.deleteIfExists(socketPath);
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
            this.serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            this.serverChannel.bind(address);
        } catch (IOException e) {
            throw new IpcException(e, e.getMessage());
        }
        listenAsync();
        return this;
    }

    public Throwable getClientException() {
        return clientException.get();
    }

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

    /// Close server socket.
    ///
    /// @throws IpcException for either of:
    ///         - Error when closing socket
    ///         - Failed to delete temporary socket file
    @Override
    public void close() throws IpcException {
        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            if (listenerFuture != null) {
                listenerFuture.join();
            }
        } catch (IOException e) {
            throw new IpcException(e, e.getMessage());
        } finally {
            try {
                if (socketPath != null) {
                    Files.deleteIfExists(socketPath);
                }
            } catch (IOException e) {
                throw new IpcException(e, e.getMessage());
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
        // AtomicReference<Throwable> childThreadException = new AtomicReference<>();
        this.listenerFuture = CompletableFuture.runAsync(() -> listenLoop());
        // {
        //     try {
        //         listenLoop();
        //     } catch (Throwable e) {
        //         childThreadException.set(e);
        //     }
        // });
        // if (childThreadException.get() != null) {
        //     throw childThreadException.get();
        // }
    }

    private void listenLoop() {
        AtomicReference<Throwable> childThreadException = new AtomicReference<>();
        while (serverChannel.isOpen()) {
            try {
                SocketChannel clientChannel = serverChannel.accept();
                activeConnections.register(); // Track active task
                Thread.ofVirtual().start(() -> {
                    try {
                        handleClientConnection(clientChannel);
                    } catch (Throwable t) {
                        childThreadException.compareAndSet(null, t);

                        // Make the while loop advance
                        try {
                            serverChannel.close();
                        } catch (IOException ignored) {}

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
        Throwable thrown = childThreadException.get();
        if (thrown != null) {
            clientException.set(thrown);
            return;
        }
    }

    private void handleClientConnection(SocketChannel clientChannel) throws Throwable {
        try (clientChannel;
             BufferedReader reader = new BufferedReader(
                 Channels.newReader(clientChannel, StandardCharsets.UTF_8))) {

            Serializer<?>  serializer = IpcConfig.createSerializer();
            if (serializer == null) {
                throw new ExecutionException(ExecutionMessage.NO_DESERIALIZER);
            }

            // Send server PID etc in the IPC handshake header
            IpcConfig config = new IpcConfig();
            config.setVersion(Cascara.instance().getVersion());
            config.setIpcServerPid(ProcessHandle.current().pid());
            config.setIpcDebug(ipcDebug);
            config.setPropIpcSerializer(serializer.getClass().getName());
            String header = config.toString() + IpcConfig.HEADER_SEPARATOR + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                if (ipcDebug) {
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

                        e.printStackTrace();

                        // This will close the connection and the client will
                        // output future diagnostics to System.err.
                        // Should this be optional?
                        return;
                    }

                    // Forward exceptions from the child JVM process
                    if (exceptionForwarding && payload instanceof Throwable throwable) {
                        // This will be re-thrown in the parent thread
                        throw throwable;
                    }

                    // Forward diagnostics from the child JVM process
                    if (diagnosticForwarding && payload instanceof Diagnostic diagnostic) {
                        if (filterDiagnosticsByLevel) {
                            // This reports forwarded diagnostics that are included in the source's level:
                            ClassReporter reporter = GlobalReporter.forSource(diagnostic.getSource());
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
                    // TODO all the other related exceptions
                    throw new ExecutionException(e, GenericMessage.CLASS_NOT_FOUND, className);
                } finally {}
            }
        } catch (IOException e) {
            throw new ExecutionException(e, ExecutionMessage.IPC_RECV_FAILED, e.getMessage());
        }
    }
}
