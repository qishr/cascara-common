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

import java.io.InputStream;

import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.ConfigExtends;
import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Config;

@ConfigExtends(Cascara.class)
@ConfigNamespace("cascara.log.console")
public class ConsoleLoggerConfig extends Config {
    @ConfigSetting
    private boolean immediateFlush = true;

    @ConfigSetting
    private boolean ansiColoring = true;

    @ConfigSetting
    private boolean systemError = true;

    public static ConsoleLoggerConfig get() {
        return get(ConsoleLoggerConfig.class);
    }

    public static ConsoleLoggerConfig get(InputStream is) {
        return get(ConsoleLoggerConfig.class, is);
    }

    public boolean isImmediateFlushEnabled() {
        return immediateFlush;
    }

    public boolean isAnsiColoringEnabled() {
        return ansiColoring;
    }

    public boolean isSystemErrorEnabled() {
        return systemError;
    }

    /// Controls whether diagnostics logged to console are flushed immediately.
    public ConsoleLoggerConfig setImmediateFlushEnabled(boolean b) {
        immediateFlush = b;
        return this;
    }

    /// Controls whether errors logged to console go to System.out or System.err.
    public ConsoleLoggerConfig setSystemErrorEnabled(boolean b) {
        systemError = b;
        return this;
    }

    /// Controls whether diagnostics logged to console use ANSI coloring.
    public ConsoleLoggerConfig setAnsiColoringEnabled(boolean b) {
        ansiColoring = b;
        return this;
    }
}
