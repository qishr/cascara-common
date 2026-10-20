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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcConfig;
import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.lang.processor.Serializer;

public abstract class AbstractExecutionTask<I, O> {

    protected abstract O run(I input) throws Exception;

    public static void main(String[] args) {
        String command = System.getProperty("sun.java.command");

        // 1. Isolate the main class token (before any application args)
        String mainToken = command.contains(" ") ? command.split(" ")[0] : command;

        // 2. Strip JPMS module prefix if present (e.g., "module.name/package.ClassName" -> "package.ClassName")
        String targetClassName = mainToken.contains("/")
            ? mainToken.substring(mainToken.indexOf('/') + 1)
            : mainToken;

        try {
            // Find the concrete subclass that was passed to the 'java' command
            Class<?> targetClass = Class.forName(targetClassName);

            // Instantiate the subclass
            @SuppressWarnings("unchecked")
            AbstractExecutionTask<Object, Object> task =
                (AbstractExecutionTask<Object, Object>) targetClass.getDeclaredConstructor().newInstance();

            // Execute the lifecycle (read stdin -> run task -> write to IPC/stdout)
            task.executeLifecycle(targetClass);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new ExecutionException(e, DiagnosticMessage.forException(e), targetClassName);
        }
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
            throw new ExecutionException(e, ExecutionMessage.PROCESS_FAILED, targetClass.getName());
        }

        String json = sb.toString();

        Serializer<?> serializer = IpcConfig.createSerializer();

        I object = null;
        if (!json.isBlank()) {
            object = (I) serializer.fromString(json, inputClass);
        }

        O response = null;
        if (object == null) {
            // TODO: ExecutionException
            error("Failed to deserialize payload: " + json);
            return;
        } else {
            try {
                response = run(object);
            } catch (Exception e) {
                // TODO: ExecutionException
                error(e, e.getMessage());
            }
        }

        // Emit task output payload via IpcClient if IPC is enabled, otherwise fallback to stdout
        if (response != null) {
            try (IpcClient ipcClient = IpcClient.tryConnect(serializer)) {
                if (ipcClient != null) {
                    ipcClient.send(response);
                }else {
                    System.out.println(serializer.toString(response));
                }
            } catch (IOException e) {
                error(e, "Failed to transmit task output over IPC: " + e.getMessage());
                // Fallback to stdout if socket transmit fails
                System.out.println(serializer.toString(response));
            }
        }
    }

    protected void error(String msg) {
        error(null, msg);
    }

    protected void error(Throwable cause, String msg) {
        System.err.println(msg);
    }
}