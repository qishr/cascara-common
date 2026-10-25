package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.JarManifestVerifier;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;

public class CertTests {
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

    @Test
    void test_encode() {
        ASN1TaggedObject tagged =
                new ASN1TaggedObject(
                        ASN1Tag.INTEGER,
                        new ASN1Integer(2),
                        true);
        byte[] bytes = tagged.getEncoded();
        REPORTER.debug(
            "Bytes: " + HexFormat.of().formatHex(Arrays.copyOf(bytes, 16))
        );
    }


    @Test
    void test_verifySignature() throws Exception {
        ASN1Decoder dec = new DERDecoder();
        try (InputStream in = new FileInputStream("/Users/sandy/security/t/META-INF/SERVER.RSA")) {
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

                // for (X509Certificate cert : certificates) {
                //     REPORTER.debug("Subject: " + cert.getSubjectX500Principal());
                //     REPORTER.debug("Issuer : " + cert.getIssuerX500Principal());
                // }

                // 1. Parse SignerInfo set from SignedData
                ASN1Set signerInfoSet = sd.signerInfos();
                for (ASN1Object<?> obj : signerInfoSet) {
                    if (obj instanceof ASN1Sequence signerSeq) {
                        SignerInfo signerInfo = SignerInfo.parse(signerSeq);

                        // 2. Find matching certificate from our parsed CertificateSet
                        X509Certificate signingCert = signerInfo.findSigningCertificate(certificates);
                        assertNotNull(signingCert, "Signing certificate must be present in PKCS#7 block");

                        // 3. Read raw .SF file bytes from META-INF/
                        byte[] sfBytes = Files.readAllBytes(Paths.get("/Users/sandy/security/t/META-INF/SERVER.SF"));

                        // 4. Verify signature!
                        boolean isValid = signerInfo.verifySignature(signingCert, sfBytes);
                        assertTrue(isValid, "JAR Signature on MANIFEST.SF must be cryptographically valid!");

                        REPORTER.debug("Verified signature for: " + signingCert.getSubjectX500Principal());
                    }
                }
            }
        }
    }


    @Test
    void test_jarEntry() {
        Path path = Path.of("/Users/sandy/security/signedjar.jar");
        try (JarFile jar = JarFile.open(path)) {

            byte[] mfContent = jar.extractFile("META-INF/MANIFEST.MF");
            byte[] sfContent = jar.extractFile("META-INF/SERVER.SF");
            // byte[] signatureBlock = jar.extractFile("META-INF/SERVER.RSA");

            JarManifestVerifier verifier = new JarManifestVerifier(mfContent, sfContent);
            boolean v1 = verifier.verifySignatureFileAgainstManifest();
            assertTrue(v1, "SF not verified against MF");

            String entryName = "io/github/qishr/cascara/common/util/JarFile.class";
            try (InputStream is = jar.getInputStream(entryName)) {
                boolean v2 = verifier.verifyEntryContent(entryName, is);
                assertTrue(v2, "Entry not verified");
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (LocalizableIOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
