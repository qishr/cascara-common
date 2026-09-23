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

package io.github.qishr.cascara.common.test.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.util.ArchiveFile;
// import org.opentest4j.FileInfo;
import io.github.qishr.cascara.common.util.ArchiveFile.EntryInfo;

class ArchiveFileTests {

    @TempDir
    Path tempDir;

    @Test
    void addDirectoryAddsFilesToArchive() throws Exception {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir.resolve("nested"));

        Files.writeString(
                sourceDir.resolve("one.txt"),
                "first file",
                StandardCharsets.UTF_8
        );

        Files.writeString(
                sourceDir.resolve("nested/two.txt"),
                "second file",
                StandardCharsets.UTF_8
        );

        Path archivePath = tempDir.resolve("archive.zip");

        ArchiveFile archive = ArchiveFile.create(archivePath);
        archive.addDirectory(sourceDir);

        Files.writeString(
            sourceDir.resolve("three.txt"),
            "third file",
            StandardCharsets.UTF_8
        );

        archive.addFile(sourceDir.resolve("three.txt"), "three.txt");
        archive.addFile(sourceDir.resolve("three.txt"), "folder/four.txt");
        // archive.addFile(Path.of("/tmp/hello"), "three.txt");

        assertTrue(Files.exists(archivePath));

        List<EntryInfo> files = archive.listFiles();

        assertContainsFile("folder/four.txt", files, archivePath);
        assertContainsFile("three.txt", files, archivePath);
        assertContainsFile("source/nested/two.txt", files, archivePath);
        assertContainsFile("source/one.txt", files, archivePath);
    }

    private void assertContainsFile(String fileName, List<EntryInfo> files, Path pkgPath) throws IOException {
        for (EntryInfo info : files) {
            if (info.getPath().equals(fileName)) {
                return;
            }
        }
        showContents(pkgPath);
        assertTrue(false, "File missing: " + fileName);
    }

    private void showContents(Path zipFile) throws IOException {
        System.out.println("Archive contents:");
        try (ZipInputStream zip = new ZipInputStream(
                Files.newInputStream(zipFile))) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                System.out.println(entry.getName());
            }
        }
    }

}
