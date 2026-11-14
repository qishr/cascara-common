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

import java.io.File;
import java.io.FileReader;
import java.util.Locale;
import java.util.Scanner;

public class LinuxFileLocaleDetector {
    @SuppressWarnings("deprecation")
    public static Locale detect() {
        File localeFile = new File("/etc/default/locale");
        if (!localeFile.exists()) return null;

        try (Scanner scanner = new Scanner(new FileReader(localeFile))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("LANG=")) {
                    String lang = line.split("=")[1].replace("\"", ""); // e.g., "en_US.UTF-8"
                    String[] parts = lang.split("[._]");
                    return new Locale(parts[0], parts.length > 1 ? parts[1] : "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}