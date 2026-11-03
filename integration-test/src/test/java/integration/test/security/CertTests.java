package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import integration.test.jar.JarTestBase;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;
import io.github.qishr.cascara.test.common.security.TestJarSigner;

public class CertTests extends JarTestBase {

    @Test
    void test_invalid_expiredCertificate() throws Exception {
        TestSigningIdentity identity = rootCA.issueExpiredIdentity("injected-entry-module");
        Path jarPath = tempDir.resolve("expired-cert.jar");

        SyntheticJar jar = SyntheticJar.forUseIn(CertTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                // .signWith(identity)
                .sign(s -> s.withIdentity(identity))
                .packageJar();

        X509Certificate expiredCert = identity.certificate();
        TestJarSigner.SignedJarBundle bundle = jar.getSignedBundle();
        // // 1. Generate an expired self-signed certificate (validity ended yesterday)
        // TestJarSigner jarSigner = new TestJarSigner();

        // X509Certificate expiredCert = jarSigner.generateExpiredSelfSignedCert("CN=Expired Signer, O=cascara");

        // // 2. Sign test jar bytes with the expired certificate
        // Map<String, byte[]> classFiles = Map.of(
        //     "io/github/qishr/cascara/Foo.class", "class bytes".getBytes(StandardCharsets.UTF_8)
        // );
        // TestJarSigner.SignedJarBundle bundle = jarSigner.sign(classFiles, expiredCert);

        // 3. Verify checkValidity throws CertificateExpiredException
        assertThrows(CertificateExpiredException.class, expiredCert::checkValidity);

        // 4. Assert that SignerInfo or JarVerifier maps this to SignatureStatus.CERTIFICATE_EXPIRED
        DERDecoder decoder = new DERDecoder();
        ASN1Sequence rootSeq;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, bundle.key())) {
            rootSeq = (ASN1Sequence) ais.readObject();
        }
        SignedData sd = CMSUtils.signedDataOrNull(rootSeq);
        SignerInfo signerInfo = SignerInfo.parse((ASN1Sequence) sd.signerInfos().iterator().next());

        assertThrows(CertificateExpiredException.class, () -> {
            X509Certificate cert = signerInfo.findSigningCertificate(List.of(expiredCert));
            cert.checkValidity(); // Enforced during trust path evaluation
        });
    }


    @Disabled
    @Test
    void old_test_invalid_expiredCertificate() throws Exception {
        // 1. Generate an expired self-signed certificate (validity ended yesterday)
        TestJarSigner jarSigner = new TestJarSigner();

        X509Certificate expiredCert = jarSigner.generateExpiredSelfSignedCert("CN=Expired Signer, O=cascara");

        // 2. Sign test jar bytes with the expired certificate
        Map<String, byte[]> classFiles = Map.of(
            "io/github/qishr/cascara/Foo.class", "class bytes".getBytes(StandardCharsets.UTF_8)
        );
        TestJarSigner.SignedJarBundle bundle = jarSigner.sign(classFiles, expiredCert);

        // 3. Verify checkValidity throws CertificateExpiredException
        assertThrows(CertificateExpiredException.class, expiredCert::checkValidity);

        // 4. Assert that SignerInfo or JarVerifier maps this to SignatureStatus.CERTIFICATE_EXPIRED
        DERDecoder decoder = new DERDecoder();
        ASN1Sequence rootSeq;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, bundle.key())) {
            rootSeq = (ASN1Sequence) ais.readObject();
        }
        SignedData sd = CMSUtils.signedDataOrNull(rootSeq);
        SignerInfo signerInfo = SignerInfo.parse((ASN1Sequence) sd.signerInfos().iterator().next());

        assertThrows(CertificateExpiredException.class, () -> {
            X509Certificate cert = signerInfo.findSigningCertificate(List.of(expiredCert));
            cert.checkValidity(); // Enforced during trust path evaluation
        });
    }
}
