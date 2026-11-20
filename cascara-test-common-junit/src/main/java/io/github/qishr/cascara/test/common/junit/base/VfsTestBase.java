package io.github.qishr.cascara.test.common.junit.base;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Stream;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Config;
import io.github.qishr.cascara.test.common.util.TestException;

public class VfsTestBase extends TestBase {
    private static final String vfsHomeDir = "/.cascara";

    private SPL rootLayer;
    private FileSystem vfs;
    private String physicalHome;
    private URI vfsUri;
    public SPL spl;

    @BeforeEach
    protected void setUp() {
        Path zipFile = tempDir.resolve("cascara-vfs.zip");
        physicalHome = zipFile.toString() + "!" + vfsHomeDir;

        vfsUri = URI.create("jar:" + zipFile.toUri());

        Config.resetAll();

        try {
            vfs = FileSystems.newFileSystem(vfsUri, Map.of("create", "true"));
            Path vHome = vfs.getPath(vfsHomeDir);
            Files.createDirectories(vHome);
            Cascara.get(vHome);
            Files.createDirectories(Cascara.get().getModulePath());
        } catch (IOException e) {
            throw new TestException(e, "Failed to initialize VFS: " + e.getLocalizedMessage());
        }

        // This must come after VFS is set up
        super.setUp();
        rootLayer = SPL.getRoot();
        spl = rootLayer.create("baseTestLayer");
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
        if (rootLayer != null && spl != null) {
            rootLayer.remove(spl.getName());
        }
        if (vfs != null && vfs.isOpen()) {
            vfs.close();
        }
        Config.resetAll();
    }

    protected String getPhysicalHomePath() {
        return physicalHome;
    }

    protected FileSystem getVFS() {
        return vfs;
    }

    protected void syncVfs() {
        try {
            vfs.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            vfs = FileSystems.newFileSystem(vfsUri, Map.of());


            // Cascara.get().setHomePath(vFs.getPath(vfsHomeDir));


            // Cascara.get().reset();
            Config.resetAll();
            Cascara.get(vfs.getPath(vfsHomeDir));


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void displayVfs() {
        if (vfs != null && vfs.isOpen()) {
            try (Stream<Path> walk = Files.walk(vfs.getPath("/"))) {
                walk.filter(Files::isRegularFile).forEach(System.out::println);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
