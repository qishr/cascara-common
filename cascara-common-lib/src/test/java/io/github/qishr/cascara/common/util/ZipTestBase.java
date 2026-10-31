package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;

import java.nio.file.Files;

public abstract class ZipTestBase {
    private static final Reporter REPORTER = Reporter.forClass(ZipTestBase.class);

    @TempDir
    private Path tempDir;

    protected Path sourceDir;

    protected Path getTempDir() {
        return tempDir;
    }

    protected Path createTempFile() {

        try {
            Path tempFile = Files.createTempFile(tempDir, "zip-file-tests-", ".tmp");

            return tempFile;
        } catch (IOException e) {
            REPORTER.error(e);
            return null;
        }
    }

    protected Path getSourceDir() {
        return sourceDir;
    }

    @BeforeEach
    protected void setUp() throws IOException {
        sourceDir = tempDir.resolve("source");
        Path parentDir = sourceDir.resolve("parent");
        Path nestedDir1 = parentDir.resolve("nested1");
        Path nestedDir2 = parentDir.resolve("nested2");
        Files.createDirectories(nestedDir1);
        Files.createDirectories(nestedDir2);

        Files.writeString(parentDir.resolve("file0"), "file0");
        Files.writeString(nestedDir1.resolve("file1"), "file1");
        Files.writeString(nestedDir2.resolve("file2"), "file2");
    }

    @AfterEach
    protected void tearDown() throws IOException {

    }

    protected void assertContainsEntry(String entryName, List<ZipEntry> entries, Path archivePath) throws IOException {
        for (ZipEntry entry : entries) {
            if (entry.getName().equals(entryName)) {
                return;
            }
        }
        DebugUtils.showContents(archivePath);
        showEntries(entries);
        assertTrue(false, "Entry missing but should exist: " + entryName);
    }

    protected void assertDoesNotContainEntry(String entryName, List<ZipEntry> entries, Path archivePath) throws IOException {
        for (ZipEntry entry : entries) {
            if (entry.getName().equals(entryName)) {
                DebugUtils.showContents(archivePath);
                showEntries(entries);
                assertTrue(false, "Entry exists but should not: " + entryName);
            }
        }
    }

    public static void showEntries(List<ZipEntry> entries) {
        System.out.println("Entries:");
        for (ZipEntry entry : entries) {
            System.out.println(entry.getName());
        }
        System.out.println();
    }
}
