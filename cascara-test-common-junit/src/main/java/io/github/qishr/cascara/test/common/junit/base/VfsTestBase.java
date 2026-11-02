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

    private SPL rootLayer;
    private FileSystem vFs;
    private String physicalHome;
    private URI vfsUri;


    public SPL spl;

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
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        // Points ZipFS to a writable temp file path provided by JUnit
        Path zipFile = tempDir.resolve("cascara-vfs.zip");
        vfsUri = URI.create("jar:" + zipFile.toUri());

        vFs = FileSystems.newFileSystem(vfsUri, Map.of("create", "true"));
        String vfsHomeDir = "/.cascara";
        Path vHome = vFs.getPath(vfsHomeDir);
        Files.createDirectories(vHome);

        Cascara.setHomePath(vHome);

        physicalHome = zipFile.toString() + "!" + vfsHomeDir;

        Files.createDirectories(Cascara.getModulePath());

        rootLayer = SPL.getRoot();

        spl = rootLayer.create("baseTestLayer");
    }

    @AfterEach
    protected void tearDown() throws IOException {
        super.tearDown();
        if (rootLayer != null && spl != null) {
            rootLayer.remove(spl.getName());
        }
        if (vFs != null && vFs.isOpen()) {
            vFs.close();
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

    // void copyToVfsAndRegister(String mavenName, SPL layer) throws IOException {
    //     Path testJarPath = ArtifactResolver.resolveJarPath(mavenName);
    //     Path vfsTestJarPath = Cascara.getModulePath().resolve(testJarPath.getFileName().toString());
    //     Files.copy(testJarPath, vfsTestJarPath);
    //     layer.registerJar(vfsTestJarPath);
    // }

}
