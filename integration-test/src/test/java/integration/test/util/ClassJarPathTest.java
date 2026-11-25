package integration.test.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.util.Classes;
import io.github.qishr.cascara.test.common.junit.base.TestBase;

@Isolated
public class ClassJarPathTest extends TestBase {
    @Test
    void test_jarPath() {

        Path jarPath = Classes.getJarPath(SplSignaturePolicy.class.getName());
        assertNotNull(jarPath);
    }
}