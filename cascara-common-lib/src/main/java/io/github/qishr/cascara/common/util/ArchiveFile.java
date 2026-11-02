package io.github.qishr.cascara.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface ArchiveFile<T> extends Closeable {
    T getEntry(String name);
    List<T> getEntries();
    List<T> getEntries(String entryName);
    List<T> getEntries(String entryName, boolean subDirContents);
    Stream<T> stream();
    int size();
    String getComment();
    InputStream getInputStream(T entry) throws IOException;
    InputStream getInputStream(String entryName) throws IOException;
    byte[] extractFile(String entryName) throws IOException;
    ArchiveFile<T> addDirectory(Path entryName) throws IOException;
    ArchiveFile<T> addDirectory(Path sourcePath, String entryName) throws IOException;
    ArchiveFile<T> addFile(Path sourcePath, String entryName) throws IOException;
    ArchiveFile<T> addFile(byte[] content, String entryName) throws IOException;
    ArchiveFile<T> addFile(String content, String entryName) throws IOException;
    ArchiveFile<T> remove(String entryName) throws IOException;
    void close() throws IOException;
}
