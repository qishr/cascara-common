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

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public enum ExecutionMessage implements DiagnosticMessage {
    PROCESS_FAILED("EXEC-101", "Failed to start process for {0}"),
    TASK_FAILED("EXEC-102", "Task {0} failed"),
    INPUT_FAILED("EXEC-103", "Failed to write input to task {0}"),
    OUTPUT_FAILED("EXEC-104", "Failed to read output of task {0}.\n System.out: \"{1}\"\n System.err: \"{2}\""),
    INTERRUPT("EXEC-105", "Task {0} was interrupted"),
    DIAGNOSTIC_SERVER_FAILED("EXEC-106", "Failed to initialize DiagnosticIpcServer for task {0}"),
    DIAGNOSTIC_DESERIALIZATION_FAILED("EXEC-107", "Failed to deserialize diagnostics for task {0}"),
    IPC_RECV_FAILED("EXEC-108", "IPC receive failed: {0}"),
    NO_SOCKET_FILE("EXEC-109", "Socket file does not exist: {0}"),
    CONNECT_FAILED("EXEC-110", "IPC connection failed: {0}");

    private final String code;
    private final String format;

    ExecutionMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}