package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import integration.test.jar.JarSignatureValidTests;
import integration.test.util.SourceCode;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.base.TestBase;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;

public class PolicyTests extends TestBase implements SourceCode {

    @BeforeAll
    static void init() {
        GlobalReporter.globalInstance()
            .setLevel(Level.DEBUG)
            .getFormatOptions()
                .setIncludeTime(false)
                .setIncluedPid(false)
                .setIncluedThread(false)
                .setUseSimpleClassName(true);
    }

    @AfterAll
    static void end() {
        GlobalReporter.globalInstance()
            .setLevel(Level.INFO);
    }

    @Test
    void test_module_signing_with_trust_policy() throws Exception {
        // 1. Instantiate Root CA and issue a signing identity
        TestCertificateAuthority ca = TestCertificateAuthority.create("Cascara Root CA");
        TestCertificateAuthority.TestSigningIdentity signer = ca.issueIdentity("provider-module");

        // 2. Build and sign a synthetic module JAR using the issued identity
        SyntheticJar testJar = SyntheticJar.forUseIn(PolicyTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .signWith(signer)
            .packageJar();

        // 3. Verify JAR under a SignaturePolicy backed by the CA's TrustStore
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSigned(true)
            .trustStore(ca.trustStore())
            .build();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            // Evaluates valid signature, cert expiry, and TrustStore anchor directly on the JarFile
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.VALID, status);
        }
    }

    @Test
    void test_invalid_untrustedSigner_signaturePolicy() throws Exception {
        // 1. Create a trusted CA and a foreign CA
        TestCertificateAuthority cascaraCA = TestCertificateAuthority.create("Cascara Root CA");
        TestCertificateAuthority foreignCA = TestCertificateAuthority.create("Foreign Root CA");

        // 2. Sign module with foreign identity
        SyntheticJar foreignSignedJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempDir().resolve("foreign-provider.jar"))
            .addDependencies("cascara-common", "integration-test-fixtures")
            .addSources(sourceCodeA())
            .signWith(foreignCA.issueIdentity("foreign-module"))
            .packageJar();

        // 3. Configure SignaturePolicy requiring trust anchors from Cascara CA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSigned(true)
            .trustStore(cascaraCA.trustStore())
            .build();

        // 4. Opening the JAR under Cascara policy rejects the foreign signer
        try (JarFile jar = JarFile.open(foreignSignedJar.getPath())) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status); // Failure: status is INVALID_SIGNATURE
        }
    }

    @Test
    void test_policy_and_entry_verification() throws Exception {
        // 1. Instantiate Root CA, issue a signer, and package a synthetic JAR
        TestCertificateAuthority ca = TestCertificateAuthority.create("Cascara Root CA");
        TestCertificateAuthority.TestSigningIdentity signer = ca.issueIdentity("provider-module");

        SyntheticJar testJar = SyntheticJar.forUseIn(PolicyTests.class)
            .setPath(getTempDir().resolve("signed-foo.jar"))
            .addDependencies("cascara-common", "integration-test-fixtures")
            .addSources(sourceCodeA())
            .signWith(signer)
            .packageJar();

        // 2. Build SignaturePolicy backed by CA's TrustStore
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSigned(true)
            .trustStore(ca.trustStore())
            .build();

        // 3. Open JarFile and verify entry + policy using standard JRE APIs
        try (JarFile jar = JarFile.open(testJar.getPath())) {
            String entryName = "module-info.class";

            // 1. Reading/extracting the file verifies the bytes against MANIFEST.MF digests under the hood
            byte[] classContent = jar.extractFile(entryName);
            assertNotNull(classContent);

            // 2. Standard JRE JarEntry API: certificates/signers are populated after reading
            JarEntry entry = jar.getJarEntry(entryName);
            assertNotNull(entry);
            assertTrue(Arrays.asList(entry.getCertificates()).contains(signer.certificate()));

            // 3. Evaluate SPL signature policy against the JarFile
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.VALID, status);
        }
    }

    // @Test
    // void test_policy_and_entry_verification() throws Exception {
    //     // 1. Instantiate Root CA and issue a signing identity
    //     TestCertificateAuthority ca = TestCertificateAuthority.create("Cascara Root CA");
    //     TestCertificateAuthority.TestSigningIdentity signer = ca.issueIdentity("provider-module");
    //     TrustStore trustStore = ca.trustStore();
    //     X509Certificate myCaCert = ca.rootCertificate();
    //     // Or is it signer.certificate();?

    //     // TrustStore trustStore = new TrustStore(Set.of(myCaCert));
    //     SplSignaturePolicy policy = SplSignaturePolicy.builder()
    //         .requireSigned(true)
    //         .trustStore(trustStore)
    //         .build();

    //     // ??? There is no signedJarPath yet
    //     try (JarFile jar = JarFile.open(signedJarPath)) {
    //         String entryName = "io/github/qishr/cascara/Foo.class";

    //         // 1. Verify entry bytes against manifest digest
    //         byte[] classContent = jar.extractFile(entryName);

    //         // There is no jar.getVerifier(), as we discusses this morning
    //         boolean digestValid = jar.getVerifier().verifyEntrySignature(entryName, classContent);
    //         assertTrue(digestValid);

    //         // Verify entry was signed by myCaCert via standard JarEntry API
    //         JarEntry entry = jar.getJarEntry(entryName);
    //         assertNotNull(entry);
    //         assertTrue(Arrays.asList(entry.getCertificates()).contains(myCaCert));

    //         // 2. Evaluate SPL policy directly against JarFile
    //         SignatureStatus status = policy.evaluate(jar);
    //         assertEquals(SignatureStatus.VALID, status);
    //     }
    // }


    // @Test
    // void test_policy_and_entry_verification() throws Exception {
    //     TrustStore trustStore = new TrustStore(Set.of(myCaCert));
    //     SignaturePolicy policy = SignaturePolicy.builder()
    //         .requireSigned(true)
    //         .trustStore(trustStore)
    //         .build();

    //     try (JarFile jar = JarFile.open(signedJarPath)) {
    //         CascaraJarVerifier verifier = (CascaraJarVerifier) jar.getVerifier();

    //         // 1. Verify entry bytes against cert
    //         byte[] classContent = jar.extractFile("io/github/qishr/cascara/Foo.class");
    //         boolean isVerified = verifier.verifySignature(myCaCert, "io/github/qishr/cascara/Foo.class", classContent);
    //         assertTrue(isVerified);

    //         // 2. Evaluate SPL policy
    //         SignatureStatus status = policy.evaluate(myCaCert, verifier.verifyAllSignatureFilesAgainstManifest());
    //         assertEquals(SignatureStatus.VALID, status);
    //     }
    // }



}
