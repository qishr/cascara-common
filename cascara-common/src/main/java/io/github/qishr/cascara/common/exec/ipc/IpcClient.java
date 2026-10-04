package io.github.qishr.cascara.common.exec.ipc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.github.qishr.cascara.common.lang.processor.Serializer;

public final class IpcClient implements AutoCloseable {

    public static final String SOCKET_PROP = "cascara.ipc.socket";
    private static volatile IpcClient INSTANCE;

    private final SocketChannel channel;
    private final BufferedWriter writer;
    private final Serializer<?> serializer;

    private IpcClient(SocketChannel channel, BufferedWriter writer, Serializer<?> serializer) {
        this.channel = channel;
        this.writer = writer;
        this.serializer = serializer;
    }

    // public static IpcClient getInstance(Serializer<?> serializer) {
    //     if (INSTANCE == null) {
    //         synchronized (IpcClient.class) {
    //             if (INSTANCE == null) {
    //                 INSTANCE = tryConnect(serializer);
    //             }
    //         }
    //     }
    //     return INSTANCE;
    // }

    // private static IpcClient tryConnect(Serializer<?> serializer) {
    //     String socketPathStr = System.getProperty(SOCKET_PROP);
    //     if (socketPathStr == null || socketPathStr.isBlank()) {
    //         return null;
    //     }

    //     try {
    //         UnixDomainSocketAddress address = UnixDomainSocketAddress.of(Path.of(socketPathStr));
    //         SocketChannel channel = SocketChannel.open(address);
    //         BufferedWriter writer = new BufferedWriter(
    //             Channels.newWriter(channel, StandardCharsets.UTF_8)
    //         );
    //         return new IpcClient(channel, writer, serializer);
    //     } catch (IOException e) {
    //         return null;
    //     }
    // }

    public static IpcClient tryConnect(Serializer<?> serializer) {
        String socketPathStr = System.getProperty(SOCKET_PROP);
        if (socketPathStr == null || socketPathStr.isBlank()) {
            return null;
        }

        Path socketPath = Path.of(socketPathStr);
        if (!Files.exists(socketPath)) {
            return null;
        }

        try {
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
            SocketChannel channel = SocketChannel.open(address);
            BufferedWriter writer = new BufferedWriter(
                Channels.newWriter(channel, StandardCharsets.UTF_8)
            );
            return new IpcClient(channel, writer, serializer);
        } catch (IOException e) {
            return null;
        }
    }

    // public synchronized void send(Object message) throws IOException {
    //     if (message == null) return;

    //     // No double encoding!
    //     IpcFrame frame = IpcFrame.of(message);
    //     String frameJson = serializer.toString(frame);

    //     writer.write(frameJson.replace('\n', ' '));
    //     writer.newLine();
    //     writer.flush();
    // }

    public synchronized void send(Object message) throws IOException {
        if (message == null) return;

        IpcFrame frame = IpcFrame.of(message);
        String frameJson = serializer.toString(frame);

        try {
            writer.write(frameJson.replace('\n', ' '));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            // Close broken channel state
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        try { writer.close(); } catch (IOException ignored) {}
        try { channel.close(); } catch (IOException ignored) {}
    }
}

// public final class IpcClient implements AutoCloseable {

//     public static final String SOCKET_PROP = "cascara.ipc.socket";

//     private final SocketChannel channel;
//     private final BufferedWriter writer;
//     private final Serializer<?> serializer;

//     private IpcClient(SocketChannel channel, BufferedWriter writer, Serializer<?> serializer) {
//         this.channel = channel;
//         this.writer = writer;
//         this.serializer = serializer;
//     }

//     public static IpcClient tryConnect(Serializer<?> serializer) {
//         String socketPathStr = System.getProperty(SOCKET_PROP);
//         if (socketPathStr == null || socketPathStr.isBlank()) {
//             return null;
//         }

//         try {
//             UnixDomainSocketAddress address = UnixDomainSocketAddress.of(Path.of(socketPathStr));
//             SocketChannel channel = SocketChannel.open(address);
//             BufferedWriter writer = new BufferedWriter(
//                 Channels.newWriter(channel, StandardCharsets.UTF_8)
//             );
//             return new IpcClient(channel, writer, serializer);
//         } catch (IOException e) {
//             System.err.println("Failed to connect to IPC socket: " + e.getMessage());
//             return null;
//         }
//     }

//     public synchronized void send(Object message) throws IOException {
//         String payloadJson = serializer.toString(message);
//         IpcFrame frame = IpcFrame.of(message.getClass(), payloadJson);
//         String frameJson = serializer.toString(frame);

//         System.out.println("CLIENT: sending: " + frameJson);
//         writer.write(frameJson.replace('\n', ' '));
//         writer.newLine();
//         writer.flush();
//     }

//     @Override
//     public void close() {
//         try { writer.close(); } catch (IOException ignored) {}
//         try { channel.close(); } catch (IOException ignored) {}
//     }
// }