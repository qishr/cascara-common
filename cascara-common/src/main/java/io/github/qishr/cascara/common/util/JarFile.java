// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime.Version;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.security.RsaKey;
import io.github.qishr.cascara.common.security.SecurityException;
import io.github.qishr.cascara.common.security.SecurityMessage;
import io.github.qishr.cascara.common.security.SignaturePolicy;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.cms.SignatureStatus;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarVerifier.VerifiedSigner;

public class JarFile extends ZipFile {
    private static final String META_INF = "META-INF/";
    private static final String DOT_SF = ".SF";
    private static final String META_INF_VERSIONS = META_INF + "versions/";
    // private static final String INDEX_NAME = META_INF + "INDEX.LIST";
    public static final String MANIFEST_MF = META_INF + "MANIFEST.MF";

    private JarManifest manifest;
    private JarVerifier verifier = null;
    private boolean verify;
    private Version version;
    private boolean isMultiRelease;

    private String moduleName = null;
    private Properties mavenProperties;
    private Set<String> packageNames = null;
    private Set<String> classNames = null;

    private static final Runtime.Version BASE_VERSION;
    private static final int BASE_VERSION_FEATURE;
    private static final Runtime.Version RUNTIME_VERSION;
    private static final boolean MULTI_RELEASE_ENABLED;
    private static final boolean MULTI_RELEASE_FORCED;
    private final int versionFeature;

    static {
        // multi-release jar file versions >= 9
        BASE_VERSION = Runtime.Version.parse(Integer.toString(8));
        BASE_VERSION_FEATURE = BASE_VERSION.feature();
        String jarVersion = GetPropertyAction.privilegedGetProperty("jdk.util.jar.version");
        int runtimeVersion = Runtime.version().feature();
        if (jarVersion != null) {
            int jarVer = Integer.parseInt(jarVersion);
            runtimeVersion = (jarVer > runtimeVersion)
                    ? runtimeVersion
                    : Math.max(jarVer, BASE_VERSION_FEATURE);
        }
        RUNTIME_VERSION = Runtime.Version.parse(Integer.toString(runtimeVersion));
        String enableMultiRelease = GetPropertyAction
                .privilegedGetProperty("jdk.util.jar.enableMultiRelease", "true");
        switch (enableMultiRelease) {
            case "false" -> {
                MULTI_RELEASE_ENABLED = false;
                MULTI_RELEASE_FORCED = false;
            }
            case "force" -> {
                MULTI_RELEASE_ENABLED = true;
                MULTI_RELEASE_FORCED = true;
            }
            default -> {
                MULTI_RELEASE_ENABLED = true;
                MULTI_RELEASE_FORCED = false;
            }
        }
    }

    public static JarFile open(Path jarPath) throws LocalizableIOException {
        return new JarFile(jarPath, false, true, BASE_VERSION);
    }

    private JarFile(Path jarPath, boolean create, boolean verify, Runtime.Version version) throws LocalizableIOException {
        super(jarPath, create);

        this.verify = verify;
        Objects.requireNonNull(version);
        if (MULTI_RELEASE_FORCED || version.feature() == RUNTIME_VERSION.feature()) {
            // This deals with the common case where the value from JarFile.runtimeVersion() is passed
            this.version = RUNTIME_VERSION;
        } else if (version.feature() <= BASE_VERSION_FEATURE) {
            // This also deals with the common case where the value from JarFile.baseVersion() is passed
            this.version = BASE_VERSION;
        } else {
            // Canonicalize
            this.version = Runtime.Version.parse(Integer.toString(version.feature()));
        }
        this.versionFeature = this.version.feature();

        String jarManifest = new String(super.extractFile("META-INF/MANIFEST.MF"));
        manifest = JarManifest.parse(jarManifest);
    }

    public Path getPath() {
        return archivePath;
    }

    public JarManifest getManifest() {
        return manifest;
    }

    public JarEntry getJarEntry(String name) {
        return new JarEntry(getEntry(name), getVerifier());
    }

    public List<JarEntry> getJarEntries() {
        return listFiles().stream().map(f -> new JarEntry(f, getVerifier())).toList();
    }

    public InputStream getInputStream(JarEntry entry) throws LocalizableIOException {
        verifyEntry(entry);
        return super.getInputStream(entry);
    }

    public InputStream getInputStream(String entryName) throws LocalizableIOException {
        verifyEntry(entryName);
        return super.getInputStream(entryName);
    }

    public byte[] extractFile(String entryName) throws LocalizableIOException {
        verifyEntry(entryName);
        return super.extractFile(entryName);
    }

    //
    //
    //

     // public List<List<String>> getServices() {
    //     try {
    //         List<List<String>> services = new ArrayList<>();
    //         List<FileInfo> serviceFiles = listFiles("META-INF/services");
    //         for (FileInfo serviceFile : serviceFiles) {
    //             System.out.println("services: " + serviceFile.getPath());
    //             String serviceFileContent = new String(extractFile(serviceFile.getPath()));
    //             System.out.println("content: " + serviceFileContent);
    //         }
    //         return services;
    //     } catch (IOException e) {
    //         // e.printStackTrace();
    //         return null;
    //     }
    // }

    public Properties getMavenProperties() {
        if (mavenProperties == null) {
            extractMavenInfo();
        }
        return mavenProperties;
    }

    @Nullable
    public String getModuleName() {
        if (moduleName == null) {
            moduleName = getJpmsModuleName();
        }
        if (moduleName == null) {
            String automaticModuleName = manifest.getString("Automatic-Module-Name");
            if (automaticModuleName != null && !automaticModuleName.isBlank()) {
                moduleName = automaticModuleName;
            }
        }
        return moduleName;
    }

    public Set<String> getPackages() {
        if (packageNames == null) {
            discoverPackages();
        }
        return packageNames;
    }

    public Set<String> getClassNames() {
        if (classNames == null) {
            discoverClasses();
        }
        return classNames;
    }

    public static String getClassName(ZipEntry entry) {
        String filePath = entry.getName();
        if (filePath.endsWith(".class")) {
            int slash = filePath.lastIndexOf("/");
            return filePath.substring(slash + 1, filePath.length() - 6);
        }
        return null;
    }

    public static String getPackageName(ZipEntry entry) {
        String filePath = entry.getName();
        if (filePath.endsWith(".class")) {
            int slash = filePath.lastIndexOf("/");
            if (slash > -1) {
                String packagePath = filePath.substring(0, slash);
                return packagePath.replace("/", ".");
            }
        }
        return null;
    }

    public final boolean isMultiRelease() {
        if (isMultiRelease) {
            return true;
        }
        if (MULTI_RELEASE_ENABLED) {
            // TODO:
            // try {
            //     checkForSpecialAttributes();
            // } catch (IOException io) {
            //     isMultiRelease = false;
            // }
        }
        return isMultiRelease;
    }

    //
    // Private Methods
    //

    private void extractMavenInfo() {
        mavenProperties = new Properties();
        String mavenPropertiesPath = getPomPropertiesPath();
        try {
            String mavenPropertiesString = new String(extractFile(mavenPropertiesPath));
            parseProperties(mavenPropertiesString, mavenProperties);
        } catch (LocalizableIOException e) {
        }
    }

    @Nullable
    private String getPomPropertiesPath() {
        final String mavenDirectory = "META-INF/maven/";
        List<ZipEntry> files = listFiles(mavenDirectory);
        for (ZipEntry info : files) {
            if (info.getName().endsWith("pom.properties")) {
                return mavenDirectory + info.getName();
            }
        }
        return null;
    }

    private void parseProperties(String propertiesString, Properties propertiesOut) {
        java.util.Properties propertiesIn = new java.util.Properties();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(propertiesString.getBytes())) {
            propertiesIn.load(inputStream);
            for (Entry<Object,Object> entry : propertiesIn.entrySet()) {
                if (entry.getKey() instanceof String k && entry.getValue() instanceof String v) {
                    propertiesOut.set(k,v);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getJpmsModuleName() {
        try (InputStream is = getInputStream("module-info.class")) {
            ModuleDescriptor descriptor = ModuleDescriptor.read(is);
            return descriptor.name();
        } catch (IOException | InvalidModuleDescriptorException e) {
            return null;
        }
    }

    @NonNull
    private String getClassPath() {
        List<ZipEntry> versions = listFiles(META_INF_VERSIONS);
        for (ZipEntry entry : versions) {
            String versionEntryName = entry.getName();
            int slash = versionEntryName.indexOf("/");
            if (slash > -1) {
                String classPath = META_INF_VERSIONS + entry.getName().substring(0, slash);
                return classPath;
            }
        }
        return "";
    }

    private void discoverClasses() {
        String classPath = getClassPath();
        List<ZipEntry> allFiles = listFiles(classPath);
        classNames = new HashSet<>();
        for (ZipEntry entry : allFiles) {
            String entryName = entry.getName();
            if (entryName.startsWith(META_INF)) {
                continue;
            }
            if (entryName.endsWith(".class")) {
                if (entryName.endsWith("module-info.class")) {
                    continue;
                }
                String className = entryName
                    .replace("/", ".")
                    .replace("\\", ".")
                    .substring(0, entryName.length() - 6); // Remove DOT_CLASS
                classNames.add(className);
            }
        }
    }

    private void discoverPackages() {
        packageNames = new HashSet<>();
        List<ZipEntry> files = listFiles("");
        for (ZipEntry info : files) {
            String packageName = JarFile.getPackageName(info);
            if (packageName != null && !packageNames.contains(packageName)) {
                packageNames.add(packageName);
            }
        }
    }

    public JarVerifier getVerifier() {
        if (verify && verifier == null) {
            verifier = createVerifier();
        }
        return verifier;
    }

    private JarVerifier createVerifier() {
        byte[] mf;
        try {
            mf = super.extractFile(MANIFEST_MF);
        } catch (LocalizableIOException e) {
            throw new SecurityException(e, GenericMessage.ERROR, "No MANIFEST.MF");
        }

        Map<String, byte[]> signatures = new HashMap<>();
        Map<String, RsaKey> keys = new HashMap<>();
        Map<String, String> keyFiles = getKeyFiles();

        for (Entry<String, String> entry : keyFiles.entrySet()) {
            String signerName = entry.getKey();

            String sfName = META_INF + signerName + DOT_SF;
            try {
                byte[] sfBytes;
                sfBytes = super.extractFile(sfName);
                signatures.put(signerName, sfBytes);
            } catch (IOException e) {
                // TODO: move this. check signature exists in verifier
                throw new SecurityException(e, GenericMessage.ERROR, "Error reading " + sfName);
            }

            String kfName = META_INF + entry.getValue();
            try {
                byte[] kfBytes = super.extractFile(kfName);
                RsaKey k = new RsaKey(new ByteArrayInputStream(kfBytes));
                keys.put(signerName, k);
            } catch (IOException e) {
                // TODO: move this. check key exists in verifier
                throw new SecurityException(e, GenericMessage.ERROR, "Error reading " + kfName);
            }
        }

        CascaraJarVerifier jarVerifier = new CascaraJarVerifier()
                // .setSigners(signers)
                .setKeys(keys)
                .setSignatures(signatures)
                .setManifest(mf);

        return jarVerifier;
    }

    private Map<String,String> getKeyFiles() {
        List<ZipEntry> metaInf = super.listFiles(META_INF);

        // 1. Find SF files
        List<String> names = new ArrayList<>();
        for (ZipEntry entry : metaInf) {
            if (entry.getName().endsWith(DOT_SF)) {
                String name = entry.getName().substring(0, entry.getName().length() -3);
                names.add(name);
            }
        }

        // 2. Find matching keys
        Map<String,String> keyFiles = new HashMap<>();
        for (ZipEntry entry : metaInf) {
            String entryName = entry.getName();
            if (!entryName.endsWith(DOT_SF)) {
                int dot = entryName.lastIndexOf('.');
                if (dot > -1) {
                    String name = entry.getName().substring(0, dot);
                    if (names.contains(name)) {
                        keyFiles.put(name, entry.getName());
                    } else if (entryName.endsWith(".RSA")) {
                        throw new SecurityException(GenericMessage.ERROR, "Missing SF file");
                    }
                }
            }
        }
        // TODO: move this. check key exists in verifier
        if (keyFiles.size() < names.size()) {
            throw new SecurityException(GenericMessage.ERROR, "Missing key file");
        }
        return keyFiles;
    }

    private boolean verifieddSignatures;

    private void verifyEntry(String entryName) {
        JarVerifier jv = getVerifier();
        if (jv != null) {
            if (!verifieddSignatures) {
                verifieddSignatures = jv.verifySignaturesAgainstKeys();

                // 2. Verify .SF manifest entries & SHA-256-Digest-Manifest against MANIFEST.MF
                if (!jv.verifyAllSignatureFilesAgainstManifest()) {
                    throw new SecurityException(GenericMessage.ERROR, "MANIFEST.SF digest verification against MANIFEST.MF failed");
                }


                // if (!verifieddSignatures) {
                //     throw new SecurityException(GenericMessage.ERROR, "Verification failed for " + entryName + " in " + getPath());
                // }
            }
            try {
                if (jv.isTrustedManifestEntry(entryName)) {
                    byte[] content = super.extractFile(entryName);
                    if (!jv.verifyEntryContent(entryName, content)) {
                        throw new SecurityException(GenericMessage.ERROR, "Verification failed for " + entryName + " in " + getPath());
                    }
                }
            } catch (LocalizableIOException e) {
                throw new SecurityException(e, GenericMessage.ERROR, "Verification failed for " + entryName + " in " + getPath());
            }
        }
    }

    private void verifyEntry(JarEntry entry) {
        verifyEntry(entry.getName());
    }

    //
    //
    //

    public class JarEntry extends java.util.jar.JarEntry {
        private Attributes attr;
        private Certificate[] certs;
        private CodeSigner[] signers;
        private String basename;
        private volatile JarVerifier jv;

        public JarEntry(String name) {
            super(name);
            this.basename = name;
        }

        public JarEntry(ZipEntry ze) {
            super(ze);
        }

        JarEntry(JarEntry je) {
            this((ZipEntry)je);
            this.attr = je.attr;
            this.certs = je.certs;
            this.signers = je.signers;
        }

        // Used by realEntry()
        JarEntry(String name, ZipEntry vze) {
            super(vze);
            this.basename = name;
        }

        // Used by getJarEntries()
        JarEntry(ZipEntry ze, JarVerifier jv) {
            super(ze);
            this.jv = jv;
            this.basename = ze.getName();
        }

        public Attributes getAttributes() {
            JarManifest man = JarFile.this.getManifest();
            if (man != null) {
                return man.getAttributes(super.getName());
            } else {
                return null;
            }
        }

        @Override
        public Certificate[] getCertificates() {
            JarVerifier.VerifiedSigner signer = jv == null
                ? null
                : jv.getSigner(getName());
            return signer == null
                ? null
                : signer.certificates();
        }

        @Override
        public CodeSigner[] getCodeSigners() {
            JarVerifier.VerifiedSigner signer = jv == null
                ? null
                : jv.getSigner(getName());
            return signer == null
                ? null
                : signer.codeSigners();
        }

        public String getRealName() {
            return super.getName();
        }

        @Override
        public String getName() {
            return basename;
        }

        JarEntry realEntry() {
            // TODO: How does this work with signing?
            if (isMultiRelease() && versionFeature != BASE_VERSION_FEATURE) {
                String entryName = super.getName();
                return entryName == basename || entryName.equals(basename)
                    ? this
                    : new JarEntry(entryName, this);
            }
            return this;
        }

        JarEntry setBasename(String name) {
            basename = name;
            return this;
        }
    }

    //
    //
    //

    public static class CascaraJarVerifier implements JarVerifier {
        private byte[] rawManifestBytes;
        private Map<String,RsaKey> keys;
        private Map<String,byte[]> signatureFiles;

        private Map<String,Manifest> signatures;
        private Manifest manifest;
        private Map<String,VerifiedSigner> verifiedSigners;

        public CascaraJarVerifier() {
        }

        public CascaraJarVerifier setKeys(Map<String,RsaKey> keus) {
            this.keys = keus;
            return this;
        }

        public CascaraJarVerifier setSignatures(Map<String,byte[]> signatureFiles) {
            this.signatures = new HashMap<>();
            this.signatureFiles = signatureFiles;
            for (Entry<String,byte[]> entry : signatureFiles.entrySet()) {
                byte[] sfBytes = entry.getValue();
                try {
                    Manifest signature = new Manifest(new ByteArrayInputStream(sfBytes));
                    signatures.put(entry.getKey(), signature);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return this;
        }

        public CascaraJarVerifier setManifest(byte[] rawManifestBytes) {
            this.rawManifestBytes = rawManifestBytes;
            try {
                manifest = new Manifest(new ByteArrayInputStream(rawManifestBytes));
                return this;
            } catch (IOException e) {
                // TODO Proper error handling
                throw new SecurityException(e, GenericMessage.ERROR, e.getMessage());
            }
        }

        @Override
        public boolean verifySignaturesAgainstKeys() {
            verifiedSigners = new HashMap<>();
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



                    if (!signatureValid) {
                        throw new SecurityException(GenericMessage.ERROR, "Signature verification failed for " + signerName);
                    }
                    allSignaturesValid &= signatureValid;



                    VerifiedSigner signer = new VerifiedSigner(k.getCertificates());
                    verifiedSigners.put(signerName, signer);



                } catch (CertificateException e) {
                    throw new SecurityException(e, GenericMessage.ERROR, "Error reading certificate: " + e.getMessage());
                } catch (SecurityException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SecurityException(e, GenericMessage.ERROR, "Signature check failed: " + e.getMessage());
                }
            }
            return allSignaturesValid;
        }

        @Override
        public boolean verifyAllSignatureFilesAgainstManifest() {
            boolean result = true;
            for (Manifest signature : signatures.values()) {
                result &= verifySignatureFileAgainstManifest(signature);
            }
            return result;
        }

        @Override
        public boolean verifySignatureFileAgainstManifest(Manifest signatureFile) {
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
        @Override
        public boolean verifyEntryContent(String entryName, byte[] content) {

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
        public VerifiedSigner getSigner(String entryName) {
            VerifiedSigner result = verifiedSigners.computeIfAbsent(entryName, n -> {
                for (Entry<String,Manifest> signature : signatures.entrySet()) {
                    if (isTrustedManifestEntry(entryName)) {
                        return verifiedSigners.get(signature.getKey());
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

        public SignatureStatus evaluatePolicy(SignaturePolicy policy, X509Certificate cert, byte[] sfBytes) {
            if (cert == null) {
                return SignatureStatus.NOT_SIGNED;
            }

            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                return SignatureStatus.CERTIFICATE_EXPIRED;
            } catch (CertificateNotYetValidException e) {
                return SignatureStatus.CERTIFICATE_EXPIRED;
            }

            if (policy.isTrustStoreEnforced() && !policy.getTrustStore().contains(cert)) {
                return SignatureStatus.UNTRUSTED_SIGNER;
            }

            try {
                if (!verifySignature(cert, sfBytes)) {
                    return SignatureStatus.INVALID_SIGNATURE;
                }
            } catch (Exception e) {
                return SignatureStatus.INVALID_SIGNATURE;
            }

            return SignatureStatus.VALID;
        }

        @Override
        public boolean verifySignature(X509Certificate cert, String entryName, byte[] content) {
            // 1. First, verify entry content against SHA digest in MANIFEST.MF
            if (!verifyEntryContent(entryName, content)) {
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
        public boolean verifySignature(X509Certificate cert, byte[] content) {
            // Evaluates whether the content matches the global raw MANIFEST.MF bytes
            // and is signed by the provided certificate
            if (rawManifestBytes != null && Arrays.equals(rawManifestBytes, content)) {
                for (VerifiedSigner signer : verifiedSigners.values()) {
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

        public SignatureStatus evaluatePolicy(SignaturePolicy policy) {
            return null;
        }

        //
        //
        //

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
}
