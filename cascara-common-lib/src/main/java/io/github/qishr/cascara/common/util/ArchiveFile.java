package io.github.qishr.cascara.common.util;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;

public interface ArchiveFile {
    InputStream getInputStream(ZipEntry entry) throws LocalizableIOException;
    InputStream getInputStream(String entryName) throws LocalizableIOException;
    byte[] extractFile(String entryName) throws LocalizableIOException;
    ZipEntry getEntry(String name);
    List<ZipEntry> listFiles();
    List<ZipEntry> listFiles(String entryName);
    ZipFile addDirectory(Path entryName) throws LocalizableIOException;
    ZipFile addDirectory(Path sourcePath, String entryName) throws LocalizableIOException;
    ZipFile addFile(Path sourcePath, String entryName) throws LocalizableIOException;
    ZipFile addFile(String content, String entryName) throws LocalizableIOException;
    ZipFile remove(String entryName);
}
