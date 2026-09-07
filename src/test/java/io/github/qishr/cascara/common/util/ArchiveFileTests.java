package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
