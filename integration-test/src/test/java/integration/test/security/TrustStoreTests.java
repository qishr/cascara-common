package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.test.common.junit.base.TestBase;

class TrustStoreTests extends TestBase {

    private X509Certificate validCert;
    private X509Certificate expiredCert;

    private static void saveToKeyStore(Path path, char[] password, String alias, Certificate cert) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType()); // Defaults to PKCS12 in modern JDKs
        ks.load(null, password); // Initialize a new, empty keystore
        ks.setCertificateEntry(alias, cert);

        try (OutputStream os = Files.newOutputStream(path)) {
            ks.store(os, password);
        }
    }

    //
    //
    //

    @BeforeEach
    protected void setUp() {
        super.setUp();
        validCert = rootCA.issueIdentity("valid-service").certificate();
        expiredCert = rootCA.issueExpiredIdentity("expired-service").certificate();
    }

    //
    //
    //

    @Test
    void test_emptyTrustStore_containsNothing() {
        TrustStore store = TrustStore.empty();
        assertTrue(store.getCertificates().isEmpty());
        assertFalse(store.contains(validCert));
        assertFalse(store.isTrusted(validCert, false));
    }

    @Test
    void test_contains_returnsTrueForExactCert() {
        TrustStore store = new TrustStore(Set.of(validCert));
        assertTrue(store.contains(validCert));
        assertFalse(store.contains(expiredCert));
    }

    @Test
    void test_isTrusted_withCheckExpiry_rejectsExpired() {
        TrustStore store = new TrustStore(Set.of(validCert, expiredCert));

        // Without expiry check: both are present
        assertTrue(store.isTrusted(validCert, false));
        assertTrue(store.isTrusted(expiredCert, false));

        // With expiry check: expired is rejected
        assertTrue(store.isTrusted(validCert, true));
        assertFalse(store.isTrusted(expiredCert, true));
    }

    @Test
    void test_load_fromKeyStoreFile() throws Exception {
        Path ksPath = getTempPath().resolve("truststore.p12");
        char[] password = "changeit".toCharArray();

        // Helper to save rootCA.certificate() to a PKCS12 / JKS file
        saveToKeyStore(ksPath, password, "cascara-root", rootCA.certificate());

        TrustStore store = TrustStore.load(ksPath, password);
        assertEquals(1, store.getCertificates().size());
        assertTrue(store.contains(rootCA.certificate()));
    }
}