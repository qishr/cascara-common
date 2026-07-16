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
