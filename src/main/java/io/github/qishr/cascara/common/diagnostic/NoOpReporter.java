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


package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;

public class NoOpReporter implements Reporter {

    public NoOpReporter(Consumer<String> writer) {
    }

    public NoOpReporter() {
        // Nothing to see here
    }

    @Override
    public boolean collectsProblems() {
        return false;
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Override
    public NoOpReporter setLevel(Level level) {
        return this;
    }

    @Override
    public NoOpReporter setDiagnosticCollector(Consumer<Diagnostic> diagnosticCollector) {
        return this;
    }

    @Override
    public NoOpReporter setProblemCollector(Consumer<Diagnostic> diagnosticCollector) {
        return this;
    }

    public NoOpReporter setDisableSystemOutput(boolean b) {
        return this;
    }

    //
    // Exception
    //

    @Override
    public void error(LocalizableException e) {}

    @Override
    public void error(LocalizableRuntimeException e) {}

    //
    // Plain
    //

    @Override
    public void trace(String format, Object... args) {}

    @Override
    public void debug(String format, Object... args) {}

    @Override
    public void info(DiagnosticCode code, Object... args) {}

    @Override
    public void warn(DiagnosticCode code, Object... args) {}

    @Override
    public void error(DiagnosticCode code, Object... args) {}

    @Override
    public void error(Throwable cause, DiagnosticCode code, Object... args) {}

    //
    // With Location
    //

    @Override
    public void infoAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(URI uri, int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... args) {}

    //
    // With Location invluding offset
    //

    @Override
    public void infoAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, int start, int end, Throwable cause, DiagnosticCode code, Object... args) {}

    //
    // With Token
    //

    @Override
    public void infoAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(Token token, Throwable cause, DiagnosticCode code, Object... args) {}

}
