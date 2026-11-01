package integration.test.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.util.Classes;

public class ClassJarPathTest {
    @Test
    void test_jarPath() {

        Path jarPath = Classes.getJarPath(SplSignaturePolicy.class.getName());
        assertNotNull(jarPath);
    }
}