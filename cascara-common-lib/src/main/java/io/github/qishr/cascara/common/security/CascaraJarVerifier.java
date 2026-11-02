package io.github.qishr.cascara.common.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarVerifier;
import io.github.qishr.cascara.common.util.Pair;

public class CascaraJarVerifier implements JarVerifier {
    private byte[] manifestBytes;
    private Map<String,byte[]> signatureFiles;

    private Map<String,SignatureBlock> keys;
    private Map<String,Manifest> signatures;
    private Manifest manifest;
    private Map<String, JarVerifier.VerifiedSigner> signers;

    public CascaraJarVerifier() {
    }

    @NonNull
    public CascaraJarVerifier setKeys(Map<String,byte[]> keysFiles) {
        this.keys = new HashMap<>();
        for (Entry<String,byte[]> entry : keysFiles.entrySet()) {
            String identityName = entry.getKey();
            byte[] kfBytes = entry.getValue();
            SignatureBlock key = new SignatureBlock(new ByteArrayInputStream(kfBytes));
            keys.put(identityName, key);
        }
        return this;
    }

    @NonNull
    public CascaraJarVerifier setSignatures(Map<String,byte[]> signatureFiles) {
        this.signatureFiles = signatureFiles;
        this.signatures = new HashMap<>();
        for (Entry<String,byte[]> entry : signatureFiles.entrySet()) {
            String identityName = entry.getKey();
            byte[] sfBytes = entry.getValue();
            try {
                Manifest signature = new Manifest(new ByteArrayInputStream(sfBytes));
                signatures.put(identityName, signature);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return this;
    }

    @NonNull
    public CascaraJarVerifier setManifest(byte[] mf) {
        manifestBytes = mf;
        try {
            manifest = new Manifest(new ByteArrayInputStream(mf));
            return this;
        } catch (IOException e) {
            // TODO: Better status code and more specific exception class
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, e.getMessage());
        }
    }

    @Override
    public void verifyJarSignature() {
        assertSetup();
        verifySignaturesAgainstKeys();
        verifyAllSignatureFilesAgainstManifest();
    }

    /// Verifies an individual file entry's stream against its SHA-256 digest in MANIFEST.MF.
    @Override
    public void verifyEntrySignature(String entryName, byte[] content) {
        assertSetup();

        Attributes mfAttrs = manifest.getAttributes(entryName);
        if (mfAttrs == null) {
            // Not a signed entry (e.g., unindexed META-INF files)
            // TODO: Better status code and/or more specific exception class
            throw new SignatureException(SignatureStatus.NOT_SIGNED, GenericMessage.ERROR, entryName + " is not a signed entry");
        }

        try (InputStream contentStream = new ByteArrayInputStream(content)) {

            Pair<String,String> digestPair = getAlgorithmAndDigest(mfAttrs);
            if (digestPair == null) {
                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, SecurityMessage.NO_ALGORITHM, entryName);
            }

            String algorithm = digestPair.getL();
            String expectedDigestBase64 = digestPair.getR();

            MessageDigest md = getAlgorithm(algorithm);

            byte[] buffer = new byte[8192];
            int read;
            try {
                while ((read = contentStream.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            } catch (IOException e) {
                // TODO: Better status code
                throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, FileMessage.READ_ERROR, entryName);
            }

            byte[] computedDigest = md.digest();
            byte[] expectedDigest = Base64.getDecoder().decode(expectedDigestBase64);

            if (!Arrays.equals(computedDigest, expectedDigest)) {
                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, FileMessage.READ_ERROR, entryName);
            }

        } catch (LocalizableIOException e) {
            // TODO: Better status code
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, FileMessage.READ_ERROR, entryName);
        } catch (IOException e) {
            // TODO: Better status code
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, FileMessage.READ_ERROR, entryName);
        }
    }

    public Map<String, VerifiedSigner> getSigners() {
        assertSetup();
        ensureSigners();
        return signers;
    }

    @Override
    @Nullable
    public VerifiedSigner getSigner(String entryName) {
        assertSetup();
        ensureSigners();

        // Special handling for overall MANIFEST.MF
        if ("META-INF/MANIFEST.MF".equals(entryName)) {
            if (signers == null || signers.isEmpty()) {
                return null;
            }
            return signers.values().iterator().next();
        }

        VerifiedSigner result = signers.computeIfAbsent(entryName, n -> {
            for (Entry<String,Manifest> signature : signatures.entrySet()) {
                if (isTrustedManifestEntry(entryName)) {
                    return signers.get(signature.getKey());
                }
            }
            return null;
        });
        return result;
    }

    @Override
    public boolean isTrustedManifestEntry(String entryName) {
        assertSetup();
        for (Entry<String,Manifest> signature : signatures.entrySet()) {
            if (signature.getValue().getAttributes(entryName) != null) {
                // TODO: Also check that attributes contain digest
                return true;
            }
        }
        return false;
    }


    //
    //
    //

    private void assertSetup() {
        if (signatures == null) {
            // TODO: Better exception
            throw new IllegalStateException("setSignatures has not been called");
        }
        if (manifest == null) {
            // TODO: Better exception
            throw new IllegalStateException("setManifest has not been called");
        }
        if (keys == null) {
            // TODO: Better exception
            throw new IllegalStateException("setKeys has not been called");
        }
    }

    private void ensureSigners() {
        if (signers == null) {
            verifySignaturesAgainstKeys();
        }
    }

    private void verifyAllSignatureFilesAgainstManifest() {
        for (Manifest signature : signatures.values()) {
            verifySignatureFileAgainstManifest(signature);
        }
    }

    private void verifySignatureFileAgainstManifest(Manifest signatureFile) {
        Attributes mainSfAttrs = signatureFile.getMainAttributes();

        // 1. Verify global MANIFEST.MF digest (SHA-256-Digest-Manifest)
        String sfManifestDigest = mainSfAttrs.getValue("SHA-256-Digest-Manifest");
        if (sfManifestDigest != null) {
            MessageDigest md = getAlgorithm("SHA-256");
            byte[] computed = md.digest(manifestBytes);
            byte[] expected = Base64.getDecoder().decode(sfManifestDigest);

            if (!Arrays.equals(computed, expected)) {
                // TODO: Proper exception message
                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Manifest does not match digest");
            }
        }
        // TODO: WHat if sfManifestDigest *IS* null? Is it valid?

        // 2. Verify individual section digests in MANIFEST.SF against MANIFEST.MF
        Map<String, Attributes> sfEntries = signatureFile.getEntries();
        Map<String, Attributes> mfEntries = manifest.getEntries();

        for (Map.Entry<String, Attributes> entry : sfEntries.entrySet()) {
            String entryName = entry.getKey();
            Attributes sfEntryAttrs = entry.getValue();

            String sfEntryDigest = sfEntryAttrs.getValue("SHA-256-Digest");
            if (sfEntryDigest == null) {
                continue;
            }

            Attributes mfEntryAttrs = mfEntries.get(entryName);
            if (mfEntryAttrs == null) {
                // File listed in .SF missing from .MF!
                // TODO: Proper exception message
                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Missing attributes for " + entryName + " in MANIFEST.MF");
            }

            // Note: Section digest in .SF is calculated over the raw header lines of that section in MANIFEST.MF
            String mfEntryDigest = mfEntryAttrs.getValue("SHA-256-Digest");
            if (mfEntryDigest == null) {
                // TODO: Proper exception message
                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Missing digest for " + entryName);
            }
        }
    }

    private void verifySignaturesAgainstKeys() {
        signers = new HashMap<>();
        // `keys` cannot be null here due to earlier assertSetup() call

        if (keys.size() < signatureFiles.size()) {
            // TODO: Proper exception
            throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Missing key file");
        }
        if (keys.size() > signatureFiles.size()) {
            // TODO: Proper exception
            throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Extra key file");
        }

        for (Entry<String,SignatureBlock> entry : keys.entrySet()) {

            String signerName = entry.getKey();
            SignatureBlock k = entry.getValue();

            byte[] sfBytes = signatureFiles.get(signerName);
            try {

                // 1. Verify PKCS#7 / CMS signature over raw .SF file bytes
                SignedData sd = k.getSignedData();
                boolean signatureValid = false;

                if (sd != null && sd.signerInfos() != null) {
                    for (ASN1Object<?> signerObj : sd.signerInfos()) {
                        if (signerObj instanceof ASN1Sequence signerSeq) {
                            SignerInfo signerInfo = SignerInfo.parse(signerSeq);
                            X509Certificate cert = signerInfo.findSigningCertificate(k.getCertificates());
                            if (cert == null) {
                                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Signature verification failed for identity \"" + signerName + "\"");
                            }
                            boolean verified = signerInfo.verifySignature(cert, sfBytes);
                            if (cert != null && verified) {
                                signatureValid = true;
                                break;
                            }
                        }
                    }
                }

                if (!signatureValid) {
                    // TODO: This happens when the SF file's digest doesn't match the signature
                    // TODO: Proper exception

                    // FAILURE: Test test_invalid_untrustedSigner_signaturePolicy expects UNTRUSTED_SIGNER
                    throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Signature verification failed for identity \"" + signerName + "\"");
                }

                VerifiedSigner signer = new VerifiedSigner(k.getCertificates());
                signers.put(signerName, signer);

            } catch (CertificateException e) {
                // TODO: Better status code
                throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Error reading certificate: " + e.getMessage());
            } catch (SignatureException e) {
                throw e;
            // } catch (InvalidKeyException e) {
            //     // TODO: Better status code
            //     // "No installed provider supports this key: sun.security.ec.ECPublicKeyImpl"
            //     throw new LocalizableInvalidKeyException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Signature check failed: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: Better status code
                throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Signature check failed: " + e.getMessage());
            }
        }
    }



    //====================================================
    // New methods:
    public static String resolveSignatureAlgorithm(String digestOid, PublicKey publicKey) {
        String keyAlg = publicKey.getAlgorithm(); // "RSA", "EC", "DSA"
        String digestName = resolveDigestName(digestOid); // "SHA256", "SHA384", "SHA512", "SHA1"

        if ("EC".equalsIgnoreCase(keyAlg)) {
            return digestName + "withECDSA";
        } else if ("DSA".equalsIgnoreCase(keyAlg)) {
            return digestName + "withDSA";
        } else if ("RSA".equalsIgnoreCase(keyAlg)) {
            return digestName + "withRSA";
        }

        throw new UnsupportedAlgorithmException(keyAlg);
    }
    private static String resolveDigestName(String oid) {
        return switch (oid) {
            case "2.16.840.1.101.3.4.2.1" -> "SHA256";
            case "2.16.840.1.101.3.4.2.2" -> "SHA384";
            case "2.16.840.1.101.3.4.2.3" -> "SHA512";
            case "1.3.14.3.2.26"           -> "SHA1";
            default -> throw new UnsupportedDigestException(oid);
        };
    }
    //
    //====================================================




    private MessageDigest getAlgorithm(String name) {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedAlgorithmException(e, name);
        }
    }

    @Nullable
    private Pair<String,String> getAlgorithmAndDigest(Attributes mfAttrs) {
        Pair<String,String> pair = tryAlgorithm("SHA-384-Digest", mfAttrs);
        if (pair == null) {
            pair = tryAlgorithm("SHA-256-Digest", mfAttrs);
        }
        if (pair == null) {
            pair = tryAlgorithm("SHA-Digest", mfAttrs);
        }
        return pair;
    }

    @Nullable
    private Pair<String,String> tryAlgorithm(String headerKey, Attributes mfAttrs) {
        String digest = mfAttrs.getValue(headerKey);
        if (digest == null) {
            return null;
        }
        String algorithm = headerKey.replaceAll("-Digest$", "").replace("SHA1", "SHA-1");
        return new Pair<>(algorithm, digest);
    }

    private static class SignatureBlock {
        private List<X509Certificate> certificates;
        private SignedData sd;

        public SignatureBlock(InputStream in) {
            loadCerts(in);
        }

        public List<X509Certificate> getCertificates() {
            return certificates;
        }

        public SignedData getSignedData() {
            return sd;
        }

        private void loadCerts(InputStream in) {
            ASN1Decoder dec = new DERDecoder();
            try (ASN1InputStream ais = new ASN1InputStream(dec, in)) {
                ASN1Sequence seq = ais.readObject();

                // A simple wrapper that checks the structure an calls SignedData.parse
                sd = CMSUtils.signedDataOrNull(seq);

                // TODO: Null check
                // also this needs re-written to not use CMSUtils which can then be removed.

                // 1. Parse the tagged object as an implicit ASN1Set
                ASN1Set certSet = sd.certificates().getObject(ASN1Tag.SET);

                // 2. Parse the CertificateSet from the ASN1Set
                CertificateSet certificateSet = CertificateSet.parse(certSet);

                certificates = certificateSet.certificates()
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
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, e.getMessage());
            }
        }
    }
}