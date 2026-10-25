package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.Certificate;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.CmsParser;
import io.github.qishr.cascara.common.security.cms.ContentInfo;
import io.github.qishr.cascara.common.security.cms.SignedData;

public class CertTests {
    private final static Reporter REPORTER = GlobalReporter.forClass(CertTests.class);

    @Test
    void test_file() throws IOException, KeyStoreException {
        GlobalReporter.globalInstance()
            .setLevel(Level.DEBUG)
            .getFormatOptions()
                .setIncludeTime(false)
                .setIncluedPid(false)
                .setIncluedThread(false)
                .setUseSimpleClassName(true);

        ASN1Decoder dec = new DERDecoder();
        try (InputStream in = new FileInputStream("/Users/sandy/security/t/META-INF/SERVER.RSA")) {
            try (ASN1InputStream ais = new ASN1InputStream(dec, in)) {
                Iterator<ASN1Object> it = ais.iterator();
                while (it.hasNext()) {
                    ASN1Object<?> obj = it.next();
                    ContentInfo contentInfo = CmsParser.parseContentInfo(obj);
                    // CMSUtils.dump(obj, REPORTER);

                    if (obj instanceof ASN1Sequence seq) {
                        SignedData sd = CMSUtils.signedDataOrNull(seq);
                        assertNotNull(sd);
                        REPORTER.info(GenericMessage.INFO, "Success!");

                        CertificateSet certificateSet = CertificateSet.parse(
                            (ASN1Sequence) sd.certificates().getObject()
                        );
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

                        for (X509Certificate cert : certificates) {

                            System.out.println(
                                    cert.getSubjectX500Principal());

                            System.out.println(
                                    cert.getIssuerX500Principal());
                        }
                    }
                }
            }
        }
    }






    @Test
    void test_file2() throws CertificateException, IOException {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        System.out.println();
        System.out.println("CertificateFactory Object Info: ");
        System.out.println("Type = "+cf.getType());
        System.out.println("Provider = "+cf.getProvider());
        System.out.println("toString = "+cf.toString());



        Path path = Paths.get("/Users/sandy/security/t/META-INF/SERVER.SF");
        byte[] bytes = Files.readAllBytes(path);
        // InputStream is = new
        InputStream in = new ByteArrayInputStream(bytes);

        // FileInputStream fis = new FileInputStream(path.toString());
        java.security.cert.Certificate cert = cf.generateCertificate(in);
        in.close();

      System.out.println();
      System.out.println("Certificate Object Info: ");
      System.out.println("Type = "+cert.getType());
      System.out.println("toString = "+cert.toString());

      PublicKey pubKey = cert.getPublicKey();
      System.out.println();
      System.out.println("PublicKey Object Info: ");
      System.out.println("Algorithm = "+pubKey.getAlgorithm());
      System.out.println("Format = "+pubKey.getFormat());
      System.out.println("toString = "+pubKey.toString());

    //   FileOutputStream fos = new FileOutputStream(output);
    //   byte[] certBytes = cert.getEncoded();
    //   fos.write(certBytes);
    //   fos.close();

    }

    @Test
    void test_signature() {
        Path path = Path.of("/Users/sandy/security/signedjar.jar");
        try {
            JarFile jar = JarFile.open(path);
        } catch (LocalizableIOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
