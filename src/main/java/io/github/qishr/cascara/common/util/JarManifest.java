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


package io.github.qishr.cascara.common.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.semver.SemVer;

public class JarManifest extends Properties {
    private JarManifest() {
        // No public constructor
    }

    /// Parses the content of a MANIFEST.MF file string, handles line continuations,
    /// and prints the resulting property names and values.
    ///
    /// @param manifest The string containing the MANIFEST.MF file content.
    public static JarManifest parse(String manifest) {
        JarManifest manifestProperties = new JarManifest();
        Map<String, StringBuilder> mf = new LinkedHashMap<>();
        String currentKey = null;
        try (BufferedReader reader = new BufferedReader(new StringReader(manifest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    // Empty line signifies the end of the main section or an entry block
                    currentKey = null;
                    continue;
                }
                if (line.startsWith(" ")) {
                    // A line starting with a space (0x20) is a continuation of the previous line's value.
                    if (currentKey != null) {
                        // Append the continuation line, trimming the leading space (0x20)
                        String continuation = line.substring(1);
                        mf.get(currentKey).append(continuation);
                    }
                    // Note: If currentKey is null here, it's an improperly formatted continuation line, so we ignore it.
                } else {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        currentKey = key;
                        mf.put(key, new StringBuilder(value));
                    }
                }
            }
        } catch (Exception e) {
            throw new LocalizableRuntimeException(e, GenericDiagnosticCode.MANIFEST_READ, e.getMessage());
        }

        for (Map.Entry<String, StringBuilder> entry : mf.entrySet()) {
            manifestProperties.set(entry.getKey(), entry.getValue().toString());
        }

        return manifestProperties;
    }

    public static JarManifest of(Class<?> clazz) {
        InputStream is;
        try {
            is = JreUtils.getResourceAsStream(clazz, "/META-INF/MANIFEST.MF");
        } catch (LocalizableIOException e) {
            throw new LocalizableRuntimeException(
                e,
                GenericDiagnosticCode.MANIFEST_READ,
                clazz.getSimpleName()
            );
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String content = br.lines().collect(Collectors.joining("\n"));
            return parse(content);
        } catch (Exception e) {
            throw new LocalizableRuntimeException(
                e,
                GenericDiagnosticCode.MANIFEST_READ,
                clazz.getSimpleName()
            );
        }
    }

    public SemVer getVersion() {
        return new SemVer(getString("Implementation-Version", "0.0.0"));
    }
}
