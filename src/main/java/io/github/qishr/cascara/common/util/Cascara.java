package io.github.qishr.cascara.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.UnexpectedNullParameterException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.semver.SemVerException;

public class Cascara {
    private static Cascara INSTANCE;
    private static String ACTIVE_VERSION = "active-version";
    private static String CASCARA_PROPERTIES = "cascara.properties";
    private static String CONTENT_TYPES = "content-types.yaml";
    private static String SCHEMAS = "schemas";

    private String homeEnvVar;
    private Path homePath;

    public static SemVer getVersion() {
        JarManifest manifest;
        try {
            manifest = JarManifest.parse(JreUtils.getResourceAsString(Cascara.class, "/META-INF/MANIFEST.MF"));
        } catch (IOException e) {
            return new SemVer("0.0.0");
        }
        return new SemVer(manifest.getString("Cascara-Version", "0.0.0"));
    }

    public static String getHomeEnvVar() {
        return instance().homeEnvVar;
    }

    public static Path getHomePath() {
        return instance().homePath;
    }

    public static Path getContentTypesPath() {
        Path activeVersionPath = getActiveVersionPath();
        return activeVersionPath.resolve(CONTENT_TYPES);
    }

    public static Path getSchemasPath() {
        Path activeVersionPath = getActiveVersionPath();
        return activeVersionPath.resolve(SCHEMAS);
    }

    public static List<String> getInstalledVersions() {
        List<String> versions = new ArrayList<>();
        File[] files = getHomePath().toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (isVersionDirectory(file)) {
                    versions.add(file.getName());
                }
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

    //
    // Private Helpers
    //

    private Cascara() {
        homePath = Paths.get(getHome());
    }

    private static Cascara instance() {
        if (INSTANCE == null) {
            INSTANCE = new Cascara();
        }
        return INSTANCE;
    }

    private static Path getSharedPath() {
        return getHomePath().resolve("shared");
    }

    private static boolean isVersionDirectory(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        String name = file.getName();
        try {
            toSemVer(name);
            return true;
        } catch (SemVerException e) {
            return false;
        }
    }

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

    private String tryGetHome(String envVar) {
        String home = System.getenv(envVar);
        if (home != null) {
            homeEnvVar = envVar;
        }
        return home;
    }
}
