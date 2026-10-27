package io.github.qishr.cascara.common.util;

import java.security.CodeSigner;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import io.github.qishr.cascara.common.security.RsaKey;

public interface JarVerifier {
    JarVerifier setKeys(Map<String,RsaKey> keus);
    // JarVerifier setSigners(Map<String,VerifiedSigner> signers);
    JarVerifier setSignatures(Map<String,byte[]> signatures);
    JarVerifier setManifest(byte[] rawManifestBytes);

    boolean verifySignaturesAgainstKeys();
    boolean verifyAllSignatureFilesAgainstManifest();
    boolean verifySignatureFileAgainstManifest(Manifest signatureFile);
    boolean verifyEntryContent(String entryName, byte[] content);
    VerifiedSigner getSigner(String entryName);
    boolean isTrustedManifestEntry(String entryName);
    /**
     * Verifies that the given file content matches the SHA digest in MANIFEST.MF
     * AND that the entry was signed by the specified X509Certificate.
     */
    boolean verifySignature(X509Certificate cert, String entryName, byte[] content);

    boolean verifySignature(X509Certificate cert, byte[] content);

    public final static class VerifiedSigner {
        private final Certificate[] certificates;
        private final CodeSigner[] codeSigners;

        public VerifiedSigner(List<? extends Certificate> chain) throws CertificateException {
            this.certificates = chain.toArray(Certificate[]::new);

            CertificateFactory factory =
                    CertificateFactory.getInstance("X.509");

            List<? extends Certificate> certificates = List.copyOf(chain);
            CertPath certPath =
                    factory.generateCertPath(
                            certificates
                    );

            this.codeSigners = new CodeSigner[] {
                    new CodeSigner(certPath, null)
            };
        }

        public Certificate[] certificates() {
            return certificates;
        }

        public CodeSigner[] codeSigners() {
            return codeSigners;
        }
    }
}
