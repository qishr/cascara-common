package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
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

import io.github.qishr.cascara.common.exec.ExecutionException;
import io.github.qishr.cascara.common.exec.ExecutionMessage;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.processor.Serializer;

public class IpcServer implements AutoCloseable {

    private final Path socketPath;
    private final Serializer<?> serializer;
    private final ServerSocketChannel serverChannel;
    private final ConcurrentHashMap<String, List<Object>> payloadsByType = new ConcurrentHashMap<>();
    private final Phaser activeConnections = new Phaser(1); // 1 registered for the server itself
    private CompletableFuture<Void> listenerFuture;

    public IpcServer(Path socketPath, Serializer<?> serializer) throws IOException {
        this.socketPath = socketPath;
        this.serializer = serializer;

        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        this.serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        this.serverChannel.bind(address);
    }

    public static IpcServer start(Serializer<?> serializer) throws IOException {
        Path tempSocket = Files.createTempFile("spl-ipc-", ".sock");
        Files.deleteIfExists(tempSocket);

        IpcServer server = new IpcServer(tempSocket, serializer);
        server.listenAsync();
        return server;
    }

    public synchronized void listenAsync() {
        if (listenerFuture != null && !listenerFuture.isDone()) {
            return;
        }
        this.listenerFuture = CompletableFuture.runAsync(this::listenLoop);
    }

    public Path getSocketPath() {
        return socketPath;
    }

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

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                int delimiterIdx = line.indexOf('|');
                if (delimiterIdx == -1) continue;

                String className = line.substring(0, delimiterIdx).trim();
                String payloadJson = line.substring(delimiterIdx + 1).trim();

                try {
                    Class<?> targetClass = Class.forName(className);

                    Object payload = null;
                    try {
                        payload = serializer.fromString(payloadJson, targetClass);
                    } catch (SerializerException e) {
                        // TODO: Do we report this?
                        System.err.println("SERVER ERROR: Could not deserialize payload: " + e.getMessage());
                    }

                    payloadsByType
                        .computeIfAbsent(className, k -> new CopyOnWriteArrayList<>())
                        .add(payload);

                } catch (Exception e) {
                    // TODO: Do we report this?
                    System.err.println("SERVER ERROR: Could not find class " + className);
                }
            }
        } catch (IOException e) {
            throw new ExecutionException(ExecutionMessage.IPC_RECV_FAILED, e.getMessage());
        }
    }
}
