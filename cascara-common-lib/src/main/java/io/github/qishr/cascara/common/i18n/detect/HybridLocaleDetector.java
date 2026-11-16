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

package io.github.qishr.cascara.common.i18n.detect;

import java.util.Locale;

import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;

public class HybridLocaleDetector {
    private static final Reporter REPORTER = GlobalReporter.forClass(HybridLocaleDetector.class);

    public static Locale detectOSLocale() {
        Locale locale;

        // Step 1: Check environment variables
        locale = EnvVarLocaleDetector.detect();
        if (locale != null) {
            REPORTER.debug("Found locale via EnvVarLocaleDetector: " + locale.toLanguageTag());
            return locale;
        }

        // TODO: Move this to its own module and get the Windows side working

        // There should probalby be something like:
        //   cascara.platform
        //   cascara.platform.linux
        //   cascara.platform.macos
        //   cascara.platform.windows

        // Or, since we already have cascara.macos:
        //   cascara.common - handle the loading of platform-dependent things
        //   cascara.linux
        //   cascara.macos
        //   cascara.windows

        // https://www.javathinking.com/blog/how-to-detect-operating-system-language-locale-from-java-code/

        // Step 2: Platform-specific detection
        String os = System.getProperty("os.name").toLowerCase();
        // if (os.contains("win")) {
        //     locale = WindowsRegistryLocaleDetector.detectFromRegistry();
        // } else
        if (os.contains("nix") || os.contains("nux")) {
            locale = LinuxFileLocaleDetector.detect();
            if (locale != null) {
                REPORTER.debug("Found locale via LinuxFileLocaleDetector: " + locale.toLanguageTag());
                return locale;
            }
        } else if (os.contains("mac")) {
            locale = MacOSLocaleDetector.detect();
            if (locale != null) {
                REPORTER.debug("Found locale via MacOSLocaleDetector: " + locale.toLanguageTag());
                return locale;
            }
        }

        // Step 3: Fallback to JVM default
        locale = Locale.getDefault();
        REPORTER.debug("Using JVM default locale: " + locale.toLanguageTag());
        return locale;
    }
}