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
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.qishr.cascara.common.exec.ExecutionException;
import io.github.qishr.cascara.common.i18n.exception.LocalizableIOException;
import io.github.qishr.cascara.common.lang.processor.Serializer;

public final class IpcClient implements AutoCloseable {
    public static final String PROP_IPC_SOCKET = "cascara.ipc.socket";

    private IpcConfig config;
    private final SocketChannel channel;
    private final BufferedWriter writer;
    private final Serializer<?> serializer;

    private IpcClient(SocketChannel channel, BufferedWriter writer, Serializer<?> serializer, IpcConfig config) {
        this.channel = channel;
        this.writer = writer;
        this.serializer = serializer;
        this.config = config;
    }

    public static IpcClient tryConnect(Serializer<?> serializer) {
        String socketPathStr = System.getProperty(PROP_IPC_SOCKET);
        if (socketPathStr == null || socketPathStr.isBlank()) {
            return null;
        }

        Path socketPath = Path.of(socketPathStr);
        if (!Files.exists(socketPath)) {
            throw ExecutionException.noSocketFile(socketPath);
        }

        try {
            try {
                UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
                SocketChannel channel = SocketChannel.open(address);

                // Read the IPC handshake header from the server
                BufferedReader reader = new BufferedReader(
                    Channels.newReader(channel, StandardCharsets.UTF_8)
                );

                IpcConfig config = new IpcConfig();
                while (true) {
                    String line = reader.readLine();
                    if (line.startsWith(IpcConfig.HEADER_SEPARATOR)) {
                        break;
                    }
                    config.set(line);
                }

                BufferedWriter writer = new BufferedWriter(
                    Channels.newWriter(channel, StandardCharsets.UTF_8)
                );
                return new IpcClient(channel, writer, serializer, config);
            } catch (IOException e) {
                throw new LocalizableIOException(e);
            }
        } catch (Exception e) {
            throw ExecutionException.connectionFailed(e, e.getMessage());
        }
    }

    public long getServerProcessId() {
        return config.ipcServerPid();
    }

    public Serializer<?> getSerializer() {
        return serializer;
    }

    public synchronized void send(Object message) throws IOException {
        if (message == null) return;

        String className = message.getClass().getName();
        String json = serializer.toString(message);
        if (json == null) {
            return;
        }
        String payloadJson = json.replace('\n', ' ');

        try {
            String line = className + IpcConfig.PAYLOAD_SEPARATOR + payloadJson + "\n";

            if (config.ipcDebug()) {
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
