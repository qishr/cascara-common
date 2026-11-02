package io.github.qishr.cascara.test.common.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

public enum KeyAlgorithm {
    RSA("RSA", "SHA256withRSA", "1.2.840.113549.1.1.11"),
    EC("EC", "SHA256withECDSA", "1.2.840.10045.4.3.2"),
    DSA("DSA", "SHA256withDSA", "2.16.840.1.101.3.4.3.2");

    private final String keyAlgorithm;
    private final String signatureAlgorithm;
    private final String signatureOid;

    KeyAlgorithm(String keyAlgorithm, String signatureAlgorithm, String signatureOid) {
        this.keyAlgorithm = keyAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
        this.signatureOid = signatureOid;
    }

    public static KeyAlgorithm of(String name) {
        switch(name.toUpperCase()) {
            case "RSA":
                return RSA;
            case "DSA":
                return DSA;
            case "EC":
                return EC;
            default:
                return null;
        }
    }

    public String getKeyAlgorithm() { return keyAlgorithm; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public String getSignatureOid() { return signatureOid; }

    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyAlgorithm);
        if (this == EC) {
            // Standard NIST P-256 (secp256r1)
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
        } else if (this == RSA) {
            kpg.initialize(2048);
        } else if (this == DSA) {
            kpg.initialize(2048);
        }
        return kpg.generateKeyPair();
    }
}