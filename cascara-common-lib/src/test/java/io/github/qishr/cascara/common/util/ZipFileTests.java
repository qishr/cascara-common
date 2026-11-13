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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;

class ZipFileTests extends ZipTestBase {

    @Test
    void test_subDirectoryListing_withoutTrailingSlash() throws Exception {
        try (ZipFile zipFile = ZipFile.create(createTempFile())) {
            zipFile.addDirectory(sourceDir);
            assertEquals(7, zipFile.size());
            List<ZipEntry> files = zipFile.getEntries("source");
            assertDoesNotContainEntry("source/", files, zipFile.getPath());
            assertContainsEntry("source/parent/", files, zipFile.getPath());
            assertContainsEntry("source/parent/file0", files, zipFile.getPath());
        }
    }

    @Test
    void test_subDirectoryListing_withTrailingSlash() throws Exception {
        ZipFile zipFile = ZipFile.create(createTempFile());
        zipFile.addDirectory(sourceDir);
        List<ZipEntry> files = zipFile.getEntries("source/");
        assertDoesNotContainEntry("source/", files, zipFile.getPath());
        assertContainsEntry("source/parent/", files, zipFile.getPath());
        assertContainsEntry("source/parent/file0", files, zipFile.getPath());
    }

    @Test
    void test_directoryListing_withoutSubDirectories() throws Exception {
        ZipFile zipFile = ZipFile.create(createTempFile());
        zipFile.addDirectory(sourceDir);
        List<ZipEntry> files = zipFile.getEntries("source/parent/", false);
        assertDoesNotContainEntry("source/", files, zipFile.getPath());
        assertDoesNotContainEntry("source/parent/", files, zipFile.getPath());
        assertContainsEntry("source/parent/file0", files, zipFile.getPath());
        assertContainsEntry("source/parent/nested1/", files, zipFile.getPath());
        assertDoesNotContainEntry("source/parent/nested1/file1", files, zipFile.getPath());
    }

    @Test
    void test_addDirectory_default() throws Exception {
        ZipFile zipFile = ZipFile.create(createTempFile());
        zipFile.addDirectory(sourceDir);
        List<ZipEntry> files = zipFile.getEntries();
        assertTrue(Files.exists(zipFile.getPath()));
        assertContainsEntry("source/parent/file0", files, zipFile.getPath());
        assertContainsEntry("source/parent/nested1/file1", files, zipFile.getPath());
        assertContainsEntry("source/parent/nested2/file2", files, zipFile.getPath());
    }

    @Test
    void test_addDirectory_toDirectory() throws Exception {
        ZipFile zipFile = ZipFile.create(createTempFile());
        zipFile.addDirectory(sourceDir, "directory");
        List<ZipEntry> files = zipFile.getEntries();
        assertContainsEntry("directory/parent/file0", files, zipFile.getPath());
        assertContainsEntry("directory/parent/nested1/file1", files, zipFile.getPath());
        assertContainsEntry("directory/parent/nested2/file2", files, zipFile.getPath());
    }

    @Test
    void test_addFiles() throws Exception {
        ZipFile zipFile = ZipFile.create(createTempFile());
        zipFile.addFile(sourceDir.resolve("parent/file0"), "three.txt");
        zipFile.addFile(sourceDir.resolve("parent/file0"), "folder/four.txt");
        List<ZipEntry> files = zipFile.getEntries();
        assertTrue(Files.exists(zipFile.getPath()));
        assertContainsEntry("folder/four.txt", files, zipFile.getPath());
        assertContainsEntry("three.txt", files, zipFile.getPath());
    }

    @Test
    void test_addFile_reopen() throws Exception {
        Path path = createTempFile();
        try (ZipFile zipFile = ZipFile.create(path)) {
            zipFile.addFile(sourceDir.resolve("parent/file0"), "three.txt");
            List<ZipEntry> files = zipFile.getEntries();
            assertContainsEntry("three.txt", files, zipFile.getPath());
        }
        try (ZipFile zipFile = ZipFile.open(path)) {
            List<ZipEntry> files = zipFile.getEntries();
            assertContainsEntry("three.txt", files, zipFile.getPath());
        }
    }
}
