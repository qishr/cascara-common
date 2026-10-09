package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.qishr.cascara.common.exec.ExecutionException;
import io.github.qishr.cascara.common.exec.ExecutionMessage;
import io.github.qishr.cascara.common.lang.processor.Serializer;

public final class IpcClient implements AutoCloseable {

    public static final String SOCKET_PROP = "cascara.ipc.socket";
    public static final String DEBUG_PROP = "cascara.ipc.debug";

    private final SocketChannel channel;
    private final BufferedWriter writer;
    private final Serializer<?> serializer;
    private final long serverPid;
    private final boolean ipcDebugEnabled;

    private IpcClient(SocketChannel channel, BufferedWriter writer, Serializer<?> serializer, long serverPid, boolean ipcDebugEnabled) {
        this.channel = channel;
        this.writer = writer;
        this.serializer = serializer;
        this.serverPid = serverPid;
        this.ipcDebugEnabled = ipcDebugEnabled;
    }

    public static IpcClient tryConnect(Serializer<?> serializer) {
        // TODO: DEBUG_PROP shouldd be passed over in handshake rather than as env var.
        String ipcDebugStr = System.getProperty(DEBUG_PROP);
        boolean ipcDebugEnabled = "true".equalsIgnoreCase(ipcDebugStr);

        String socketPathStr = System.getProperty(SOCKET_PROP);
        if (socketPathStr == null || socketPathStr.isBlank()) {
            return null;
        }

        Path socketPath = Path.of(socketPathStr);
        if (!Files.exists(socketPath)) {
            throw new ExecutionException(ExecutionMessage.NO_SOCKET_FILE, socketPathStr);
        }

        try {
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
            SocketChannel channel = SocketChannel.open(address);

            // Read the handshake header from the server
            BufferedReader reader = new BufferedReader(
                Channels.newReader(channel, StandardCharsets.UTF_8)
            );
            String line = reader.readLine();
            if (line == null || !line.startsWith("SERVER_PID:")) {
                throw new ExecutionException(ExecutionMessage.CONNECT_FAILED, "Invalid server handshake header");
            }
            long serverPid = Long.parseLong(line.substring("SERVER_PID:".length()));

            BufferedWriter writer = new BufferedWriter(
                Channels.newWriter(channel, StandardCharsets.UTF_8)
            );
            return new IpcClient(channel, writer, serializer, serverPid, ipcDebugEnabled);
        } catch (IOException | NumberFormatException e) {
            throw new ExecutionException(ExecutionMessage.CONNECT_FAILED, e.getMessage());
        }
    }

    public long getServerProcessId() {
        return this.serverPid;
    }

    public synchronized void send(Object message) throws IOException {
        if (message == null) return;

        String className = message.getClass().getName();
        String json = serializer.toString(message);
        String payloadJson = json.replace('\n', ' ');

        try {
            String line = className + "|" + payloadJson + "\n";

            if (ipcDebugEnabled) {
                System.out.println("CLIENT: " + className + ": " + json);
            }

            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            // Close broken channel state
            close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (channel != null && channel.isOpen()) {
                writer.flush();
                // Signal EOF gracefully to the server before closing the channel
                channel.shutdownOutput();
            }
        } catch (IOException ignored) {
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }
}
