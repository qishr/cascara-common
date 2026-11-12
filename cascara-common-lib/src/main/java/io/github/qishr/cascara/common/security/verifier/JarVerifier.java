package io.github.qishr.cascara.common.security.verifier;

import java.security.CodeSigner;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;

public interface JarVerifier {

    @NonNull JarVerifier setKeys(Map<String,byte[]> keys);
    @NonNull JarVerifier setSignatures(Map<String,byte[]> signatures);
    @NonNull JarVerifier setManifest(byte[] rawManifestBytes);

    void verifyJarSignature();
    void verifyEntrySignature(String entryName, byte[] content);
    boolean isTrustedManifestEntry(String entryName);

    @Nullable Map<String, VerifiedSigner> getSigners();
    @Nullable VerifiedSigner getSigner(String entryName);

    //
    //
    //

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
