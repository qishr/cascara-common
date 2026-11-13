package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.CMSUtils;
import io.github.qishr.cascara.test.common.security.KeyAlgorithm;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class JarSignatureValidTests extends JarTestBase {
    private final static Reporter REPORTER = Reporter.forClass(JarSignatureValidTests.class);

    @Test
    void test_verifySignature() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            ASN1Decoder dec = new DERDecoder();
            try (InputStream in = jar.getInputStream("META-INF/TEST.RSA")) {
                try (ASN1InputStream ais = new ASN1InputStream(dec, in)) {
                    ASN1Sequence seq = ais.readObject();

                    // A simple wrapper that checks the structure an calls SignedData.parse
                    SignedData sd = CMSUtils.signedDataOrNull(seq);

                    assertNotNull(sd);

                    // 1. Parse the tagged object as an implicit ASN1Set
                    ASN1Set certSet = sd.certificates().getObject(ASN1Tag.SET);

                    // 2. Parse the CertificateSet from the ASN1Set
                    CertificateSet certificateSet = CertificateSet.parse(certSet);
                    assertNotNull(certificateSet);

                    List<X509Certificate> certificates = certificateSet.certificates()
                            .stream()
                            .map(c -> {
                                try {
                                    return c.toX509Certificate();
                                } catch (CertificateException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .toList();

                    assertFalse(certificates.isEmpty());

                    // 1. Parse SignerInfo set from SignedData
                    ASN1Set signerInfoSet = sd.signerInfos();
                    for (ASN1Object<?> obj : signerInfoSet) {
                        if (obj instanceof ASN1Sequence signerSeq) {
                            SignerInfo signerInfo = SignerInfo.parse(signerSeq);

                            // 2. Find matching certificate from our parsed CertificateSet
                            X509Certificate signingCert = signerInfo.findSigningCertificate(certificates);
                            assertNotNull(signingCert, "Signing certificate must be present in PKCS#7 block");

                            // 3. Read raw .SF file bytes from META-INF/
                            byte[] sfBytes = jar.extractFile("META-INF/TEST.SF");

                            // 4. Verify signature!
                            boolean isValid = signerInfo.verifySignature(signingCert, sfBytes);
                            assertTrue(isValid, "JAR Signature on MANIFEST.SF must be cryptographically valid!");

                            REPORTER.debug("Verified signature for: " + signingCert.getSubjectX500Principal());
                        }
                    }
                }
            }
        }
    }

    @Test
    void test_valid_readJarEntries() {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {

            for (JarEntry entry : jar.getJarEntries()) {
                InputStream is = jar.getInputStream(entry.getName());
                is.close();
            }

        } catch (LocalizableIOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void test_ecSignedJar_evaluatesValid(@TempDir Path tempDir) throws Exception {
        // GlobalReporter.globalInstance().setLevel("io.github.qishr.cascara.test.common.security.TestCertificateAuthority", Level.DEBUG);
        // GlobalReporter.globalInstance().setLevel("io.github.qishr.cascara.test.common.security.TestJarSigner", Level.DEBUG);

        // 1. Create an EC Root CA
        TestCertificateAuthority ecCA = TestCertificateAuthority.create("Cascara EC Root CA", KeyAlgorithm.EC);
        TestSigningIdentity ecIdentity = ecCA.issueIdentity("ec-provider-module");

        // 2. Build synthetic JAR signed with EC identity
        SyntheticJar ecSignedJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
                .setPath(tempDir.resolve("ec-provider.jar"))
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s.withIdentity(ecIdentity))
                .packageJar();

        // 3. Verify under SignaturePolicy requiring ecCA trust anchor
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(ecCA.certificate())))
                .build();

        try (JarFile jar = JarFile.open(ecSignedJar.getPath())) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.VALID, status);
        }
    }
}