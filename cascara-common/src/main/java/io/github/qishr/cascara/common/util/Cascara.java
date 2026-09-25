package io.github.qishr.cascara.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.UnexpectedNullParameterException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.semver.SemVerException;

public class Cascara {
    private static Cascara INSTANCE;
    private static final String ACTIVE_VERSION = "active-version";
    private static final String CASCARA_PROPERTIES = "cascara.properties";
    private static final String CONTENT_TYPES = "content-types.yaml";
    private static final String SCHEMAS = "schemas";
    private static final String SPL_PREFS = "spl.properties";
    private static final String MODULEPATH = "modulepath";

    private String homeEnvVar;
    private Path homePath;

    public static boolean isFileTimeSupported() {
        return !getHomePath().toUri().getScheme().equals("jar");
    }

    // public static SemVer getVersion() {
    //     try {
    //         JarManifest manifest = JarManifest.parse(JreUtils.getResourceAsString(Cascara.class, "/META-INF/MANIFEST.MF"));
    //         return new SemVer(manifest.getString("Cascara-Version", "0.0.0"));
    //     } catch (IOException e) {
    //         // System.err.println("Failed to parse JarManifest: " + e.getMessage());
    //         return new SemVer("0.0.0");
    //     }
    // }

    public static SemVer getVersion() {
        try {
            var codeSource = Cascara.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                URL location = codeSource.getLocation();

                if (location.getPath().endsWith(".jar")) {
                    // Packaged JAR execution
                    try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new File(location.toURI()))) {
                        java.util.jar.Manifest manifest = jarFile.getManifest();
                        if (manifest != null) {
                            String ver = manifest.getMainAttributes().getValue("Cascara-Version");
                            if (ver != null && !ver.isBlank()) {
                                return new SemVer(ver);
                            }
                        }
                    }
                } else {
                    // Exploded directory execution (IDEs / Gradle tasks)
                    File classesDir = new File(location.toURI());

                    // 1. Direct output directory (classes/java/main/META-INF/MANIFEST.MF)
                    File manifestFile = new File(classesDir, "META-INF/MANIFEST.MF");

                    // 2. Sibling resources output directory (resources/main/META-INF/MANIFEST.MF)
                    if (!manifestFile.exists() && classesDir.getParentFile() != null) {
                        manifestFile = new File(classesDir.getParentFile(), "resources/main/META-INF/MANIFEST.MF");
                    }

                    if (manifestFile.exists()) {
                        try (InputStream is = new FileInputStream(manifestFile)) {
                            java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
                            String ver = manifest.getMainAttributes().getValue("Cascara-Version");
                            if (ver != null && !ver.isBlank()) {
                                return new SemVer(ver);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new SemVer("0.0.0");
    }

    public static String getHomeEnvVar() {
        return instance().homeEnvVar;
    }

    public static Path getHomePath() {
        return instance().homePath;
    }

    @Experimental
    public static void setHomePath(Path path) {
        instance().homeEnvVar = null;
        instance().homePath = path;
    }

    /// The path of the content types registry file.
    public static Path getContentTypesPath() {
        return getSharedPath().resolve(CONTENT_TYPES);
    }

    /// The path of the schema store irectory.
    public static Path getSchemasPath() {
        return getSharedPath().resolve(SCHEMAS);
    }

    /// The path of the properties file containing definitions of default or preferred service providers
    /// If this file is empty, or if a service is not listed in it, when a provider is requested the
    /// first  matching provider will be returned by SPL.
    public static Path getSplPropertiesPath() {
        return getActiveVersionPath().resolve(SPL_PREFS);
    }

    public static Path getModulePath() {
        return getActiveVersionPath().resolve(MODULEPATH);
    }

    public static List<String> getInstalledVersions() {
        List<String> versions = new ArrayList<>();
        Path homePath = getHomePath();

        if (Files.isDirectory(homePath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(homePath)) {
                for (Path entry : stream) {
                    if (isVersionDirectory(entry)) {
                        versions.add(entry.getFileName().toString());
                    }
                }
            } catch (IOException e) {
                // Return empty list or log depending on error handling policy
            }
        }

        return versions;
    }
    @Nullable
    public static Path getActiveVersionPath() {
        String version = getActiveVersionString();
        if (version == null) {
            return getHomePath();
        }
        return getHomePath().resolve(version);
    }

    // TODO: Cache this and only re-calculate if properties file is modified
    @Nullable
    public static String getActiveVersionString() {
        List<String> installedVersions = getInstalledVersions();
        Path path = getSharedPath().resolve(CASCARA_PROPERTIES);
        if (Files.isRegularFile(path)) {
            try {
                Properties properties = Properties.load(path);
                String version = properties.get(ACTIVE_VERSION);
                if (version != null && !version.isBlank()) {
                    if (installedVersions.contains(version)) {
                        return version;
                    }
                }
            } catch (IOException e) {}
        }
        String highestVersionString = null;
        SemVer highest = new SemVer("0.0.0");
        for (String versionString : installedVersions) {
            SemVer version = toSemVer(versionString);
            if (version.isGreaterThan(highest)) {
                highest = version;
                highestVersionString = versionString;
            }
        }
        return highestVersionString;
    }

    public static SemVer toSemVer(String version) {
        if (version == null || version.isBlank()) {
            throw new UnexpectedNullParameterException("version");
        }
        int dots = 0;
        for (int i = 0; i < version.length(); i++) {
            if (version.charAt(i) == '.') {
                dots++;
            }
        }
        if (dots == 2) {
            return new SemVer(version);
        }
        if (dots == 1) {
            return new SemVer(version + ".0");
        }
        return new SemVer(version + ".0.0");
    }

    public static Path getSharedPath() {
        return getHomePath().resolve("shared");
    }

    //
    // Private Helpers
    //

    private Cascara() {
        homePath = resolvePath(getHome());
    }

    private static Cascara instance() {
        if (INSTANCE == null) {
            INSTANCE = new Cascara();
        }
        return INSTANCE;
    }

    private static Path resolvePath(String pathString) {
        return Path.of(pathString);
    }

    private static boolean isVersionDirectory(Path file) {
        if (!Files.isDirectory(file)) {
            return false;
        }
        String name = file.getFileName().toString();
        try {
            toSemVer(name);
            return true;
        } catch (SemVerException e) {
            return false;
        }
    }

    @Nullable
    private String getHome() {
        String home = tryGetHome("CASCARA_HOME");
        if (home == null) {
            home = tryGetHome("CASC_HOME");
        }
        if (home == null) {
            String userHome = System.getProperty("user.home");
            home = userHome + File.separator + ".cascara";
        }
        return home;
    }

    @Nullable
    private String tryGetHome(String envVar) {
        String home = System.getenv(envVar);
        if (home != null) {
            homeEnvVar = envVar;
        }
        return home;
    }
}
