package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DiagnosticIpcServer implements AutoCloseable {

    private final Path socketPath;
    private final ServerSocketChannel serverChannel;
    private final List<String> rawDiagnostics = Collections.synchronizedList(new ArrayList<>());
    private CompletableFuture<Void> listenerFuture;

    private DiagnosticIpcServer(Path socketPath, ServerSocketChannel serverChannel) {
        this.socketPath = socketPath;
        this.serverChannel = serverChannel;
    }

    public static DiagnosticIpcServer start() throws IOException {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path socketPath = Files.createTempFile(tempDir, "cascara-diag-", ".sock");
        // Delete the dummy file so ServerSocketChannel can bind to the path
        Files.deleteIfExists(socketPath);

        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(UnixDomainSocketAddress.of(socketPath));

        DiagnosticIpcServer server = new DiagnosticIpcServer(socketPath, serverChannel);
        server.listenAsync();
        return server;
    }

    public Path getSocketPath() {
        return socketPath;
    }

    private void listenAsync() {
        this.listenerFuture = CompletableFuture.runAsync(() -> {
            try {
                // Accept the connection from the child JVM
                SocketChannel clientChannel = serverChannel.accept();

                // Read lines continuously without closing the channel until EOF
                BufferedReader reader = new BufferedReader(
                    Channels.newReader(clientChannel, StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        rawDiagnostics.add(line);
                    }
                }
            } catch (IOException e) {
                // Socket closed cleanly or child process exited
            }
        });
    }

    public List<String> getRawDiagnostics() {
        // Wait briefly for listener to flush any remaining buffer
        if (listenerFuture != null) {
            try {
                listenerFuture.get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
        }
        return List.copyOf(rawDiagnostics);
    }

    @Override
    public void close() {
        try {
            serverChannel.close();
        } catch (IOException ignored) {}
        try {
            Files.deleteIfExists(socketPath);
        } catch (IOException ignored) {}
    }
}