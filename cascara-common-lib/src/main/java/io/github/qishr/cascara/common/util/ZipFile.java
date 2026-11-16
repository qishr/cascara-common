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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.i18n.exception.LocalizableIOException;

public class ZipFile implements ArchiveFile<ZipEntry> {
    protected Path archivePath = null;
    private boolean create;

    public static ZipFile open(Path archivePath) throws IOException {
        return new ZipFile(archivePath, false);
    }

    public static ZipFile create(Path archivePath) throws IOException {
        return new ZipFile(archivePath, true);
    }

    protected ZipFile(Path archivePath, boolean create) throws IOException {
        // If it exists and it's not a directory, remove and re-create
        if (create) {
            if (Files.exists(archivePath)) {
                if (Files.isDirectory(archivePath)) {
                    throw new LocalizableIOException(FileMessage.IS_DIRECTORY, archivePath);
                } else {
                    try {
                        Files.delete(archivePath);
                    } catch (IOException e) {
                        throw new LocalizableIOException(e, FileMessage.DELETE_ERROR, archivePath);
                    }
                }
            }
        } else {
            if (!Files.exists(archivePath)) {
                throw new LocalizableIOException(FileMessage.FILE_NOT_FOUND, archivePath);
            }
        }
        this.archivePath = archivePath;
        this.create = create;
    }

    public Path getPath() {
        return archivePath;
    }

    // public void flush() {
    //     if (fileSystem != null) {
    //         fileSystem.
    //     }
    // }

    // TODO: Implement
    @Override
    public void close() throws IOException {
        // if (fileSystem != null) {
        //     fileSystem.close();
        // }
        // fileSystem = null;
    }

    //
    //
    //

    @Override
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        byte[] byteArray = extractFileInternal(entry.getName());
        return new ByteArrayInputStream(byteArray);
    }

    @Override
    public InputStream getInputStream(String entryName) throws IOException {
        byte[] byteArray = extractFileInternal(entryName);
        return new ByteArrayInputStream(byteArray);
    }

    @Override
    public byte[] extractFile(String entryName) throws IOException {
        return extractFile(archivePath, entryName);
    }

    @Override // TODO: Entry details
    public ZipEntry getEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        return entry;
    }

    @Override
    public List<ZipEntry> getEntries() {
        try {
            return listEntries(archivePath, null, true);
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public List<ZipEntry> getEntries(String entryName) {
        return getEntries(entryName, true);
    }

    @Override
    public List<ZipEntry> getEntries(String entryName, boolean subDirContents) {
        try {
            return listEntries(archivePath, entryName, subDirContents);
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public Stream<ZipEntry> stream() {
        // TODO Implement this
        throw new UnimplementedMethodException();
    }

    @Override
    public int size() {
        int count = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archivePath))) {
            while (zipInputStream.getNextEntry() != null) {
                count++;
            }
        } catch (IOException e) {}
        return count;
    }

    @Override
    public String getComment() {
        // TODO Implement this
        throw new UnimplementedMethodException();
    }

    @Override
    public ZipFile addDirectory(Path entryName) throws IOException {
        addDirectory(entryName, "");
        return this;
    }

    @Override
    public ZipFile addDirectory(Path sourcePath, String entryName) throws IOException {
        // TODO: This pattern needs a less boilerplatey way of implementing
        List<IOException> exceptions = new ArrayList<>();
        walk(sourcePath, entryName, exceptions, (source, entryPath) -> addFileInternalNoException(source, entryPath, exceptions));
        if (!exceptions.isEmpty()) {
            throw exceptions.getFirst();
        }
        return this;
    }

    @Override
    public ZipFile addFile(Path sourcePath, String entryName) throws IOException {
        // System.out.println("addFile: sourcePath=" + sourcePath + ", entryName=" + entryName);
        try (FileSystem fileSystem = getFileSystem()) {
            addFileInternal(sourcePath, fileSystem.getPath(entryName));
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }
        return this;
    }

    @Override
    public ZipFile addFile(String content, String entryName) throws IOException {
        return addFile(content.getBytes(), entryName);
    }

    @Override
    public ZipFile addFile(byte[] content, String entryName) throws IOException {
        // System.out.println("addFile: " + content + "\nentryName=" + entryName);
        try (FileSystem fileSystem = getFileSystem()) {
            addFileInternal(content, fileSystem.getPath(entryName));
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }
        return this;
    }

    @Override
    public ZipFile remove(String entryName) throws IOException {
        try (FileSystem fileSystem = getFileSystem()) {
            removeInternal(fileSystem.getPath(entryName));
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }
        return this;
    }

    //
    //
    //

    protected void walk(Path sourcePath, String entryName, List<IOException> exceptions, BiConsumer<Path,Path> run) throws IOException {
        List<Path> directoryListing;
        try {
            directoryListing = Files.walk(sourcePath)
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            throw new LocalizableIOException(e, FileMessage.READ_ERROR, sourcePath);
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
                run.accept(file, entryPath);
            }
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }
    }

    protected static byte[] extractFile(Path archivePath, String entryName) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (IOException e) {
            throw new LocalizableIOException(e, FileMessage.READ_ERROR, entryName);
        }
        return new byte[0];
    }

    protected static List<ZipEntry> listEntries(Path archivePath, String entryName, boolean subDirContents) throws IOException {
        if (entryName != null && !entryName.isEmpty() && !entryName.endsWith("/")) {
            entryName = entryName + "/";
        }
        List<ZipEntry> entryList = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (entryName == null || entryName.isEmpty()) {
                    entryList.add(zipEntry);
                } else {
                    String zipEntryName = zipEntry.getName();
                    if (zipEntryName.startsWith(entryName)) {

                        if (zipEntryName.equals(entryName)) {
                            // Skip the entry for the specified directory itself
                            continue;
                        }

                        if (!subDirContents) {
                            // Skip if this entry in a subdirectory of specified directory
                            String relativeName = zipEntryName.substring(entryName.length());
                            int slash = relativeName.indexOf('/');
                            if (slash > -1 && slash < relativeName.length() - 1) {
                                continue;
                            }
                        }

                        entryList.add(zipEntry);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new LocalizableIOException(e, FileMessage.FILE_NOT_FOUND, archivePath);
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }

        return entryList;
    }

    //
    //
    //

    private FileSystem getFileSystem() throws IOException {
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
                throw new LocalizableIOException(e, FileMessage.WRITE_ERROR, archivePath);
            } else {
                throw new LocalizableIOException(e, FileMessage.READ_ERROR, archivePath);
            }
        }
    }

    private byte[] extractFileInternal(String entryName) throws IOException {
        return extractFile(archivePath, entryName);
    }

    private void addFileInternal(Path sourcePath, Path entryName) throws IOException{
        List<IOException> exceptions = new ArrayList<>();
        addFileInternalNoException(sourcePath, entryName, exceptions);
        if (!exceptions.isEmpty()) {
            throw exceptions.getFirst();
        }
    }

    private void addFileInternalNoException(Path sourcePath, Path entryPath, List<IOException> exceptions) {
        try (InputStream in = Files.newInputStream(sourcePath)) {
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
            exceptions.add(new LocalizableIOException(e, FileMessage.FILE_NOT_FOUND, sourcePath));
        } catch (IOException e) {
            exceptions.add(new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage()));
        }
    }

    private void addFileInternal(byte[] content, Path entryPath) throws IOException {
        ensureParentExists(entryPath);
        try (OutputStream os = Files.newOutputStream(entryPath, StandardOpenOption.CREATE)) {
            os.write(content, 0, content.length);
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }
    }

    private void removeInternal(Path entryPath) throws IOException {
        try {
            if (Files.isDirectory(entryPath)) {
                Files.walkFileTree(entryPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(entryPath);
            }
        } catch (IOException e) {
            throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
        }
    }

    private void ensureParentExists(Path entryPath) throws IOException {
        if (entryPath.getParent() != null) {
            try {
                Files.createDirectories(entryPath.getParent());
            } catch (IOException e) {
                throw new LocalizableIOException(e, GenericMessage.IO_ERROR, e.getMessage());
            }
        }
    }
}
