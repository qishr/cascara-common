package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.util.TestException;

@Isolated
public class VfsTestBase extends TestBase {
    private static final String VFS_HOME_DIR = "/.cascara";

    private static @TempDir Path vfsTempDir;
    private static FileSystem vfs;
    private static String physicalHome;
    private static URI vfsUri;

    @BeforeEach
    protected void setUp() {
        if (Cascara.isInitialized()) {
            Cascara cascara = Cascara.get();
            homePath = cascara.getHomePath();
        } else {
            Path zipFile = vfsTempDir.resolve("cascara-vfs.zip");
            physicalHome = zipFile.toString() + "!" + VFS_HOME_DIR;
            vfsUri = URI.create("jar:" + zipFile.toUri());
            try {
                vfs = FileSystems.newFileSystem(vfsUri, Map.of("create", "true"));
                homePath = vfs.getPath(VFS_HOME_DIR);
                Files.createDirectories(homePath);
                Cascara.get(homePath);
                Files.createDirectories(Cascara.get().getModulePath());
            } catch (IOException e) {
                throw new TestException(e, "Failed to initialize VFS: " + e.getLocalizedMessage());
            }
        }

        // This must come after VFS is set up
        super.setUp();
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
    }

    protected String getPhysicalHomePath() {
        return physicalHome;
    }

    protected FileSystem getVFS() {
        return vfs;
    }
}
