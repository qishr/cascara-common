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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exception.UnexpectedNullValueException;
import io.github.qishr.cascara.common.i18n.exception.LocalizableIllegalStateException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.semver.SemVerException;

@ConfigNamespace("cascara")
public class Cascara extends Config {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @ConfigSetting(name = "home")
    private String physicalHome;

    @ConfigSetting(name = "properties")
    private String cascaraPropertiesPath;

    @ConfigSetting(name = "spl.properties")
    private String splPropertiesPath;

    @ConfigSetting(name = "report.properties")
    private String reportPropertiesPath;

    private static final String ACTIVE_VERSION = "active-version";
    private static final String CASCARA_PROPERTIES = "cascara.properties";
    private static final String CONTENT_TYPES = "content-types.yaml";
    private static final String SCHEMAS = "schemas";
    private static final String SPL_PROPRTTIES = "cascara.spl.properties";
    private static final String MODULEPATH = "modulepath";

    private static Path apiSpecifiedHome;

    // The path of the .cascara directory. May be on a physical or virtual file system.
    private Path homePath;

    // These paths are determined based on `home`
    // - content types path
    // - schema path
    // - shared path
    // - module path

    private String activeVersionString;

    protected Cascara() {
        // The default physicalHome
        String userHome = System.getProperty("user.home");
        physicalHome = userHome + File.separator + ".cascara";
    }

    @Override
    protected void init() {
        if (apiSpecifiedHome == null) {
            Path specifiedHome = getSpecifiedHomePath();
            homePath = specifiedHome == null
                ? Path.of(physicalHome)
                : specifiedHome;
            Config.setFileSystem(homePath.getFileSystem());
        } else {
            homePath = apiSpecifiedHome;
            physicalHome = homePath.toString();
        }

        cascaraPropertiesPath = getSharedPath().resolve(CASCARA_PROPERTIES).toString();
        splPropertiesPath = getActiveVersionPath().resolve(SPL_PROPRTTIES).toString();

        super.init();
    }

    public static boolean isInitialized() {
        return initialized.get();
    }

    public static Cascara get(Path homePath) {
        if (initialized.compareAndSet(false, true)) {
            apiSpecifiedHome = homePath;
            return get(Cascara.class, null, null, homePath.getFileSystem());
        } else {
            throw new LocalizableIllegalStateException(GenericMessage.CASCARA_CONFIG_ALREADY_INIT);
        }
    }

    public static Cascara get() {
        initialized.compareAndSet(false, true);
        return get(Cascara.class);
    }

    public Path getPath(String pathString) {
        return homePath.getFileSystem().getPath(pathString);
    }

    @SuppressWarnings("null")
    @NonNull
    public Path getHomePath() {
        return homePath;
    }

    /// The path of the content types registry file.
    public Path getContentTypesPath() {
        return getSharedPath().resolve(CONTENT_TYPES);
    }

    /// The path of the schema store irectory.
    public Path getSchemasPath() {
        return getSharedPath().resolve(SCHEMAS);
    }

    public Path getSharedPath() {
        return getHomePath().resolve("shared");
    }

    /// The path of the properties file containing definitions of default or preferred service providers
    /// If this file is empty, or if a service is not listed in it, when a provider is requested the
    /// first  matching provider will be returned by SPL.
    @SuppressWarnings("null")
    @NonNull
    public Path getSPLPropertiesPath() {
        // This should never be null
        if (splPropertiesPath == null ) {
            throw new UnexpectedNullValueException("splPropertiesPath");
        }
        return getPath(splPropertiesPath);
    }

    @Override
    public Path getPropertiesPath() {
        // This should never be null
        if (cascaraPropertiesPath == null ) {
            throw new UnexpectedNullValueException("cascaraPropertiesPath");
        }
        return getPath(cascaraPropertiesPath);
    }

    @SuppressWarnings("null")
    @NonNull
    public Path getModulePath() {
        return getActiveVersionPath().resolve(MODULEPATH);
    }

    @SuppressWarnings("null")
    @NonNull
    public Path getActiveVersionPath() {
        String version = getActiveVersionString();
        if (version == null) {
            return getHomePath();
        }
        return getHomePath().resolve(version);
    }

    public List<String> getInstalledVersions() {
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

    public SemVer getVersion() {
        try {
            JarManifest manifest = JarManifest.parse(JreUtils.getResourceAsString(Cascara.class, "/META-INF/MANIFEST.MF"));
            return new SemVer(manifest.getString("Cascara-Version", "0.0.0"));
        } catch (IOException e) {
            return new SemVer("0.0.0");
        }
    }

    public SemVer getCommonVersion() {
        try {
            JarManifest manifest = JarManifest.parse(JreUtils.getResourceAsString(Cascara.class, "/META-INF/MANIFEST.MF"));
            return new SemVer(manifest.getString("Implementation-Version", "0.0.0"));
        } catch (IOException e) {
            return new SemVer("0.0.0");
        }
    }

    @Nullable
    public String getActiveVersionString() {
        if (activeVersionString != null) {
            return activeVersionString;
        }

        List<String> installedVersions = getInstalledVersions();

        Path path = getPropertiesPath();

        if (Files.isRegularFile(path)) {
            try {
                Properties properties = Properties.fromFile(path);
                String version = properties.get(ACTIVE_VERSION);
                if (version != null && !version.isBlank()) {
                    if (installedVersions.contains(version)) {
                        activeVersionString = version;
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
        activeVersionString = highestVersionString;
        return highestVersionString;
    }

    public boolean isFileTimeSupported() {
        return !getHomePath().toUri().getScheme().equals("jar");
    }

    //
    // Private path-related methods
    //

    private Path getHomePath(String specifiedHome) {
        Pair<Path,String> zipPaths = splitZipPath(specifiedHome);
        if (zipPaths == null) {
            return Path.of(specifiedHome);
        } else {
            Path zipFile = zipPaths.getL();
            String entryPath = zipPaths.getR();

            URI uri = URI.create("jar:" + zipFile.toUri());
            try {
                FileSystem vfs = FileSystems.newFileSystem(uri, Map.of("create", "false"));
                return vfs.getPath(entryPath);
            } catch (IOException e) {
                return null;
            }
        }
    }

    private Path getSpecifiedHomePath() {
        String candidate = System.getenv("CASCARA_HOME");
        if (candidate != null) {
            Path path = getHomePath(candidate);
            if (path != null) {
                return path;
            }
        }
        candidate = System.getProperty("cascara.home");
        if (candidate != null) {
            Path path = getHomePath(candidate);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private static Pair<Path,String> splitZipPath(String path) {
        int bang = path.indexOf("!");
        if (bang == -1) {
            return null;
        }
        String zipFile = path.substring(0, bang);
        String entryPath = path.substring(bang + 1);

        if (zipFile.startsWith("jar:")) {
            zipFile = zipFile.substring(4);
        }

        return Pair.of(Path.of(zipFile), entryPath);
    }

    //
    // Private version-relate methods
    //

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

    private static SemVer toSemVer(String version) {
        if (version == null || version.isBlank()) {
            throw new UnexpectedNullValueException("version");
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
}