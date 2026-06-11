package io.github.qishr.cascara.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

public class ArchiveFile {
    protected Path archivePath = null;

    public static ArchiveFile load(Path archivePath) throws LocalizableIOException {
        return new ArchiveFile(archivePath);
    }

    protected ArchiveFile(Path archivePath) {
        this.archivePath = archivePath;
    }

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

    public List<FileInfo> listFiles() throws LocalizableIOException {
        return listFiles(archivePath, null);
    }

    public List<FileInfo> listFiles(String dirPath) throws LocalizableIOException {
        return listFiles(archivePath, dirPath);
    }

    protected static List<FileInfo> listFiles(Path archivePath, String dirPath) throws LocalizableIOException {
        if (dirPath != null && !dirPath.isEmpty() && !dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        List<FileInfo> fileInfoList = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (dirPath == null) {
                    FileInfo fileInfo = new FileInfo(entry.getName());
                    fileInfoList.add(fileInfo);
                } else if (entry.getName().startsWith(dirPath) && !entry.getName().equals(dirPath)) {
                    FileInfo fileInfo = new FileInfo(entry.getName().substring(dirPath.length()));
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

    public static class FileInfo {
        private String path = "";

        public FileInfo(String path) {
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
