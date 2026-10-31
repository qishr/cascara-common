package io.github.qishr.cascara.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DebugUtils {
    private DebugUtils() {}

    public static void showContents(Path zipFile) throws IOException {
        System.out.println("ZipInputStream:");
        try (ZipInputStream zip = new ZipInputStream(
            Files.newInputStream(zipFile))) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                System.out.println(entry.getName());
            }
        }
        System.out.println();
    }

}
