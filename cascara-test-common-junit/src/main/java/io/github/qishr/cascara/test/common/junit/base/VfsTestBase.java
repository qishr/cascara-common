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

public class VfsTestBase extends TestBase {
    private static final String vfsHomeDir = "/.cascara";

    private SPL rootLayer;
    private FileSystem vFs;
    private String physicalHome;
    private URI vfsUri;
    public SPL spl;

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();

        Path zipFile = tempDir.resolve("cascara-vfs.zip");
        physicalHome = zipFile.toString() + "!" + vfsHomeDir;

        vfsUri = URI.create("jar:" + zipFile.toUri());
        vFs = FileSystems.newFileSystem(vfsUri, Map.of("create", "true"));
        Path vHome = vFs.getPath(vfsHomeDir);

        Files.createDirectories(vHome);
        Cascara.instance().setHomePath(vHome);
        Files.createDirectories(Cascara.instance().getModulePath());

        rootLayer = SPL.getRoot();
        spl = rootLayer.create("baseTestLayer");
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
        if (rootLayer != null && spl != null) {
            rootLayer.remove(spl.getName());
        }
        Cascara.instance().reset();
        if (vFs != null && vFs.isOpen()) {
            vFs.close();
        }
    }

    protected String getPhysicalHomePath() {
        return physicalHome;
    }

    protected FileSystem getVFS() {
        return vFs;
    }

    protected void syncVfs() {
        try {
            vFs.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            vFs = FileSystems.newFileSystem(vfsUri, Map.of());
            Cascara.instance().setHomePath(vFs.getPath(vfsHomeDir));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void displayVfs() {
        if (vFs != null && vFs.isOpen()) {
            try (Stream<Path> walk = Files.walk(vFs.getPath("/"))) {
                walk.filter(Files::isRegularFile).forEach(System.out::println);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
