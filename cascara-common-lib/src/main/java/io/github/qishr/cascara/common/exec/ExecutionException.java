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

import java.nio.file.Path;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableRuntimeException;

public class ExecutionException extends LocalizableRuntimeException {

    private final List<Diagnostic> diagnostics;

    private ExecutionException(LocalizableMessage message) {
        super(message, new Object[]{});
        this.diagnostics = null;
    }

    private ExecutionException(LocalizableMessage message, Object... details) {
        super(message, details);
        this.diagnostics = null;
    }

    private ExecutionException(List<Diagnostic> diagnostics, LocalizableMessage message, Object... details) {
        super(message, details);
        this.diagnostics = diagnostics;
    }

    private ExecutionException(Throwable cause, LocalizableMessage message, Object... details) {
        super(cause, message, details);
        this.diagnostics = null;
    }

    public static ExecutionException noSocketFile(Path socketPath) {
        return new ExecutionException(ExecutionMessage.IPC_NO_SOCKET_FILE, socketPath);
    }

    public static ExecutionException connectionFailed(Throwable cause, String message) {
        return new ExecutionException(cause, ExecutionMessage.IPC_CONNECT_FAILED, message);
    }

    public static ExecutionException receiveFailed(Throwable cause, String message) {
        return new ExecutionException(cause, ExecutionMessage.IPC_RECV_FAILED, message);
    }

    public static ExecutionException diagnosticServerFailed(Throwable cause, String taskName) {
        return new ExecutionException(cause, ExecutionMessage.DIAGNOSTIC_SERVER_FAILED, taskName);
    }

    public static ExecutionException noSerializer() {
        return new ExecutionException(ExecutionMessage.NO_DESERIALIZER);
    }

    public static ExecutionException mainClassError(Throwable cause, String typeName) {
        return new ExecutionException(cause, ExecutionMessage.MAIN_CLASS_ERROR, typeName);
    }

    public static ExecutionException unresolvedInputClass(String typeName) {
        return new ExecutionException(ExecutionMessage.UNRESOLVE_INPUT_CLASS, typeName);
    }

    public static ExecutionException unknownClass(Throwable cause, String typeName) {
        return new ExecutionException(cause, GenericMessage.CLASS_NOT_FOUND, typeName);
    }

    public static ExecutionException processNotStarted(Throwable cause, String mainClass) {
        return new ExecutionException(cause, ExecutionMessage.PROCESS_NOT_STARTED, mainClass);
    }

    public static ExecutionException processNotExited(Throwable cause, String mainClass) {
        return new ExecutionException(cause, ExecutionMessage.PROCESS_NOT_EXITED, mainClass);
    }

    public static ExecutionException processInterrupted(Throwable cause, String mainClass) {
        return new ExecutionException(cause, ExecutionMessage.INTERRUPT, mainClass);
    }

    public static ExecutionException processInputFailed(Throwable cause, String mainClass) {
        return new ExecutionException(cause, ExecutionMessage.INPUT_FAILED, mainClass);
    }

    public static ExecutionException taskFailed(String taskName, List<Diagnostic> diagnostics) {
        return new ExecutionException(diagnostics, ExecutionMessage.TASK_FAILED, taskName);
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
}
