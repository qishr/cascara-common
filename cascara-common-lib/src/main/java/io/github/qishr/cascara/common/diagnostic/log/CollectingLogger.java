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

package io.github.qishr.cascara.common.diagnostic.log;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.format.FormatOptions;

@Experimental
public class CollectingLogger implements Logger {

    private final List<Diagnostic> diagnostics = new CopyOnWriteArrayList<>();

    @Override
    public CollectingLogger log(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
        return this;
    }

    public void flushTo(Logger delegateLogger) {
        diagnostics.stream()
            .sorted(Comparator.comparing(Diagnostic::getTimestamp))
            .forEach(delegateLogger::log);
        delegateLogger.flush();
        diagnostics.clear();
    }

    @Override
    public CollectingLogger flush() {
        // Handled via explicit flushTo at end of test
        return this;
    }

    //
    //
    //

    public CollectingLogger setFormatOptions(FormatOptions options) {
        // formatter.setOptions(options);
        return this;
    }
}