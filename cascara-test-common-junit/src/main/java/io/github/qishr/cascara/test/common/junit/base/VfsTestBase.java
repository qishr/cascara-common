package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.util.TestException;

@Isolated
public class VfsTestBase extends Base {
    private static final String VFS_HOME_DIR = "/.cascara";

    private static @TempDir Path vfsTempDir;
    private static FileSystem vfs;
    private static String physicalHome;
    private static URI vfsUri;

    protected static @TempDir Path homePath;
    private static Path sharedPath;
    private static Path modulePath;
    private static Path tempPath;

    {
        // init();
    }

    @BeforeAll
    static void init() {
        Cascara cascara;
        if (Cascara.isInitialized()) {
            cascara = Cascara.get();
            homePath = cascara.getHomePath();
            if (!isVirtual(homePath.getFileSystem())) {
                throw new TestException("TestBase.init() has been called before VfsTestBase.init()");
            }
        } else {
            Path zipFile = vfsTempDir.resolve("cascara-vfs.zip");
            physicalHome = zipFile.toString() + "!" + VFS_HOME_DIR;
            vfsUri = URI.create("jar:" + zipFile.toUri());
            try {
                vfs = FileSystems.newFileSystem(vfsUri, Map.of("create", "true"));
                homePath = vfs.getPath(VFS_HOME_DIR);
                Files.createDirectories(homePath);
                cascara = Cascara.get(homePath);
                Files.createDirectories(Cascara.get().getModulePath());
            } catch (IOException e) {
                throw new TestException(e, "Failed to initialize VFS: " + e.getLocalizedMessage());
            }
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

    // @BeforeEach
    // protected void setUp() {
    //     // This must come after VFS is set up
    //     super.setUp();
    // }

    // @AfterEach
    // protected void tearDown() throws IOException {
    //     super.tearDown();
    // }

    protected String getPhysicalHomePath() {
        return physicalHome;
    }

    protected FileSystem getVFS() {
        return vfs;
    }

    //

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
