package io.github.qishr.cascara.common.exec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class JvmProcess {

    private final Class<?> mainClass;
    private JvmOptions jvmOptions = new JvmOptions();

    private JvmProcess(Class<?> mainClass) {
        this.mainClass = mainClass;
    }

    public static JvmProcess forClass(Class<?> mainClass) {
        return new JvmProcess(mainClass);
    }

    public JvmProcess setOptions(JvmOptions options) {
        this.jvmOptions = options;
        return  this;
    }

    /// Synchronous blocking execution (Preserved signature for backwards compatibility).
    public Response run() throws IOException, InterruptedException {
        return run(null);
    }

    /// Executes the process synchronously with optional standard input,
    /// blocking the calling thread until completion.
    ///
    /// @param input standard input payload to stream to the child process, or null
    /// @return the process execution response payload
    /// @throws ExecutionException if process launch, input writing, or execution fails
    public Response run(String input) {
        try {
            return runAsync(input).get();
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof ExecutionException ex) {
                throw ex;
            }
            throw new ExecutionException(e.getCause(), ExecutionMessage.PROCESS_FAILED, mainClass.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e, ExecutionMessage.INTERRUPT, mainClass.getName());
        }
    }

    /// Non-blocking background execution.
    public CompletableFuture<Response> runAsync() {
        return runAsync(null);
    }

    /// Non-blocking background execution with input.
    public CompletableFuture<Response> runAsync(String input) {

        List<String> command = buildCommand();
        ProcessBuilder pb = new ProcessBuilder(command);

        if (!jvmOptions.getEnv().isEmpty()) {
            pb.environment().putAll(jvmOptions.getEnv());
        }

        if (jvmOptions.debug()) {
            System.out.println("JvmProcess Command:");
            for (String s : command) {
                System.out.println("  " + s);
            }
            System.out.println("JvmProcess Environment:");
            for (Entry<String, String> entry : pb.environment().entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue());
            }
        }

        long startTime = System.nanoTime();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(
                new io.github.qishr.cascara.common.exec.ExecutionException(
                    e,
                    ExecutionMessage.PROCESS_FAILED,
                    mainClass.getName()
                )
            );
        }

        // Read streams concurrently to prevent OS stream buffer deadlocks
        var stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        var stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

        if (input != null && !input.isEmpty()) {
            try (var inputStream = process.getOutputStream()) {
                inputStream.write(input.getBytes(StandardCharsets.UTF_8));
                inputStream.flush();
            } catch (IOException e) {
                process.destroyForcibly();
                return CompletableFuture.failedFuture(
                    new io.github.qishr.cascara.common.exec.ExecutionException(
                        e,
                        ExecutionMessage.INPUT_FAILED,
                        mainClass.getName(),
                        null
                    )
                );
            }
        }

        long timeoutMs = jvmOptions.getTimeout().toMillis();
        CompletableFuture<Process> processCompletion = process.onExit();

        if (timeoutMs > 0) {
            processCompletion = processCompletion.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    process.destroyForcibly();
                    return process;
                });
        }

        return processCompletion.thenCompose(p ->
            CompletableFuture.allOf(stdoutFuture, stderrFuture).thenApply(v -> {
                long elapsedNanos = System.nanoTime() - startTime;
                boolean timedOut = !p.isAlive() && timeoutMs > 0 && (elapsedNanos / 1_000_000) >= timeoutMs;
                int exitCode = timedOut ? -1 : p.exitValue();

                return new Response(
                    command,
                    jvmOptions,
                    exitCode,
                    timedOut,
                    stdoutFuture.join(),
                    stderrFuture.join(),
                    Duration.ofNanos(elapsedNanos)
                );
            })
        );
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        command.add(javaBin);

        jvmOptions.getSystemProperties().forEach((k, v) -> {
            if (v == null || v.isBlank()) {
                command.add("-D" + k);
            } else {
                command.add("-D" + k + "=" + v);
            }
        });

        String modulePath = jvmOptions.getModulePath();
        if (modulePath != null && !modulePath.isBlank()) {
            command.add("--module-path");
            command.add(modulePath);
        }

        String classPath = jvmOptions.getClassPath() != null
            ? jvmOptions.getClassPath()
            : (jvmOptions.inheritParentClassPath() ? System.getProperty("java.class.path") : null);

        if (classPath != null && !classPath.isBlank()) {
            command.add("-cp");
            command.add(classPath);
        }

        for (String mod : jvmOptions.getModules()) {
            command.add("--add-modules");
            command.add(mod);
        }
        for (String openTarget : jvmOptions.getOpens()) {
            command.add("--add-opens");
            command.add(openTarget);
        }
        for (String readTarget : jvmOptions.getReads()) {
            command.add("--add-reads");
            command.add(readTarget);
        }

        String moduleName = jvmOptions.getModuleName();
        if (moduleName != null && !moduleName.isBlank()) {
            command.add("--module");
            command.add(moduleName + "/" + mainClass.getName());
        } else {
            command.add(mainClass.getName());
        }

        command.addAll(jvmOptions.getArgs());
        return command;
    }

    private static String readStream(java.io.InputStream is) {
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public static class Response {
        public final List<String> command;
        public final JvmOptions jvmOptions;
        public final int exitCode;
        public final boolean timedOut;
        public final String out;
        public final String err;
        public final Duration duration;

        Response( List<String> command, JvmOptions jvmOptions, int exitCode, boolean timedOut, String out, String err, Duration duration) {
            this.command = command;
            this.jvmOptions = jvmOptions;
            this.exitCode = exitCode;
            this.timedOut = timedOut;
            this.out = out;
            this.err = err;
            this.duration = duration;
        }
    }
}
