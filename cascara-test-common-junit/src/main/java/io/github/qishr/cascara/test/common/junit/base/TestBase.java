package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.util.TestException;

public abstract class TestBase extends Base {

    @TempDir
    private static Path tempDir;

    protected static @TempDir Path homePath;
    private static Path sharedPath;
    private static Path modulePath;
    private static Path tempPath;

    @BeforeAll
    static void init() {
        Cascara cascara;
        if (Cascara.isInitialized()) {
            cascara = Cascara.get();
            homePath = cascara.getHomePath();
            if (isVirtual(homePath.getFileSystem())) {
                throw new TestException("VfsTestBase.init() has been called before TestBase.init()");
            }
        } else {
            homePath = tempDir.resolve(".cascara");
            cascara = Cascara.get(homePath);
        }

        sharedPath = cascara.getSharedPath();
        modulePath = cascara.getModulePath();
        tempPath = sharedPath.resolve("tmp");

        try {
            if (!Files.exists(sharedPath)) { Files.createDirectories(sharedPath); }
            if (!Files.exists(modulePath)) { Files.createDirectories(modulePath); }
            if (!Files.exists(tempPath)) { Files.createDirectories(tempPath); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Path getHomePath() {
        return homePath;
    }

    protected Path getSharedPath() {
        return sharedPath;
    }

    protected Path getModulePath() {
        return modulePath;
    }

    protected Path getTempPath() {
        return tempPath;
    }

    protected void displayHomeDir() {
        displayDir(homePath);
    }
}
