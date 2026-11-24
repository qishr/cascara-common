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
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.security.SecurityConfig;
import io.github.qishr.cascara.common.security.SecurityMessage;
import io.github.qishr.cascara.common.security.exception.SignatureException;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.security.verifier.CascaraJarVerifier;
import io.github.qishr.cascara.common.security.verifier.JarVerifier;

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
    private boolean signaturesChecked;

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

        String jarVersion = System.getProperty("jdk.util.jar.version");

        int runtimeVersion = Runtime.version().feature();
        if (jarVersion != null) {
            int jarVer = Integer.parseInt(jarVersion);
            runtimeVersion = (jarVer > runtimeVersion)
                    ? runtimeVersion
                    : Math.max(jarVer, BASE_VERSION_FEATURE);
        }
        RUNTIME_VERSION = Runtime.Version.parse(Integer.toString(runtimeVersion));

        String enableMultiRelease = System.getProperty("jdk.util.jar.enableMultiRelease", "true");

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

    public static JarFile open(Path jarPath) throws IOException {
        return new JarFile(jarPath, false, true, BASE_VERSION);
    }

    @Experimental
    public static JarFile open(Path jarPath, boolean verify) throws IOException {
        return new JarFile(jarPath, false, verify, BASE_VERSION);
    }

    private JarFile(Path jarPath, boolean create, boolean verify, Runtime.Version version) throws IOException {
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

    public JarManifest getManifest() {
        return manifest;
    }

    public JarEntry getJarEntry(String name) {
        return new JarFileEntry(getEntry(name), getVerifier());
    }

    public List<JarEntry> getJarEntries() {
        return getEntries().stream().map(f -> (JarEntry) new JarFileEntry(f, getVerifier())).toList();
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        if (entry instanceof JarFileEntry jarEntry) {
            verifyEntry(jarEntry);
        }
        return super.getInputStream(entry);
    }

    public InputStream getInputStream(String entryName) throws IOException {
        verifyEntry(entryName);
        return super.getInputStream(entryName);
    }

    public byte[] extractFile(String entryName) throws IOException {
        verifyEntry(entryName);
        return super.extractFile(entryName);
    }

    //
    //
    //

    // public List<List<String>> getServices() {
    //     try {
    //         List<List<String>> services = new ArrayList<>();
    //         List<FileInfo> serviceFiles = getEntries("META-INF/services");
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


    // TODO:
    // - static Runtime.Version runtimeVersion()
    // - streaming API (in ZipFile?)

    //
    // Private Methods
    //

    private void extractMavenInfo() {
        mavenProperties = new Properties();
        String mavenPropertiesPath = getPomPropertiesPath();
        try {
            String mavenPropertiesString = new String(extractFile(mavenPropertiesPath));
            parseProperties(mavenPropertiesString, mavenProperties);
        } catch (IOException e) {
        }
    }

    @Nullable
    private String getPomPropertiesPath() {
        final String mavenDirectory = "META-INF/maven/";
        List<ZipEntry> files = getEntries(mavenDirectory);
        for (ZipEntry info : files) {
            if (info.getName().endsWith("pom.properties")) {
                return info.getName();
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
        // TODO: super.getInputStream doesn't verify, regular getInputStream does.
        // What is supposed to happen here?
        try (InputStream is = super.getInputStream("module-info.class")) {
            ModuleDescriptor descriptor = ModuleDescriptor.read(is);
            return descriptor.name();
        } catch (IOException | InvalidModuleDescriptorException e) {
            return null;
        }
    }

    @SuppressWarnings("null")
    @NonNull
    private String getClassRoot() {
        List<ZipEntry> versions = getEntries(META_INF_VERSIONS);
        for (ZipEntry entry : versions) {
            String versionEntryName = entry.getName() ;
            int slash = versionEntryName.indexOf("/", META_INF_VERSIONS.length() + 1);
            if (slash > -1) {
                return versionEntryName.substring(0, slash + 1);
            }
        }
        return "";
    }

    private void discoverClasses() {
        String classRoot = getClassRoot();
        List<ZipEntry> allFiles = getEntries(classRoot);
        classNames = new HashSet<>();
        for (ZipEntry entry : allFiles) {
            // String entryName = entry.getName();
            String entryName = entry.getName().substring(classRoot.length());

            // TODO: multirelease JAR
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
        List<ZipEntry> files = getEntries("");
        for (ZipEntry info : files) {
            String packageName = JarFile.getPackageName(info);
            if (packageName != null && !packageNames.contains(packageName)) {
                packageNames.add(packageName);
            }
        }
    }

    private JarVerifier getVerifier() {
        if (verify && verifier == null) {
            try {
                verifier = createVerifier();
            } catch (ClassNotFoundException | InstantiationException | IOException e) {
                throw new LocalizableSecurityException(e, SecurityMessage.VERIFIER_INIT_FAILED, e.getLocalizedMessage());
            }
        }
        return verifier;
    }

    /// @throws IoException
    private JarVerifier createVerifier() throws IOException, InstantiationException, ClassNotFoundException {
        byte[] mf = super.extractFile(MANIFEST_MF);

        Map<String, byte[]> signatures = new HashMap<>();
        Map<String, byte[]> keys = new HashMap<>();
        Map<String, String> keyFileNames = getKeyFilenamesByAlias();

        for (Entry<String, String> entry : keyFileNames.entrySet()) {
            String alias = entry.getKey();

            String sfName = META_INF + alias + DOT_SF;
            byte[] sfBytes = super.extractFile(sfName);
            signatures.put(alias, sfBytes);

            String kfName = META_INF + entry.getValue();
            byte[] kfBytes = super.extractFile(kfName);
            keys.put(alias, kfBytes);
        }

        SecurityConfig config = SecurityConfig.get();
        JarVerifier verifier = null;
        if (!config.getJarVerifier().isEmpty()) {
            Class<?> jvClass = Class.forName(config.getJarVerifier());
            Object instance = ReflectionUtils.createInstance(jvClass);
            if (instance instanceof JarVerifier jv) {
                verifier = jv;
            }
        }

        if (verifier == null) {
            verifier = new CascaraJarVerifier();
        }

        return verifier
                .setKeys(keys)
                .setSignatures(signatures)
                .setManifest(mf);
    }

    private Map<String,String> getKeyFilenamesByAlias() {
        List<ZipEntry> metaInf = super.getEntries(META_INF);

        // 1. Find SF files
        List<String> aliases = new ArrayList<>();
        for (ZipEntry entry : metaInf) {
            if (entry.getName().endsWith(DOT_SF)) {
                aliases.add(entry.getName().substring(META_INF.length(), entry.getName().length() -3));
            }
        }

        // 2. Find matching keys
        Map<String,String> keyFiles = new HashMap<>();
        for (ZipEntry entry : metaInf) {
            String fileName = entry.getName().substring(META_INF.length());
            if (!fileName.endsWith(DOT_SF)) {
                int dot = fileName.lastIndexOf('.');
                if (dot > -1) {
                    String alias = fileName.substring(0, dot);
                    String suffix = fileName.substring(dot + 1);

                    switch (suffix) {
                        case "DSA", "RSA", "EC":
                            if (aliases.contains(alias)) {
                                keyFiles.put(alias, fileName);
                            } else {
                                // TODO: This should be in CascaraJarVerifier...
                                // TODO: Proper exception
                                throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Missing SF file");
                            }
                    }

                }
            }
        }

        // TODO: This should be in CascaraJarVerifier...
        if (keyFiles.size() < aliases.size()) {
            // TODO: Proper exception
            throw new SignatureException(SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Missing key file");
        }

        return keyFiles;
    }

    private void verifyEntry(JarFileEntry entry) throws IOException {
        verifyEntry(entry.getName());
    }

    private void verifyEntry(String entryName) throws IOException {
        JarVerifier jv = getVerifier();
        if (jv != null) {
            if (!signaturesChecked) {
                signaturesChecked = true;
                jv.verifyJarSignature();
            }

            // Skip signature metadata files in META-INF that aren't content payloads
            if (isSignatureMetadataEntry(entryName)) {
                return;
            }

            // For all other entries, extract and verify
            // if (jv.isTrustedManifestEntry(entryName)) {
                byte[] content = super.extractFile(entryName);
                jv.verifyEntrySignature(entryName, content);
            // }
        }
    }

    private static boolean isSignatureMetadataEntry(String entryName) {
        String upper = entryName.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) {
            return false;
        }
        // Exclude manifest and signature block/file patterns
        return upper.equals("META-INF/MANIFEST.MF") ||
               upper.endsWith(".SF") || upper.endsWith(".RSA") ||
               upper.endsWith(".DSA") || upper.endsWith(".EC");
    }

    //
    //
    //

    public class JarFileEntry extends JarEntry {
        private Attributes attr;
        private Certificate[] certs;
        private CodeSigner[] signers;
        private String basename;
        private volatile JarVerifier jv;

        public JarFileEntry(String name) {
            super(name);
            this.basename = name;
        }

        public JarFileEntry(ZipEntry ze) {
            super(ze);
        }

        JarFileEntry(JarFileEntry je) {
            this((ZipEntry)je);
            this.attr = je.attr;
            this.certs = je.certs;
            this.signers = je.signers;
            this.basename = je.basename;
        }

        // Used by realEntry()
        JarFileEntry(String name, ZipEntry vze) {
            super(vze);
            this.basename = name;
        }

        // Used by getJarEntries()
        JarFileEntry(ZipEntry ze, JarVerifier jv) {
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
            if (certs == null) {
                JarVerifier.VerifiedSigner signer = jv == null
                    ? null
                    : jv.getSigner(getName());
                certs = signer == null
                    ? null
                    : signer.certificates();
            }
            return certs;
        }

        @Override
        public CodeSigner[] getCodeSigners() {
            if (signers == null) {
                JarVerifier.VerifiedSigner signer = jv == null
                    ? null
                    : jv.getSigner(getName());
                signers = signer == null
                    ? null
                    : signer.codeSigners();
            }
            return signers;
        }

        public String getRealName() {
            return super.getName();
        }

        @Override
        public String getName() {
            return basename;
        }

        @NonNull
        JarFileEntry realEntry() {
            // TODO: How does this work with signing?
            if (isMultiRelease() && versionFeature != BASE_VERSION_FEATURE) {
                String entryName = super.getName();
                return entryName == basename || entryName.equals(basename)
                    ? this
                    : new JarFileEntry(entryName, this);
            }
            return this;
        }

        @NonNull
        JarFileEntry setBasename(String name) {
            basename = name;
            return this;
        }
    }
}
