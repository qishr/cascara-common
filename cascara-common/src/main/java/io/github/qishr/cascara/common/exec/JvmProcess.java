package io.github.qishr.cascara.common.exec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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

    public Response run() throws IOException, InterruptedException {
        return run(null);
    }

    public Response run(String input) {
        List<String> command = new ArrayList<>();

        // 1. Executable Java binary
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        command.add(javaBin);

        // 2. System properties (-D)
        jvmOptions.getSystemProperties().forEach((k, v) -> command.add("-D" + k + "=" + v));

        // 3. Module path vs. Classpath
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

        // 4. Encapsulation and Access Directives
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

        // 5. Entry Point: Determined strictly by whether a module name is targeted
        String moduleName = jvmOptions.getModuleName();
        if (moduleName != null && !moduleName.isBlank()) {
            // Modular execution: java --module moduleName/mainClassName
            command.add("--module");
            command.add(moduleName + "/" + mainClass.getName());
        } else {
            // Standard classpath execution: java mainClassName
            command.add(mainClass.getName());
        }

        command.addAll(jvmOptions.getArgs());

        ProcessBuilder pb = new ProcessBuilder(command);

        // Environment variables
        if (!jvmOptions.getEnv().isEmpty()) {
            pb.environment().putAll(jvmOptions.getEnv());
        }

        if (jvmOptions.debug()) {
            System.out.println("JvmProcess Command:");
            for (String s : command) {
                System.out.println("  " + s);
            }
            System.out.println("JvmProcess Environment:");
            for (Entry<String,String> entry : pb.environment().entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue());
            }
        }

        long startTime = System.nanoTime();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ExecutionException(e, ExecutionMessage.PROCESS_FAILED, mainClass.getName());
        }

        // Capture stdout and stderr asynchronously to prevent OS buffer deadlocks
        var stdoutStream = process.getInputStream();
        var stderrStream = process.getErrorStream();

        if (input != null && !input.isEmpty()) {
            // System.out.println("Sending: " + input);
            var inputStream = process.getOutputStream();
            try {
                inputStream.write(input.getBytes());
                inputStream.flush();
                inputStream.close();
            } catch (IOException e) {
                throw new ExecutionException(e, ExecutionMessage.INPUT_FAILED, mainClass.getName(), null);
            }
        }

        boolean completed;
        try {
            completed = process.waitFor(jvmOptions.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ExecutionException(e, ExecutionMessage.INTERRUPT, mainClass.getName(), null);
        }
        long elapsedNanos = System.nanoTime() - startTime;


        if (!completed) {
            process.destroyForcibly();

            String outResponse = "";
            String errResponse = "";

            try {
                outResponse = new String(stdoutStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // TODO: We probably want to throw an ExecutionException
            }
            try {
                errResponse = new String(stderrStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // TODO: We probably want to throw an ExecutionException
            }

            return new Response(
                command,
                jvmOptions,
                -1,
                true,
                outResponse,
                errResponse,
                Duration.ofNanos(elapsedNanos)
            );
        }

        String outResponse = "";
        String errResponse = "";

        try {
            outResponse = new String(stdoutStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("TestProcess error reading stdout: " + e.getMessage());
        }
        try {
            errResponse = new String(stderrStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("TestProcess error reading stderr: " + e.getMessage());
        }

        return new Response(
            command,
            jvmOptions,
            process.exitValue(),
            false,
            outResponse,
            errResponse,
            Duration.ofNanos(elapsedNanos)
        );

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
