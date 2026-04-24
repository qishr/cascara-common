package io.github.qishr.cascara.common.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.Properties;

public class JarFile extends ArchiveFile {
    private Properties manifestProperties = new Properties();
    private Properties mavenProperties = new Properties();
    private List<String> packageList = new ArrayList<>();
    private String moduleName = "";

    public static JarFile load(Path vsixPath) throws IOException {
        String jarManifest = new String(extractFile(vsixPath, "META-INF/MANIFEST.MF"));
        JarFile jar = new JarFile(vsixPath);
        jar.manifestProperties = JarManifest.parse(jarManifest);
        jar.extractMavenInfo();
        jar.determineModuleName();
        jar.discoverPackages();
        return jar;
    }

    private JarFile(Path vsixPath) throws IOException {
        super(vsixPath);
    }

    public Path getPath() {
        return archivePath;
    }

    public Properties getManifestProperties() {
        return manifestProperties;
    }

    public Properties getMavenProperties() {
        return mavenProperties;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<String> getPackages() {
        return packageList;
    }

    private void extractMavenInfo() {
        String mavenPropertiesPath = getPomPropertiesPath();
        String mavenPropertiesString = new String(extractFile(mavenPropertiesPath));
        parseProperties(mavenPropertiesString, mavenProperties);
    }

    private String getPomPropertiesPath() {
        final String mavenDirectory = "META-INF/maven/";
        try {
            List<FileInfo> files = listFiles(mavenDirectory);
            for (FileInfo info : files) {
                if (info.getPath().endsWith("pom.properties")) {
                    return mavenDirectory + info.getPath();
                }
            }
        } catch (IOException _) {
            // Ignore for now
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

    private void determineModuleName() {
        String automaticModuleName = manifestProperties.getString("Automatic-Module-Name");
        if (automaticModuleName != null && !automaticModuleName.isBlank()) {
            moduleName = automaticModuleName;
            return;
        }
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
        moduleName = sb.toString();
    }

    private void discoverPackages() {
        try {
            List<FileInfo> files = listFiles("");
            for (FileInfo info : files) {
                String packageName = JarFile.getPackageName(info);
                if (packageName != null && !packageList.contains(packageName)) {
                    packageList.add(packageName);
                }
            }
        } catch (IOException _) {
            // Ignore for now
        }
    }

    public static String getClassName(FileInfo fileInfo) {
        String filePath = fileInfo.getPath();
        if (filePath.endsWith(".class")) {
            int slash = filePath.lastIndexOf("/");
            return filePath.substring(slash + 1, filePath.length() - 6);
        }
        return null;
    }

    public static String getPackageName(FileInfo fileInfo) {
        String filePath = fileInfo.getPath();
        if (filePath.endsWith(".class")) {
            int slash = filePath.lastIndexOf("/");
            if (slash > -1) {
                String packagePath = filePath.substring(0, slash);
                return packagePath.replace("/", ".");
            }
        }
        return null;
    }
}
