package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.channels.Channels;

public final class DiagnosticIpcClient implements AutoCloseable {

    public static final String SOCKET_PROP = "cascara.diag.socket";

    private final SocketChannel channel;
    private final BufferedWriter writer;

    private DiagnosticIpcClient(SocketChannel channel, BufferedWriter writer) {
        this.channel = channel;
        this.writer = writer;
    }

    public static DiagnosticIpcClient tryConnect() {
        String socketPathStr = System.getProperty(SOCKET_PROP);

        if (socketPathStr == null || socketPathStr.isBlank()) {
            return null;
        }

        try {
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(Path.of(socketPathStr));
            SocketChannel channel = SocketChannel.open(address); // Protocol family is automatically inferred from UnixDomainSocketAddress

            // Use Channels.newWriter directly on the SocketChannel
            BufferedWriter writer = new BufferedWriter(
                Channels.newWriter(channel, StandardCharsets.UTF_8)
            );

            return new DiagnosticIpcClient(channel, writer);
        } catch (IOException e) {
            System.err.println("Failed to connect to diagnostic IPC socket: " + e.getMessage());
            return null;
        }
    }

    public synchronized void sendDiagnosticJson(String jsonPayload) throws IOException {
        // Line-delimited JSON
        writer.write(jsonPayload.replace('\n', ' '));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException ignored) {}
        try {
            channel.close();
        } catch (IOException ignored) {}
    }
}