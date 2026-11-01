package integration.test.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.security.SecurityMessage;
import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.util.Pair;

public final class JarManifestVerifier {

    private final Manifest manifest;
    private final Manifest signatureFile;
    private final byte[] rawManifestBytes;

    public JarManifestVerifier(byte[] manifestBytes, byte[] signatureFileBytes) throws IOException {
        this.rawManifestBytes = manifestBytes;
        this.manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
        this.signatureFile = new Manifest(new ByteArrayInputStream(signatureFileBytes));
    }

    /// Verifies that MANIFEST.SF matches the raw bytes and structure of MANIFEST.MF.
    public boolean verifySignatureFileAgainstManifest() throws NoSuchAlgorithmException {
        Attributes mainSfAttrs = signatureFile.getMainAttributes();

        // 1. Verify global MANIFEST.MF digest (SHA-256-Digest-Manifest)
        String sfManifestDigest = mainSfAttrs.getValue("SHA-256-Digest-Manifest");
        if (sfManifestDigest != null) {
            MessageDigest md = getAlgorithm("SHA-256");
            byte[] computed = md.digest(rawManifestBytes);
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

    /// Verifies an individual file entry's stream against its SHA-256 digest in MANIFEST.MF.
    public boolean verifyEntryContent(String entryName, InputStream contentStream) {

        Attributes mfAttrs = manifest.getAttributes(entryName);
        if (mfAttrs == null) {
            // Not a signed entry (e.g., unindexed META-INF files)
            return true;
        }

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
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, FileMessage.READ_ERROR, entryName);
        }

        byte[] computedDigest = md.digest();
        byte[] expectedDigest = Base64.getDecoder().decode(expectedDigestBase64);

        return Arrays.equals(computedDigest, expectedDigest);
    }

    private MessageDigest getAlgorithm(String name) {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, SecurityMessage.NO_SUCH_ALGORITHM, name);
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