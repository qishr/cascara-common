package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.policy.SPLSignaturePolicy;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Classes;
import io.github.qishr.cascara.common.util.JarFile;

public class SignedCascaraModuleTests {
    @Test
    @Tag("requires-jpms")
    void verifyCascaraCommonJar() throws Exception {
        // 1. Check for Gradle-supplied signing properties; skip if missing
        String keystorePathStr = System.getProperty("cascaraSigningKeystore");
        String storePassStr = System.getProperty("cascaraSigningStorePass");

        assumeTrue(keystorePathStr != null && !keystorePathStr.isBlank(),
                "Skipping test: 'cascaraSigningKeystore' property is not configured");
        assertTrue(storePassStr != null && !storePassStr.isBlank(),
                "Skipping test: 'cascaraSigningStorePass' property is not configured");

        Path keystorePath = Path.of(keystorePathStr);
        assumeTrue(Files.exists(keystorePath),
                "Skipping test: Keystore file does not exist at " + keystorePath.toAbsolutePath());

        // 2. Resolve the packaged cascara-common JAR on the runtime classpath
        Path jarPath = Classes.getJarPath(Cascara.class.getName());
        assertNotNull(jarPath, "cascara-common JAR path must be resolvable");
        assertTrue(jarPath.toString().endsWith(".jar"), "Resolved path must point to a packaged JAR file");

        // 3. Load TrustStore using Gradle's credentials & evaluate policy
        TrustStore trustStore = TrustStore.load(keystorePath, storePassStr.toCharArray());

        SPLSignaturePolicy policy = SPLSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(trustStore)
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            assertEquals("cascara.common", jar.getModuleName(), "Module name must match 'cascara.common'");

            // Read entry bytes to populate certs
            byte[] content = jar.extractFile("module-info.class");
            assertNotNull(content, "module-info.class content must not be null");

            JarEntry entry = jar.getJarEntry("module-info.class");
            assertNotNull(entry.getCertificates(), "Certificates must be populated on JarEntry after reading");

            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.VALID, status, "cascara-common JAR signature evaluation must be VALID");
        }
    }
}