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

package io.github.qishr.cascara.common.exec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Beta;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;
import io.github.qishr.cascara.common.lang.processor.Serializer;

@Beta
public class IsolatedExecutor {
    public static final String SERIALIZATION_FORMAT = "application/json";

    private final JvmProcess jvmProcess;
    private JvmOptions options;
    private final Class<?> responseClass;
    private final Class<? extends AbstractExecutionTask<?,?>> task;
    private boolean ipcDebugEnabled;
    private boolean diagnisticForwaringEnabled;

    private IsolatedExecutor(Class<? extends AbstractExecutionTask<?,?>> task, Class<?> response) {
        this.jvmProcess = JvmProcess.forClass(task);
        this.task = task;
        this.responseClass = response;
    }

    public static IsolatedExecutor forTask(Class<? extends AbstractExecutionTask<?,?>> task, Class<?> response) {
        return new IsolatedExecutor(task, response);
    }

    public static <T> Response<T> run(Class<? extends AbstractExecutionTask<?,?>> task, Object input, Class<?> response, JvmOptions options) {
        IsolatedExecutor exec = IsolatedExecutor.forTask(task, response);
        return exec.run(input, options);
    }

    public IsolatedExecutor setOptions(JvmOptions options) {
        this.options = options;
        return  this;
    }

    public IsolatedExecutor setIpcDebugEnabled(boolean enabled) {
        this.ipcDebugEnabled = enabled;
        return  this;
    }

    public IsolatedExecutor setDiagnosticForwardingEnabled(boolean enabled) {
        this.diagnisticForwaringEnabled = enabled;
        return  this;
    }

    public <T> Response<T> run(Object input) throws IOException, InterruptedException {
        return run(input, null);
    }

    @SuppressWarnings("unchecked")
    public <T> Response<T> run(Object input, JvmOptions options) {
        if (options == null) {
            options = this.options;
        }

        Serializer<?> serializer = new ProcessorFactory().createSerializer(SERIALIZATION_FORMAT);

        String json = serializer.toString(input);

        List<Diagnostic> diagnostics = new ArrayList<>();
        T taskResponse;
        JvmProcess.Response jvmResponse;

        IpcServer ipcServer;
        try {
            ipcServer = IpcServer.start(serializer, diagnisticForwaringEnabled, ipcDebugEnabled);

            options.setSystemProperty(IpcClient.SOCKET_PROP, ipcServer.getSocketPath().toString());
            jvmProcess.setOptions(options);

            jvmResponse = jvmProcess.run(json);

            ipcServer.close();

            Class<T> targetClass = (Class<T>) responseClass;
            List<T> outputs = ipcServer.getMessages(targetClass);
            taskResponse = outputs.isEmpty() ? null : outputs.getFirst();

            diagnostics = ipcServer.getMessages(Diagnostic.class);
        } catch (IOException e) {
            throw new ExecutionException(e, ExecutionMessage.DIAGNOSTIC_SERVER_FAILED, task.getName());
        }

        return new Response<T>(jvmResponse, task, taskResponse, diagnostics, !jvmResponse.timedOut && jvmResponse.exitCode == 0);
    }

    public static class Response<T> {
        private final List<String> command;
        private final JvmOptions jvmOptions;
        private final String out;
        private final String err;
        private final Class<? extends AbstractExecutionTask<?,?>> task;
        private final T payload;
        private final List<Diagnostic> diagnostics;
        private final boolean success;

        Response(JvmProcess.Response jvmResponse, Class<? extends AbstractExecutionTask<?,?>> task, T payload, List<Diagnostic> diagnostics, boolean success) {
            this.command = jvmResponse.command;
            this.jvmOptions = jvmResponse.jvmOptions;
            this.out = jvmResponse.out;
            this.err = jvmResponse.err;
            this.task = task;
            this.payload = payload;
            this.diagnostics = diagnostics;
            this.success = success;
        }

        public T getPayload() {
            return payload;
        }

        public T orElseThrow() {
            if (success) {
                return payload;
            }
            throw new ExecutionException(ExecutionMessage.TASK_FAILED, task.getName(), diagnostics);
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        public boolean isSuccess() {
            return success;
        }

        public List<String> getCommand() {
            return command;
        }

        public JvmOptions getJvmOptions() {
            return jvmOptions;
        }

        public String getSystemOut() {
            return out;
        }

        public String getSystemErr() {
            return err;
        }
    }
}
