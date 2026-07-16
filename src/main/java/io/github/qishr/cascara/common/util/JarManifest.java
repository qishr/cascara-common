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
            is = JreUtil.getResourceAsStream(clazz, "/META-INF/MANIFEST.MF");
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
