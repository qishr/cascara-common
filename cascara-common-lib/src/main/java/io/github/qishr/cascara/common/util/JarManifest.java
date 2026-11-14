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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableIOException;
import io.github.qishr.cascara.common.i18n.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.semver.SemVer;

public class JarManifest extends Manifest {
    public JarManifest() {
    }

    /// Parses the content of a MANIFEST.MF file string, handles line continuations,
    /// and prints the resulting property names and values.
    ///
    /// @param text The string containing the MANIFEST.MF file content.
    public static JarManifest parse(String text) {
        JarManifest manifest = new JarManifest();
        try {
            manifest.read(new ByteArrayInputStream(text.getBytes()));
        } catch (IOException e) {
            // TODO: Proper error message
            throw new LocalizableRuntimeException(e, GenericMessage.ERROR, e.getMessage());
        }
        return manifest;
    }

    public static JarManifest of(Class<?> clazz) {
        InputStream is;
        try {
            is = JreUtils.getResourceAsStream(clazz, "/META-INF/MANIFEST.MF");
        } catch (LocalizableIOException e) {
            throw new LocalizableRuntimeException(
                e,
                GenericMessage.MANIFEST_READ,
                clazz.getSimpleName()
            );
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String content = br.lines().collect(Collectors.joining("\n"));
            return parse(content);
        } catch (Exception e) {
            throw new LocalizableRuntimeException(
                e,
                GenericMessage.MANIFEST_READ,
                clazz.getSimpleName()
            );
        }
    }

    public String getString(String name) {
        return getMainAttributes().getValue(name);
    }

    public String getString(String name, String defaultValue) {
        String v = getMainAttributes().getValue(name);
        return v == null ? defaultValue : v;
    }

    public SemVer getVersion() {
        return new SemVer(getString("Implementation-Version", "0.0.0"));
    }
}
