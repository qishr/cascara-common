package io.github.qishr.cascara.common.util;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;

public class JarFileTests {
    @Test
    void test_signature() {
        Path path = Path.of("/Users/sandy/security/signedjar.jar");
        try {
            JarFile jar = JarFile.open(path);
        } catch (LocalizableIOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
