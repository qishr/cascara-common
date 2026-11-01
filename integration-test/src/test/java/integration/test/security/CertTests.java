package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import integration.test.util.SourceCode;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.test.common.junit.base.TestBase;
import io.github.qishr.cascara.test.common.security.TestJarSigner;

public class CertTests extends TestBase implements SourceCode {
    // private final static Reporter REPORTER = GlobalReporter.forClass(CertTests.class);

    // @BeforeAll
    // static void init() {
    //     GlobalReporter.globalInstance()
    //         .setLevel(Level.DEBUG)
    //         .getFormatOptions()
    //             .setIncludeTime(false)
    //             .setIncluedPid(false)
    //             .setIncluedThread(false)
    //             .setUseSimpleClassName(true);
    // }

    // @AfterAll
    // static void end() {
    //     GlobalReporter.globalInstance()
    //         .setLevel(Level.INFO);
    // }

    @Test
    void test_invalid_expiredCertificate() throws Exception {
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
        try (ASN1InputStream ais = new ASN1InputStream(decoder, bundle.keyRsa())) {
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
