package io.github.qishr.cascara.test.common.junit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.JarEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.CMSUtils;
import io.github.qishr.cascara.test.common.security.TestJarSigner;
import io.github.qishr.cascara.test.common.security.TestJarSigner.SignedJarBundle;

public class JarSignerTest extends MetaTestBase {


    @Test
    void testSignSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources("code/ModuleInfoA.txt", "code/ProviderA.txt")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            byte[] mf = jar.extractFile("META-INF/MANIFEST.MF");
            byte[] sf = jar.extractFile("META-INF/MANIFEST.SF");
            JarManifestVerifier jv = new JarManifestVerifier(mf, sf);
            assertTrue(jv.verifySignatureFileAgainstManifest());
            for (JarEntry jarEntry : jar.getJarEntries()) {
                // Certificate[] certs = jarEntry.getCertificates();
                try (InputStream is = jar.getInputStream(jarEntry)) {
                    assertTrue(jv.verifyEntryContent(jarEntry.getName(), is));
                }
            }
        }
    }


    @Test
    void testSignedJarEntryHasCertificate() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignerTest.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources("code/ModuleInfoA.txt", "code/ProviderA.txt")
            .sign()
            .packageJar();

        SignedJarBundle sb = testJar.getSignedBundle();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            JarEntry jarEntry = jar.getJarEntry("module-info.class");
            Certificate[] certs = jarEntry.getCertificates();
            assertEquals(sb.certificate(), certs[0]);

            // Make the JarVerifier verify each entry
            for (JarEntry je : jar.getJarEntries()) {
                try (InputStream is = jar.getInputStream(je)) {
                }
            }

        }
    }

    @Test
    void test_mock_jar_signature_verification() throws Exception {
        // 1. Define mock class file entries
        Map<String, byte[]> classFiles = Map.of(
            "io/github/qishr/cascara/Foo.class", "mock byte code for Foo".getBytes(StandardCharsets.UTF_8),
            "io/github/qishr/cascara/Bar.class", "mock byte code for Bar".getBytes(StandardCharsets.UTF_8)
        );

        // 2. Programmatically generate full signed JAR bundle
        TestJarSigner.SignedJarBundle bundle = new TestJarSigner().sign(classFiles);

        assertNotNull(bundle.manifestMf());
        assertNotNull(bundle.manifestSf());
        assertNotNull(bundle.key());

        // 3. Unmarshal KEY.RSA back through CMS / ASN1 engine

        DERDecoder decoder = new DERDecoder();
        SignedData sd = null;

        try (ASN1InputStream ais = new ASN1InputStream(decoder, bundle.key())) {
            ASN1Sequence rootSeq = (ASN1Sequence) ais.readObject();
            sd = CMSUtils.signedDataOrNull(rootSeq);
        };
        assertNotNull(sd, "SignedData must parse correctly from generated KEY.RSA");

        // 4. Extract Certificates & SignerInfo
        ASN1Set certSet = sd.certificates().getObject(ASN1Tag.SET);
        CertificateSet certificateSet = CertificateSet.parse(certSet);
        X509Certificate parsedCert = certificateSet.certificates().get(0).toX509Certificate();

        assertEquals(bundle.certificate().getSubjectX500Principal(), parsedCert.getSubjectX500Principal());

        SignerInfo signerInfo = SignerInfo.parse((ASN1Sequence) sd.signerInfos().iterator().next());

        // 5. Verify Signature on MANIFEST.SF
        boolean isSigValid = signerInfo.verifySignature(parsedCert, bundle.manifestSf());
        assertTrue(isSigValid, "Generated PKCS#7 signature over MANIFEST.SF must be cryptographically valid!");

        // // 6. Verify Manifest Entries against Class Stream
        // JarManifestVerifier verifier = new JarManifestVerifier(bundle.manifestMf(), bundle.manifestSf());
        // assertTrue(verifier.verifySignatureFileAgainstManifest());

        // ByteArrayInputStream fooStream = new ByteArrayInputStream(classFiles.get("io/github/qishr/cascara/Foo.class"));
        // assertTrue(verifier.verifyEntryContent("io/github/qishr/cascara/Foo.class", fooStream));
    }

}
