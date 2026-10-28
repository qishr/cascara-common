package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import integration.test.util.SourceCode;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.SecurityException;
import io.github.qishr.cascara.common.security.SignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignatureStatus;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.security.CascaraJarVerifier;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.common.util.ZipFile;
import io.github.qishr.cascara.test.common.junit.util.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.util.TestBase;
import io.github.qishr.cascara.test.common.junit.util.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.junit.util.TestJarSigner;

public class PolicyTests extends TestBase implements SourceCode {
    private final static Reporter REPORTER = GlobalReporter.forClass(CertTests.class);

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
            // .signWith(signer.certificate(), signer.privateKey())
            .signWith(signer) //TODO: remove unused sign* methods
            .packageJar();

        // 3. Verify JAR under a SignaturePolicy backed by the CA's TrustStore
        SignaturePolicy policy = SignaturePolicy.builder()
            .requireSigned(true)
            .trustStore(ca.trustStore())
            .build();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            CascaraJarVerifier verifier = (CascaraJarVerifier) jar.getVerifier();

            // Evaluates valid signature, cert expiry, and TrustStore anchor
            SignatureStatus status = policy.evaluate(signer.certificate());
            assertEquals(SignatureStatus.VALID, status);

        }

    }

    // TODO:

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
