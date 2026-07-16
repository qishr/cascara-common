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
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;

public class JarFile extends ArchiveFile {
    private Properties manifestProperties = new Properties();
    private Properties mavenProperties;
    private Set<String> packageNames = null;
    private Set<String> classNames = null;
    private String moduleName = null;

    public static JarFile load(Path jarPath) throws LocalizableIOException {
        return new JarFile(jarPath);
    }

    private JarFile(Path jarPath) throws LocalizableIOException {
        super(jarPath);
        String jarManifest = new String(extractFile("META-INF/MANIFEST.MF"));
        manifestProperties = JarManifest.parse(jarManifest);
    }

    public Path getPath() {
        return archivePath;
    }

    public Properties getManifestProperties() {
        return manifestProperties;
    }

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

    public String getModuleName() {
        if (moduleName == null) {
            moduleName = getJpmsModuleName();
        }
        if (moduleName == null) {
            String automaticModuleName = manifestProperties.getString("Automatic-Module-Name");
            if (automaticModuleName != null && !automaticModuleName.isBlank()) {
                moduleName = automaticModuleName;
            }
        }
        if (moduleName == null) {
            determineModuleName();
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

    private void extractMavenInfo() {
        mavenProperties = new Properties();
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
        } catch (LocalizableIOException e) {
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

    private String getJpmsModuleName() {
        InputStream is = getInputStream("module-info.class");
        ModuleDescriptor descriptor;
        try {
            descriptor = ModuleDescriptor.read(is);
            return descriptor.name();
        } catch (IOException | InvalidModuleDescriptorException e) {
            return null;
        }
    }

    private void discoverClasses() {
        List<FileInfo> allFiles;
        try {
            allFiles = listFiles();
        } catch (LocalizableIOException e) {
            return;
        }
        classNames = new HashSet<>();
        for (FileInfo fileInfo : allFiles) {
            String entryName = fileInfo.getPath();
            // if (fileInfo.getPath().endsWith(".class")) {
            if (entryName.endsWith(".class")) {
                // if (checkClassFile(jarFile, entry)) {
                // }

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
        try {
            List<FileInfo> files = listFiles("");
            for (FileInfo info : files) {
                String packageName = JarFile.getPackageName(info);
                if (packageName != null && !packageNames.contains(packageName)) {
                    packageNames.add(packageName);
                }
            }
        } catch (LocalizableIOException e) {
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

    // private static boolean checkClassFile(JarFile jarFile, JarEntry jarEntry) {
    //     String arch = System.getProperty("os.arch");
    //     System.out.println("JVM Architecture: " + arch);
    //     try {
    //         InputStream is = jarFile.getInputStream(jarEntry);
    //         // System.out.println();
    //         return true;
    //     } catch (Exception e) {
    //         System.out.println(e.getMessage());
    //         e.printStackTrace();
    //     }
    //     return false;
    // }
}
