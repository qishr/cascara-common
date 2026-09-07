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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.zip.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

public class ArchiveFile implements AutoCloseable {
    protected Path archivePath = null;
    // private FileSystem fileSystem;
    private boolean create;

    public static ArchiveFile load(Path archivePath) throws LocalizableIOException {
        if (!Files.exists(archivePath)) {
            throw new LocalizableIOException(FileDiagnosticCode.FILE_NOT_FOUND, archivePath);
        }
        return new ArchiveFile(archivePath, false);
    }

    public static ArchiveFile create(Path archivePath) throws LocalizableIOException {
        // TODO: If it exists, remove and re-create
        return new ArchiveFile(archivePath, true);
    }

    protected ArchiveFile(Path archivePath, boolean create) throws LocalizableIOException {
        if (!create) {
            // TODO: Exception if it doesn't exist
        }
        this.archivePath = archivePath;
        this.create = create;
    }

    private FileSystem getFileSystem() throws LocalizableIOException {
        Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
            create = false;
        }
        URI uri = URI.create("jar:" + archivePath.toUri());
        try {
            return FileSystems.newFileSystem(uri, env);
        } catch (IOException e) {
            if (create) {
                throw new LocalizableIOException(e, FileDiagnosticCode.WRITE_ERROR, archivePath);
            } else {
                throw new LocalizableIOException(e, FileDiagnosticCode.READ_ERROR, archivePath);
            }
        }
    }

    // public void flush() {
    //     if (fileSystem != null) {
    //         fileSystem.
    //     }
    // }

    @Override
    public void close() throws Exception {
        // if (fileSystem != null) {
        //     fileSystem.close();
        // }
        // fileSystem = null;
    }

    //
    //
    //

    public InputStream getInputStream(String filePath) {
        byte[] byteArray = this.extractFile(filePath);
        return new ByteArrayInputStream(byteArray);
    }

    public byte[] extractFile(String filePath) {
        return extractFile(archivePath, filePath);
    }

    protected static byte[] extractFile(Path archivePath, String filePath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals(filePath)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
        return new byte[0];
    }

    // TODO: FileInfo should be EntryInfo

    public List<EntryInfo> listFiles() throws LocalizableIOException {
        return listFiles(archivePath, null);
    }

    public List<EntryInfo> listFiles(String dirPath) throws LocalizableIOException {
        return listFiles(archivePath, dirPath);
    }

    public void addDirectory(Path dirPath) throws LocalizableIOException {
        addDirectory(dirPath, "");
    }

    public void addDirectory(Path sourcePath, String entryName) throws LocalizableIOException {
        List<Path> directoryListing;
        try {
            directoryListing = Files.walk(sourcePath)
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            throw new LocalizableIOException(e, FileDiagnosticCode.READ_ERROR, sourcePath);
        }

        String entryPrefix = entryName == null || entryName.isEmpty()
            ? sourcePath.getFileName().toString()
            : entryName.endsWith("/")
                ? entryName.substring(0, entryName.length() - 1)
                : entryName;

        try (FileSystem fileSystem = getFileSystem()) {
            for (Path file : directoryListing) {
                Path relative = sourcePath.relativize(file);
                String internalFilePath = entryPrefix + "/" + relative.toString().toString()
                    .replace(File.separatorChar, '/');
                Path entryPath = fileSystem.getPath(internalFilePath);
                addFileInternal(file, entryPath);
            }
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }
    }

    public void addFile(Path sourcePath, String entryName) throws LocalizableIOException{
        System.out.println("addFile: sourcePath=" + sourcePath + ", entryName=" + entryName);
        try (FileSystem fileSystem = getFileSystem()) {
            addFileInternal(sourcePath, fileSystem.getPath(entryName));
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }
    }

    public void addFile(String content, String entryName) throws LocalizableIOException {
        System.out.println("addFile: " + content + "\nentryName=" + entryName);
        try (FileSystem fileSystem = getFileSystem()) {
            addFileInternal(content, fileSystem.getPath(entryName));
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }
    }

    //
    //
    //

    protected static List<EntryInfo> listFiles(Path archivePath, String dirPath) throws LocalizableIOException {
        if (dirPath != null && !dirPath.isEmpty() && !dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        List<EntryInfo> fileInfoList = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (dirPath == null) {
                    EntryInfo fileInfo = new EntryInfo(entry.getName());
                    fileInfoList.add(fileInfo);
                } else if (entry.getName().startsWith(dirPath) && !entry.getName().equals(dirPath)) {
                    EntryInfo fileInfo = new EntryInfo(entry.getName().substring(dirPath.length()));
                    fileInfoList.add(fileInfo);
                }
            }
        } catch (FileNotFoundException e) {
            throw new LocalizableIOException(e, FileDiagnosticCode.FILE_NOT_FOUND, archivePath);
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }

        return fileInfoList;
    }

    private void addFileInternal(Path sourcePath, Path entryPath) throws LocalizableIOException{
        try (FileInputStream in = new FileInputStream(sourcePath.toFile())) {
            byte[] buf = new byte[1024];
            int len;
            ensureParentExists(entryPath);
            try (OutputStream os = Files.newOutputStream(entryPath, StandardOpenOption.CREATE)) {
                while ((len = in.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                in.close();
            }
        } catch (FileNotFoundException e) {
            throw new LocalizableIOException(e, FileDiagnosticCode.FILE_NOT_FOUND, sourcePath);
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }
    }

    private void addFileInternal(String content, Path entryPath) throws LocalizableIOException {
        byte[] buf = content.getBytes();
        ensureParentExists(entryPath);
        try (OutputStream os = Files.newOutputStream(entryPath, StandardOpenOption.CREATE)) {
            os.write(buf, 0, buf.length);
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
        }
    }

    private void ensureParentExists(Path entryPath) throws LocalizableIOException {
        if (entryPath.getParent() != null) {
            try {
                Files.createDirectories(entryPath.getParent());
            } catch (IOException e) {
                throw new LocalizableIOException(e, GenericDiagnosticCode.IO_ERROR, e.getMessage());
            }
        }
    }

    public static class EntryInfo {
        private String path = "";

        public EntryInfo(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
