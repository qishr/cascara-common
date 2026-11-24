package integration.test.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.service.SPLSignaturePolicy;
import io.github.qishr.cascara.common.util.Classes;

@Isolated
public class ClassJarPathTest {
    @Test
    void test_jarPath() {

        Path jarPath = Classes.getJarPath(SPLSignaturePolicy.class.getName());
        assertNotNull(jarPath);
    }
}