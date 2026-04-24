package io.github.qishr.cascara.common.io;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

public class ArchiveFile {
    protected Path archivePath = null;

    public static ArchiveFile load(Path archivePath) throws IOException {
        return new ArchiveFile(archivePath);
    }

    protected ArchiveFile(Path archivePath) {
        this.archivePath = archivePath;
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

    public List<FileInfo> listFiles(String dirPath) throws IOException {
        return listFiles(archivePath, dirPath);
    }

    protected static List<FileInfo> listFiles(Path archivePath, String dirPath) throws IOException {
        if (!dirPath.isEmpty() && !dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        List<FileInfo> fileInfoList = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().startsWith(dirPath) && !entry.getName().equals(dirPath)) {
                    FileInfo fileInfo = new FileInfo(entry.getName().substring(dirPath.length()));
                    fileInfoList.add(fileInfo);
                }
            }
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
