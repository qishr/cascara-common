package io.github.qishr.cascara.common.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.ASN1ParseException;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Enumerated;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1ObjectIdentifier;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1String;
import io.github.qishr.cascara.common.security.cms.CmsParser;
import io.github.qishr.cascara.common.security.cms.ContentInfo;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1OctetString;

public class JarFileTests {
    @Test
    void test_file() throws IOException, KeyStoreException {
        // KeyStore p12 = KeyStore.getInstance("pkcs12");
        // p12.load(new FileInputStream("THE_PFX_FILE_PATH"),
        //         getPassword().toCharArray());
        // // the key is ready to be used !
        // Key key = p12.getKey("SERVER", getPassword().toCharArray());



        Path path = Paths.get("/Users/sandy/security/t/META-INF/SERVER.RSA");
        byte[] bytes = Files.readAllBytes(path);
        // InputStream is = new
        InputStream in = new ByteArrayInputStream(bytes);

        ASN1Decoder dec = new DERDecoder();
        ASN1InputStream ais = new ASN1InputStream(dec, in);

        // for (ASN1Object obj : ais) {
        //     System.out.println("  ASN1Object: " + obj);
        // }

        // ais.forEach(a -> {
        //     System.out.println("  ASN1Object: " + a);
        // });

        while (ais.available() > 0) {
            try {
                ASN1Object obj = ais.readObject();

                ContentInfo contentInfo = CmsParser.parseContentInfo(obj);

                dump(obj,0);

                // System.out.println("  ASN1Object: " + obj);

                // if (obj instanceof ASN1TaggedObject to) {
                //     System.out.println("    ASN1TaggedObject");
                //     System.out.println("    Obj: " + to.getObject());
                //     System.out.println("    Tag: " + to.getTag());
                // }(obj in)


                // Object o = obj.getValue();
                // System.out.println("  Object: " + o.getClass().getName());


                // CertificateFactory factory = CertificateFactory.getInstance("X.509");
                // byte[] certEnc = seq.getObjectAt(1).toASN1Primitive().getEncoded();
                // Certificate cert = factory.generateCertificate(new ByteArrayInputStream(certEnc));

            } catch (ASN1ParseException e) {
                // System.err.println(e.getMessage());
            }
        }
    }

    private static void dump(Object obj, int depth) {

        String indent = "  ".repeat(depth);

        if (obj instanceof ASN1Sequence seq) {
            System.out.println(indent + "SEQUENCE");

            for (Object child : seq) {
                dump(child, depth + 1);
            }
        }
        else if (obj instanceof ASN1Set set) {
            System.out.println(indent + "SET");

            for (Object child : set) {
                dump(child, depth + 1);
            }
        }
        else if (obj instanceof ASN1ObjectIdentifier oid) {
            System.out.println(indent + "OID " + oid.getValue());
        }
        else if (obj instanceof ASN1TaggedObject tagged) {
            System.out.println(indent + "TAGGED " + tagged.getTag());
            System.out.println(indent + "  toString  : " + tagged.toString());
            System.out.println(indent + "  getTag    : " + tagged.getTag());
            System.out.println(indent + "  getTagNo  : " + tagged.getTagNo());
            System.out.println(indent + "  isExplicit: " + tagged.isExplicit());
            System.out.println(indent + "  getDecoder: " + tagged.getDecoder());
            System.out.println(
                HexFormat.of().formatHex(
                    tagged.getBytes()
                )
            );
            System.out.println(indent + "  getValue  : " + tagged.getValue());
            dump(tagged.getObject(), depth + 1);
        }
        else {
            System.out.println(indent + obj);
        }
    }


    void output(ASN1Object obj, int epth){
        String prefix = "    ".repeat(epth);

        if (obj instanceof ASN1OctetString os) {
            System.out.println(prefix + "  ASN1OctetString");
            System.out.println(prefix + "    : " + os.getValue());

            byte[] encodedCertificate = os.getValueBytes();
            try {
                Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encodedCertificate));
                System.out.println("*** SUCCESS ***");
            } catch (CertificateException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }

        else
        if (obj instanceof ASN1String s){
            System.out.println(prefix + "  ASN1String");
            System.out.println(prefix + "    : " + s.getValue());

        }

        else if (obj instanceof ASN1TaggedObject o){
            System.out.println(prefix + "  ASN1TaggedObject");
            output(o.getObject(), epth+1);
        }
        else if (obj instanceof ASN1Enumerated e) {
            System.out.println(prefix + "  ASN1Enumerated");
            System.out.println(prefix + "    val: " + e.getValue());
        }
        else if (obj instanceof ASN1Sequence seq) {
            System.out.println(prefix + "  ASN1Sequence");
            System.out.println(prefix + "    Size: " + seq.size());
            System.out.println(prefix + "    Tag: " + seq.getTag());
            for (int i = 0; i < seq.size(); i++) {

                ASN1Object item = seq.get(i);
                // System.out.println("    Item " + i + ": " + item);
                output(item, epth+1);
            }
        }

        else if (obj instanceof ASN1Set set) {
            System.out.println(prefix + "  ASN1Set");
            System.out.println(prefix + "    Val: " + set.getValue());
            System.out.println(prefix + "    Tag: " + set.getTag());
            try {
                set.forEach(a -> {
                    // System.out.println("    Item: " + a);
                    output(a, epth+1);
                });
            }catch (Exception e) {

            }
        }

        else{
            System.out.println(prefix + "  unknown");
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
