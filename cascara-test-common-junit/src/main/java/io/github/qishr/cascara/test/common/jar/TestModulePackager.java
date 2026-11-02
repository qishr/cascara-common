package io.github.qishr.cascara.test.common.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;

public class TestModulePackager {

    public static SyntheticJar packageJar(SyntheticJar testJar) {
        // 1. Compile source code in memory
        if (testJar.classes.isEmpty()) {
            testJar.classes = TestModuleCompiler.compileSources(testJar);
        }

        // 2. Build manifest attributes
        Manifest manifest = null;

        if (testJar.signedBundle == null) {
            manifest = new Manifest();
        } else {
            try {
                manifest = new Manifest(new ByteArrayInputStream(testJar.signedBundle.manifestMf()));
            } catch (IOException e) {
                throw new TestJarException(e, GenericMessage.ERROR, e.getMessage());
            }
        }

        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (testJar.minCascaraVersion != null) {
            manifest.getMainAttributes().putValue("Min-Cascara-Version", testJar.minCascaraVersion);
        }

        // 3. Stream compiled entries directly into the ZipFS destination path
        try (OutputStream os = Files.newOutputStream(testJar.path);
             JarOutputStream jos = new JarOutputStream(os, manifest)) {
            JarEntry jarEntry;

            if (testJar.signedBundle != null) {
                jarEntry = new JarEntry("META-INF/TEST.SF");
                jos.putNextEntry(jarEntry);
                jos.write(testJar.signedBundle.manifestSf());
                jos.closeEntry();

                // jarEntry = new JarEntry("META-INF/TEST.RSA");

                String keyFileName = testJar.signedBundle.algorithm() == null
                    ? "META-INF/TEST.RSA"
                    : "META-INF/TEST." + testJar.signedBundle.algorithm().getKeyAlgorithm();
                jarEntry = new JarEntry(keyFileName);

                jos.putNextEntry(jarEntry);
                jos.write(testJar.signedBundle.key());
                jos.closeEntry();
            }

            for (Map.Entry<String, byte[]> entry : testJar.classes.entrySet()) {
                jarEntry = new JarEntry(entry.getKey());
                jos.putNextEntry(jarEntry);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        } catch (IOException e) {
            throw new TestJarException(e, GenericMessage.ERROR, e.getMessage());
        }
        return testJar;
    }
}