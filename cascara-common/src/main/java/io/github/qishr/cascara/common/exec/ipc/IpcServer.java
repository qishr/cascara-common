package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import io.github.qishr.cascara.common.lang.processor.Serializer;

public final class IpcServer implements AutoCloseable {

    private final Path socketPath;
    private final ServerSocketChannel serverChannel;
    private final Serializer<?> serializer;
    private final Map<String, List<String>> rawPayloadsByType = new ConcurrentHashMap<>();
    private CompletableFuture<Void> listenerFuture;

    private IpcServer(Path socketPath, ServerSocketChannel serverChannel, Serializer<?> serializer) {
        this.socketPath = socketPath;
        this.serverChannel = serverChannel;
        this.serializer = serializer;
    }

    public static IpcServer start(Serializer<?> serializer) throws IOException {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path socketPath = Files.createTempFile(tempDir, "cascara-ipc-", ".sock");
        Files.deleteIfExists(socketPath);

        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(UnixDomainSocketAddress.of(socketPath));

        IpcServer server = new IpcServer(socketPath, serverChannel, serializer);
        server.listenAsync();
        return server;
    }

    public Path getSocketPath() {
        return socketPath;
    }

    private void listenAsync() {
        this.listenerFuture = CompletableFuture.runAsync(() -> {
            while (serverChannel.isOpen()) {
                try {
                    SocketChannel clientChannel = serverChannel.accept();
                    handleClientConnection(clientChannel);
                } catch (AsynchronousCloseException e) {
                    // Server closed cleanly
                    break;
                } catch (IOException e) {
                    if (!serverChannel.isOpen()) break;
                    System.err.println("IPC accept error: " + e.getMessage());
                }
            }
        });
    }

    private void handleClientConnection(SocketChannel clientChannel) {
        try (clientChannel;
             BufferedReader reader = new BufferedReader(
                 Channels.newReader(clientChannel, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    // 1. Read raw frame metadata
                    IpcFrame frame = serializer.fromString(line, IpcFrame.class);

                    // 2. Extract payload directly (re-serialize/map if needed by serializer)
                    String rawPayloadJson = serializer.toString(frame.payload());

                    rawPayloadsByType
                        .computeIfAbsent(frame.type(), k -> new CopyOnWriteArrayList<>())
                        .add(rawPayloadJson);
                }
            }
        } catch (IOException e) {
            // Client disconnected
        }
    }

    // private void listenAsync() {
    //     this.listenerFuture = CompletableFuture.runAsync(() -> {
    //         // 1. Accept the incoming client connection from the sub-JVM
    //         SocketChannel clientChannel;
    //         try {
    //             clientChannel = serverChannel.accept();
    //         } catch (AsynchronousCloseException e) {
    //             // Expected when IpcServer is closed before a client connects
    //             return;
    //         } catch (IOException e) {
    //             System.err.println("IPC Server accept error: " + e.getMessage());
    //             return;
    //         }

    //         // 2. Read incoming frames from the connected client until EOF
    //         try (clientChannel;
    //             BufferedReader reader = new BufferedReader(
    //                 Channels.newReader(clientChannel, StandardCharsets.UTF_8))) {

    //             String line;
    //             while ((line = reader.readLine()) != null) {
    //                 if (!line.isBlank()) {
    //                     IpcFrame frame = serializer.fromString(line, IpcFrame.class);
    //                     System.out.println("SERVER: received: " + line);
    //                     rawPayloadsByType
    //                         .computeIfAbsent(frame.type(), k -> new CopyOnWriteArrayList<>())
    //                         .add(frame.payload());
    //                 }
    //             }
    //         } catch (Exception e) {
    //             // Log frame reading or deserialization issues specifically
    //             System.err.println("IPC Frame Reading Error: " + e.getMessage());
    //         }
    //     });
    // }

    // private void listenAsync() {
    //     this.listenerFuture = CompletableFuture.runAsync(() -> {
    //         try (SocketChannel clientChannel = serverChannel.accept();
    //              BufferedReader reader = new BufferedReader(
    //                  Channels.newReader(clientChannel, StandardCharsets.UTF_8))) {

    //             String line;
    //             while ((line = reader.readLine()) != null) {
    //                 if (!line.isBlank()) {
    //                     IpcFrame frame = (IpcFrame) serializer.fromString(line, IpcFrame.class);
    //                     rawPayloadsByType
    //                         .computeIfAbsent(frame.type(), k -> new CopyOnWriteArrayList<>())
    //                         .add(frame.payload());
    //                 }
    //             }
    //         } catch (Exception e) {
    //             // Socket closed cleanly on process termination
    //             System.err.println("IPC Server Frame Deserialization Error: " + e.getMessage());
    //             e.printStackTrace();
    //         }
    //     });
    // }

    public <T> List<T> getMessages(Class<T> type) {
        if (listenerFuture != null) {
            try {
                listenerFuture.get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
        }
        List<String> raw = rawPayloadsByType.getOrDefault(type.getName(), List.of());
        List<T> result = new ArrayList<>(raw.size());
        for (String json : raw) {
            result.add(type.cast(serializer.fromString(json, type)));
        }
        return result;
    }

    @Override
    public void close() {
        try { serverChannel.close(); } catch (IOException ignored) {}
        try { Files.deleteIfExists(socketPath); } catch (IOException ignored) {}
    }
}