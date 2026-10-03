package io.github.qishr.cascara.common.exec;

import java.io.IOException;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Beta;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.lang.diagnostic.ParserException;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.type.TypeReference;
import io.github.qishr.cascara.common.lang.util.ProcessorFactory;

@Beta
public class IsolatedExecutor {
    public static final String SERIALIZATION_FORMAT = "application/json";

    private final JvmProcess jvmProcess;
    private final Class<?> responseClass;
    private final Class<? extends AbstractExecutionTask<?,?>> task;

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

    public <T> Response<T> run(Object input) throws IOException, InterruptedException {
        return run(input, null);
    }

    public <T> Response<T> run(Object input, JvmOptions options) {
        if (options != null) {
            setOptions(options);
        }
        Serializer<?> serializer = new ProcessorFactory().createSerializer(SERIALIZATION_FORMAT);
        String json = serializer.toString(input);

        JvmProcess.Response jvmResponse = jvmProcess.run(json);

        T responsePayload;
        List<Diagnostic> diagnostics;
        try {
            @SuppressWarnings("unchecked")
            T r = (T) serializer.fromString(jvmResponse.out, responseClass);
            responsePayload = r;

            diagnostics = serializer.fromString(jvmResponse.err, new TypeReference<List<Diagnostic>>() {});
        } catch (ParserException e) {
            throw new ExecutionException(ExecutionMessage.OUTPUT_FAILED, task.getName());
        }

        return new Response<T>(jvmResponse.command, jvmResponse.jvmOptions, task, responsePayload, diagnostics, !jvmResponse.timedOut && jvmResponse.exitCode == 0);
    }

    public IsolatedExecutor setOptions(JvmOptions options) {
        jvmProcess.setOptions(options);
        return  this;
    }

    public static class Response<T> {
        private final List<String> command;
        private final JvmOptions jvmOptions;
        private final Class<? extends AbstractExecutionTask<?,?>> task;
        private final T payload;
        private final List<Diagnostic> diagnostics;
        private final boolean success;

        Response(List<String> command, JvmOptions jvmOptions, Class<? extends AbstractExecutionTask<?,?>> task, T payload, List<Diagnostic> diagnostics, boolean success) {
            this.command = command;
            this.jvmOptions = jvmOptions;
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
    }
}
