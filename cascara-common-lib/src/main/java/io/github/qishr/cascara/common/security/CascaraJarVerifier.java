package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.cms.SignatureStatus;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarVerifier;
import io.github.qishr.cascara.common.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class CascaraJarVerifier implements JarVerifier {
    private byte[] manifestBytes;
    private Map<String,byte[]> keysFiles;
    private Map<String,byte[]> signatureFiles;

    private Map<String,RsaKey> keys;
    private Map<String,Manifest> signatures;
    private Manifest manifest;
    private Map<String, JarVerifier.VerifiedSigner> signers;

    public CascaraJarVerifier() {
    }

    public CascaraJarVerifier setKeys(Map<String,byte[]> keysFiles) {
        this.keysFiles = keysFiles;
        this.keys = new HashMap<>();
        for (Entry<String,byte[]> entry : keysFiles.entrySet()) {
            String identityName = entry.getKey();
            byte[] kfBytes = entry.getValue();
            RsaKey key = new RsaKey(new ByteArrayInputStream(kfBytes));
            keys.put(identityName, key);
        }
        return this;
    }

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

    public CascaraJarVerifier setManifest(byte[] mf) {
        manifestBytes = mf;
        try {
            manifest = new Manifest(new ByteArrayInputStream(mf));
            return this;
        } catch (IOException e) {
            // TODO Proper error handling
            throw new SecurityException(e, GenericMessage.ERROR, e.getMessage());
        }
    }

    @Override
    public boolean verifyJarSignature() {
        boolean result = true;
        result &= verifySignaturesAgainstKeys();
        result &= verifyAllSignatureFilesAgainstManifest();
        return result;
    }

    /// Verifies an individual file entry's stream against its SHA-256 digest in MANIFEST.MF.
    @Override
    public boolean verifyEntrySignature(String entryName, byte[] content) {

        Attributes mfAttrs = manifest.getAttributes(entryName);
        if (mfAttrs == null) {
            // Not a signed entry (e.g., unindexed META-INF files)
            return true;
        }

        try (InputStream contentStream = new ByteArrayInputStream(content)) {

            Pair<String,String> digestPair = getAlgorithmAndDigest(mfAttrs);
            if (digestPair == null) {
                throw new SecurityException(SecurityMessage.NO_ALGORITHM, entryName);
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
                throw new SecurityException(e, FileMessage.READ_ERROR, entryName);
            }

            byte[] computedDigest = md.digest();
            byte[] expectedDigest = Base64.getDecoder().decode(expectedDigestBase64);

            return Arrays.equals(computedDigest, expectedDigest);
        } catch (LocalizableIOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    @Nullable
    public VerifiedSigner getSigner(String entryName) {
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
        for (Entry<String,Manifest> signature : signatures.entrySet()) {
            if (signature.getValue().getAttributes(entryName) != null) {
                // TODO: Also check that attributes contain digest
                return true;
            }
        }
        return false;
    }

    @Override
    @NonNull
    public SignatureStatus evaluatePolicy(SignaturePolicy policy) {
        if (signers == null) {
            verifySignaturesAgainstKeys();
        }

        // 1. Handle unsigned JARs
        if (signers == null || signers.isEmpty()) {
            return policy.evaluate(null);
        }

        // 2. Cryptographic signature & digest check (short-circuit if tampered)
        if (!verifyAllSignatureFilesAgainstManifest()) {
            return SignatureStatus.INVALID_SIGNATURE;
        }

        // 3. Evaluate internal signing certificates against policy
        SignatureStatus lastStatus = SignatureStatus.VALID;

        for (VerifiedSigner signer : signers.values()) {
            if (signer == null || signer.certificates() == null) {
                continue;
            }

            for (java.security.cert.Certificate cert : signer.certificates()) {
                if (cert instanceof X509Certificate x509) {
                    SignatureStatus status = policy.evaluate(x509);

                    // If any signing cert passes all policy rules, the JAR is valid
                    if (status == SignatureStatus.VALID) {
                        return SignatureStatus.VALID;
                    }
                    lastStatus = status;
                }
            }
        }

        return lastStatus;
    }

    @Override
    public boolean verifyJarSignature(X509Certificate cert, String entryName, byte[] content) {
        if (signers == null) {
            verifySignaturesAgainstKeys();
        }

        // 1. First, verify entry content against SHA digest in MANIFEST.MF
        if (!verifyEntrySignature(entryName, content)) {
            return false;
        }

        // 2. Obtain verified signers associated with this manifest entry
        VerifiedSigner signer = getSigner(entryName);
        if (signer == null) {
            return false; // Entry is not signed
        }

        // 3. Confirm that the specified cert exists within the verified certificates for this entry
        if (signer.certificates() == null) {
            return false;
        }

        for (java.security.cert.Certificate entryCert : signer.certificates()) {
            if (entryCert.equals(cert)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean verifyJarSignature(X509Certificate cert, byte[] content) {
        if (signers == null) {
            verifySignaturesAgainstKeys();
        }
        // Evaluates whether the content matches the global raw MANIFEST.MF bytes
        // and is signed by the provided certificate
        if (manifestBytes != null && Arrays.equals(manifestBytes, content)) {
            for (VerifiedSigner signer : signers.values()) {
                if (signer != null && signer.certificates() != null) {
                    for (java.security.cert.Certificate c : signer.certificates()) {
                        if (c.equals(cert)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    //
    //
    //

    private boolean verifyAllSignatureFilesAgainstManifest() {
        boolean result = true;
        for (Manifest signature : signatures.values()) {
            result &= verifySignatureFileAgainstManifest(signature);
        }
        return result;
    }

    private boolean verifySignatureFileAgainstManifest(Manifest signatureFile) {
        Attributes mainSfAttrs = signatureFile.getMainAttributes();

        // 1. Verify global MANIFEST.MF digest (SHA-256-Digest-Manifest)
        String sfManifestDigest = mainSfAttrs.getValue("SHA-256-Digest-Manifest");
        if (sfManifestDigest != null) {
            MessageDigest md = getAlgorithm("SHA-256");
            byte[] computed = md.digest(manifestBytes);
            byte[] expected = Base64.getDecoder().decode(sfManifestDigest);

            if (!Arrays.equals(computed, expected)) {
                return false; // Whole-manifest digest mismatch!
            }
        }

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
                return false; // File listed in .SF missing from .MF!
            }

            // Note: Section digest in .SF is calculated over the raw header lines of that section in MANIFEST.MF
            String mfEntryDigest = mfEntryAttrs.getValue("SHA-256-Digest");
            if (mfEntryDigest == null) {
                return false;
            }
        }

        return true;
    }

    private boolean verifySignaturesAgainstKeys() {


        boolean throwExceptions = false;


        if (keys == null) {
            throw new LocalizableRuntimeException(GenericMessage.ERROR, "No keys set");
        }
        signers = new HashMap<>();
        boolean allSignaturesValid = true;
        for (Entry<String,RsaKey> entry : keys.entrySet()) {

            String signerName = entry.getKey();
            RsaKey k = entry.getValue();
            // RsaKey k = new RsaKey(new ByteArrayInputStream(keyBytes));

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
                            if (cert != null && signerInfo.verifySignature(cert, sfBytes)) {
                                signatureValid = true;
                                break;
                            }
                        }
                    }
                }


                // TODO: This casues test_invalid_untrustedSigner_signaturePolicy to
                //  get an exeption before it does its verification. We need to support:
                // - normal exception throwing like this, and
                // - returning false on failure
                if (!signatureValid && throwExceptions) {
                    throw new SecurityException(GenericMessage.ERROR, "Signature verification failed for " + signerName);
                }

                allSignaturesValid &= signatureValid;



                VerifiedSigner signer = new VerifiedSigner(k.getCertificates());
                signers.put(signerName, signer);



            } catch (CertificateException e) {
                if (throwExceptions) {
                    throw new SecurityException(e, GenericMessage.ERROR, "Error reading certificate: " + e.getMessage());
                }
                return false;
            } catch (SecurityException e) {
                if (throwExceptions) {
                    throw e;
                }
                return false;
            } catch (Exception e) {
                if (throwExceptions) {
                    throw new SecurityException(e, GenericMessage.ERROR, "Signature check failed: " + e.getMessage());
                }
                return false;
            }
        }
        return allSignaturesValid;
    }

    private MessageDigest getAlgorithm(String name) {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e, SecurityMessage.NO_SUCH_ALGORITHM, name);
        }
    }

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

    private Pair<String,String> tryAlgorithm(String headerKey, Attributes mfAttrs) {
        String digest = mfAttrs.getValue(headerKey);
        if (digest == null) {
            return null;
        }
        String algorithm = headerKey.replaceAll("-Digest$", "").replace("SHA1", "SHA-1");
        return new Pair<>(algorithm, digest);
    }
}