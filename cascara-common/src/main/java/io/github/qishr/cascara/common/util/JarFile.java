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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime.Version;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSigner;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.report.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.security.ASN1Stream;
import io.github.qishr.cascara.common.util.JarManifest.ManifestDigester;

public class JarFile extends ArchiveFile {
    private static final String META_INF = "META-INF/";
    private static final String META_INF_VERSIONS = "META-INF/versions/";
    static final String INDEX_NAME = "META-INF/INDEX.LIST";
    public static final String MANIFEST_NAME = META_INF + "MANIFEST.MF";


    private JarManifest manifest;
    private Properties mavenProperties;
    private Set<String> packageNames = null;
    private Set<String> classNames = null;
    private String moduleName = null;
    private boolean verify;
    private Version version;

    private X509Certificate signerCert;
    private X509Certificate intermediateCert;
    private X509Certificate rootCert;

    // private java.util.jar.JarFile jujJarFile;

    private static final Runtime.Version BASE_VERSION;
    private static final int BASE_VERSION_FEATURE;
    private static final Runtime.Version RUNTIME_VERSION;
    private static final boolean MULTI_RELEASE_ENABLED;
    private static final boolean MULTI_RELEASE_FORCED;
    private final int versionFeature;

    private boolean isMultiRelease;
    private Map<String, VerifiedSigner> verifiedEntries;

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


        String jarManifest = new String(extractFile("META-INF/MANIFEST.MF"));
        manifest = JarManifest.parse(jarManifest);
        initSecurity();
    }



    public Path getPath() {
        return archivePath;
    }

    public JarManifest getManifest() {
        return manifest;
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
        // if (moduleName == null) {
        //     determineModuleName();
        // }
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

    //
    // Private MEthods
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

    @Experimental
    public String guessModuleName() {
        String[] fileNameSegments = getPath().getFileName().toString().split("-");
        StringBuilder sb = new StringBuilder();
        for (String segment : fileNameSegments) {
            if (!segment.isEmpty()) {
                int firstCodePoint = segment.codePointAt(0);
                if (Character.isAlphabetic(firstCodePoint)) {
                    if (!sb.isEmpty()) {
                        sb.append(".");
                    }
                    sb.append(segment);
                } else {
                    break;
                }
            }
        }
        return sb.toString();
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

    //
    //
    //

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
            String versionEntryName = entry.getName(); //.substring(META_INF_VERSIONS.length());
            int slash = versionEntryName.indexOf("/");
            if (slash > -1) {
                String classPath = META_INF_VERSIONS + entry.getName().substring(0, slash);
                // System.out.println("classPath: " + classPath);
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
            // System.out.println("entryName: " + entryName);
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

    //
    // Verification
    //

    private List<Certificate> chain;

    private void initSecurity() {
        verifiedEntries = new HashMap<>();

        // TODO: Set these values...

        // private X509Certificate signerCert;
        // private X509Certificate intermediateCert;
        // private X509Certificate rootCert;

        // Or just populate: List<Certificate> chain

        chain = new ArrayList<>();
        try {
            byte[] sfContent = extractFile("META-INF/SERVER.SF");
            byte[] signatureBlock = extractFile("META-INF/SERVER.RSA");

            // JAR signature blocks commonly hold a detached signature over the .SF file.
            // CMSSignedData cms = new CMSSignedData(
            //     new CMSByteArray(sfContent),
            //     signatureBlock
            // );



            // try {
            //     Path path = Paths.get("/Users/sandy/security/t/META-INF/SERVER.RSA");
            //     byte[] privKeyByteArray = Files.readAllBytes(path);

            //     PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKeyByteArray);
            //     KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //     PrivateKey myPrivKey = keyFactory.generatePrivate(keySpec);
            //     System.out.println("Algorithm: " + myPrivKey.getAlgorithm());
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }



            // ASN1Stream s = new ASN1Stream(sf);
            // CertificateFactory certFactory;
            // try {

            //     certFactory = CertificateFactory.getInstance("X.509");
            //     InputStream in = new ByteArrayInputStream(sf);
            //     X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);
            //     // if (cert instanceof Certificate c) {
            //     //     chain.add(c);
            //     // }
            //     chain.add(cert);
            // } catch (CertificateException e) {
            //     // TODO Auto-generated catch block
            //     e.printStackTrace();
            // }





            // PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKeyByteArray);
            // KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            // PrivateKey myPrivKey = keyFactory.generatePrivate(keySpec);
            // System.out.println("Algorithm: " + myPrivKey.getAlgorithm());



            // chain = List.of(signerCert, intermediateCert, rootCert);
        } catch (LocalizableIOException e) {
            e.printStackTrace();
        }

    }

    public JarEntry getJarEntry(String name) {
        ZipEntry zipEntry = getEntry(name);
        JarEntry jarEntry = new JarEntry(zipEntry);

        // TODO:
        // jarEntry.attr =
        // jarEntry.certs =
        // jarEntry.signers =


        // List<X509Certificate> chain = List.of(
        //     signerCert,
        //     intermediateCert,
        //     rootCert
        // );

        try {
            jarEntry.setVerification(
                new VerifiedEntry(chain)
            );
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



        VerifiedSigner signer = getSigner(name);
        jarEntry.certs = signer.certificates();
        try {
            jarEntry.signers = signer.codeSigners();
        } catch (CertificateException e) {

            // TODO Handle this properly
            e.printStackTrace();
        }
        return jarEntry;
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


    private VerifiedSigner getSigner(String entryName) {
        // TODO: populate verifiedEntries
        // VerifiedSigner signer = verifiedEntries.get(entryName);
        VerifiedSigner signer = verifiedEntries.computeIfAbsent(entryName, name -> {
            // List<X509Certificate> chain =
            return new VerifiedSigner(chain);
        });
        return signer;
    }

    //
    //
    //

    static class Verifier {
        public void verify() {
            // List<X509Certificate> chain =
            // List.of(
            //         signerCert,
            //         intermediateCert,
            //         rootCert
            // );

            // entry.setVerification(
            //         new VerifiedEntry(chain)
            // );
        }

        public boolean isTrustedManifestEntry(String name) {
            // TODO
            return false;
        }
    }

    private final static class VerifiedSigner {

        // private final List<X509Certificate> chain;
        private final List<? extends Certificate> chain;


        // private final Certificate[] certificates;
        // private final CodeSigner[] codeSigners;

        public VerifiedSigner(List<? extends Certificate> chain) {
            // certificates = null;
            // codeSigners = null;
            // chain = new ArrayList<>();
            this.chain = chain;
        }

        // public Certificate[] certificates() {
        //     return certificates;
        // }

        // public CodeSigner[] codeSigners() {
        //     return codeSigners;
        // }



        public Certificate[] certificates() {
            return chain.toArray(Certificate[]::new);
        }

        public CodeSigner[] codeSigners()
                throws CertificateException {

            CertificateFactory factory =
                    CertificateFactory.getInstance("X.509");

            CertPath certPath =
                    factory.generateCertPath(chain);

            return new CodeSigner[] {
                    new CodeSigner(certPath, null)
            };
        }
    }

    public class JarEntry extends java.util.jar.JarEntry {
        Attributes attr;
        Certificate[] certs;
        CodeSigner[] signers;
        private String basename;
        // private Verifier jv;
        private boolean jvInitialized;
        private boolean verify;
        private volatile VerifiedEntry verification;

        /// Creates a new {@code JarEntry} for the specified JAR file entry name.
        /// @param name the JAR file entry name
        /// @throws    NullPointerException if the entry name is {@code null}
        /// @throws    IllegalArgumentException if the entry name is longer than
        ///            0xFFFF bytes.
        public JarEntry(String name) {
            super(name);
            this.basename = name;
        }

        /// Creates a new {@code JarEntry} with fields taken from the
        /// specified {@code ZipEntry} object.
        /// @param ze the {@code ZipEntry} object to create the
        ///           {@code JarEntry} from
        public JarEntry(ZipEntry ze) {
            super(ze);
        }

        /// Creates a new {@code JarEntry} with fields taken from the
        /// specified {@code JarEntry} object.
        ///
        /// @param je the {@code JarEntry} to copy
        JarEntry(JarEntry je) {
            this((ZipEntry)je);
            this.attr = je.attr;
            this.certs = je.certs;
            this.signers = je.signers;
        }

        JarEntry(String name, ZipEntry vze) {
            super(vze);
            this.basename = name;
        }

        void setVerification(VerifiedEntry verification) {
            this.verification = verification;
        }

        /// Returns the {@code Manifest} {@code Attributes} for this
        /// entry, or {@code null} if none.
        ///
        /// @return the {@code Manifest} {@code Attributes} for this
        /// entry, or {@code null} if none
        public Attributes getAttributes() {
            JarManifest man = JarFile.this.getManifest();
            if (man != null) {
                return man.getAttributes(super.getName());
            } else {
                return null;
            }
        }

        /// Returns the {@code Certificate} objects for this entry, or
        /// {@code null} if none. This method can only be called once
        /// the {@code JarEntry} has been completely verified by reading
        /// from the entry input stream until the end of the stream has been
        /// reached. Otherwise, this method will return {@code null}.
        ///
        /// <p>The returned certificate array comprises all the signer certificates
        /// that were used to verify this entry. Each signer certificate is
        /// followed by its supporting certificate chain (which may be empty).
        /// Each signer certificate and its supporting certificate chain are ordered
        /// bottom-to-top (i.e., with the signer certificate first and the (root)
        /// certificate authority last).
        ///
        /// @return the {@code Certificate} objects for this entry, or
        /// {@code null} if none.
        @Override
        public Certificate[] getCertificates() {

            VerifiedEntry verification = this.verification;

            return verification == null
                    ? null
                    : verification.getCertificates();
        }
        // public Certificate[] getCertificates() {
        //     // return certs == null ? null : certs.clone();
        //     try {
        //         maybeInstantiateVerifier();
        //     } catch (IOException e) {
        //         throw new RuntimeException(e);
        //     }
        //     if (certs == null && jv != null) {
        //         certs = jv.getCerts(realEntry());
        //     }
        //     return certs == null ? null : certs.clone();
        // }



        /// Returns the {@code CodeSigner} objects for this entry, or
        /// {@code null} if none. This method can only be called once
        /// the {@code JarEntry} has been completely verified by reading
        /// from the entry input stream until the end of the stream has been
        /// reached. Otherwise, this method will return {@code null}.
        ///
        /// <p>The returned array comprises all the code signers that have signed
        /// this entry.
        ///
        /// @return the {@code CodeSigner} objects for this entry, or
        /// {@code null} if none.
        @Override
        public CodeSigner[] getCodeSigners() {

            VerifiedEntry verification = this.verification;

            return verification == null
                    ? null
                    : verification.getCodeSigners();
        }

        // public CodeSigner[] getCodeSigners() {
        //     // return signers == null ? null : signers.clone();
        //     try {
        //         maybeInstantiateVerifier();
        //     } catch (IOException e) {
        //         throw new RuntimeException(e);
        //     }
        //     if (signers == null && jv != null) {
        //         signers = jv.getCodeSigners(realEntry());
        //     }
        //     return signers == null ? null : signers.clone();
        // }

        /// Returns the real name of this {@code JarEntry}.
        ///
        /// If this {@code JarEntry} is an entry of a
        /// <a href="JarFile.html#multirelease">multi-release jar file</a> and the
        /// {@code JarFile} is configured to be processed as such, the name returned
        /// by this method is the path name of the versioned entry that the
        /// {@code JarEntry} represents, rather than the path name of the base entry
        /// that {@link #getName()} returns. If the {@code JarEntry} does not represent
        /// a versioned entry of a multi-release {@code JarFile} or the {@code JarFile}
        /// is not configured for processing a multi-release jar file, this method
        /// returns the same name that {@link #getName()} returns.
        ///
        /// @return the real name of the JarEntry
        public String getRealName() {
            return super.getName();
        }

        @Override
        public String getName() {
            return basename;
        }

        JarEntry realEntry() {
            if (isMultiRelease() && versionFeature != BASE_VERSION_FEATURE) {
                String entryName = super.getName();
                return entryName == basename || entryName.equals(basename) ?
                        this : new JarEntry(entryName, this);
            }
            return this;
        }

        // changes the basename, returns "this"
        JarEntry setBasename(String name) {
            basename = name;
            return this;
        }

        // /// Ensures that the JarVerifier has been created if one is
        // /// necessary (i.e., the jar appears to be signed.) This is done as
        // /// a quick check to avoid processing of the manifest for unsigned
        // /// jars.
        // private void maybeInstantiateVerifier() throws IOException {
        //     if (jv != null) {
        //        return;
        //     }

        //     if (verify) {
        //         // Gets the manifest name, but only if there are
        //         // signature-related files. If so we can assume
        //         // that the jar is signed and that we therefore
        //         // need a JarVerifier and Manifest

        //         // TODO:
        //         // String name = JUZFA.getManifestName(this, true);
        //         // if (name != null) {
        //         //     getManifest();
        //         //     return;
        //         // }

        //         // No signature-related files; don't instantiate a
        //         // verifier
        //         verify = false;
        //     }
        // }
    }
}
