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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcConfig;
import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.type.cascara.LocalizableMessageDescriptor;
import io.github.qishr.cascara.common.type.cascara.SignatureExceptionSerializer;
import io.github.qishr.cascara.common.type.lang.ThrowableSerializer;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public abstract class AbstractExecutionTask<I, O> {
    protected abstract O run(I input) throws Throwable;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String command = System.getProperty("sun.java.command");

        // 1. Isolate the main class token (before any application args)
        String mainToken = command.contains(" ") ? command.split(" ")[0] : command;

        // 2. Strip JPMS module prefix if present (e.g., "module.name/package.ClassName" -> "package.ClassName")
        String targetClassName = mainToken.contains("/")
            ? mainToken.substring(mainToken.indexOf('/') + 1)
            : mainToken;

        Class<AbstractExecutionTask<Object, Object>> targetClass;
        AbstractExecutionTask<Object, Object> task;
        try {
            // Find the concrete subclass that was passed to the 'java' command
            targetClass = (Class<AbstractExecutionTask<Object, Object>>) Class.forName(targetClassName);
            task = ReflectionUtils.createInstance(targetClass);
        } catch (InstantiationException | IllegalArgumentException | SecurityException | ClassNotFoundException e) {
            throw new ExecutionException(e, DiagnosticMessage.forException(e), targetClassName);
        }

        // Execute the lifecycle (read stdin -> run task -> write to IPC/stdout)
        task.executeLifecycle(targetClass);
    }

    @SuppressWarnings("unchecked")
    public void executeLifecycle(Class<?> targetClass) {
        Class<I> inputClass = (Class<I>) extractInputClass(getClass());
        run(targetClass, inputClass);
    }

    private static Class<?> extractInputClass(Class<?> clazz) {
        Type superclass = clazz.getGenericSuperclass();

        while (superclass != null) {
            if (superclass instanceof ParameterizedType parameterizedType) {
                // Return the first generic argument (<I, O>)
                Type firstTypeArg = parameterizedType.getActualTypeArguments()[0];
                if (firstTypeArg instanceof Class<?> typeClass) {
                    return typeClass;
                }
            }
            // Walk up the hierarchy if subclassed further
            Class<?> rawType = (Class<?>) ((superclass instanceof ParameterizedType pt)
                    ? pt.getRawType()
                    : superclass);
            superclass = rawType.getGenericSuperclass();
        }

        throw new ExecutionException(LangMessage.UNKNOWN_GENERIC_TYPE, clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public void run(Class<?> targetClass, Class<?> inputClass) {
        Throwable thrownByTask = null;
        O response = null;

        try {
            // TODO: Make this configurable via -D
            // same for spl boot errors to system.err

            GlobalReporter.globalInstance().setSystemOutputEnabled(false);

            GlobalReporter.globalInstance().setStackTraceEnabled(true);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, count);
                }
            } catch (IOException e) {
                throw new ExecutionException(e, ExecutionMessage.PROCESS_NOT_STARTED, targetClass.getName());
            }

            String json = sb.toString();

            Serializer<?> serializer = IpcConfig.createSerializer();
            if (serializer == null) {
                // TODO: Better exception
                throw new ExecutionException(GenericMessage.ERROR,"Serializer not available");
            }

            // Register ThrowableSeriaizer and LocalizableMessageDescriptor
            // so that even if SPL isn't evailable, exceptions and
            // diagnostic messages can still be serialized.
            serializer.registerTypeDescriptor(new ThrowableSerializer());
            serializer.registerTypeDescriptor(new LocalizableMessageDescriptor());
            serializer.registerTypeDescriptor(new SignatureExceptionSerializer());

            I object = null;
            if (!json.isBlank()) {
                Object deserialized = serializer.fromString(json, inputClass);
                object = (I) deserialized;
            }

            if (object == null) {
                // TODO: ExecutionException
                error("Failed to deserialize payload: " + json);
                return;
            } else {
                try {
                    response = run(object);
                } catch (Throwable e) {
                    thrownByTask = e;
                }
            }

            // Emit task output payload via IpcClient if IPC is enabled, otherwise fallback to stdout
            if (response != null || thrownByTask != null) {
                try (IpcClient ipcClient = IpcClient.tryConnect(serializer)) {
                    if (ipcClient != null) {
                        if (thrownByTask != null) {
                            ipcClient.send(thrownByTask);
                        } else {
                            ipcClient.send(response);
                        }
                    }else {
                        System.out.println(serializer.toString(response));
                    }
                } catch (IOException e) {
                    error(e, "Failed to transmit task output over IPC: " + e.getMessage());
                    // Fallback to stdout if socket transmit fails
                    System.out.println(serializer.toString(response));
                }
            }
        } catch (Throwable t) {
            error("Exception during task initialization: " + t.getLocalizedMessage());
        }
    }

    protected void displayHomeContents() {
        try(Stream<Path> jarFiles = Files.walk(Cascara.instance().getHomePath())) {
            for (Path jarFile : jarFiles.toList()) {
                System.out.println("> " + jarFile);
            }
        } catch (IOException e) {
            error(e, "Failed to display VFS contents");
        }
    }

    protected void error(String msg) {
        error(null, msg);
    }

    protected void error(Throwable cause, String msg) {
        System.err.println(cause == null
            ? msg
            : (cause.getClass().getSimpleName() + ": " + msg));
    }
}