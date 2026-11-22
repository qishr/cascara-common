package io.github.qishr.cascara.test.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class TestCascara implements Closeable {

    private Path homePath;
    private Path sharedPath;
    private Path moulePath;

    public TestCascara() {
        // Create directories
        try {
            homePath = Files.createTempDirectory("cascara-test-");
            sharedPath = homePath.resolve("shared");
            Files.createDirectories(sharedPath);
            moulePath = homePath.resolve("modulepath");
            Files.createDirectories(moulePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path getHomePath() {
        return homePath;
    }

    public Path getSharedPath() {
        return sharedPath;
    }

    public Path getModulePath() {
        return moulePath;
    }

    @Override
    public void close() throws IOException {
        try (Stream<Path> walk = Files.walk(homePath)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(t -> {
                    try {
                        Files.delete(t);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
    }
}
