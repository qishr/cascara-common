package integration.test.jar;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.test.common.junit.base.TestBase;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;

public abstract class JarTestBase extends TestBase {

    protected static TestCertificateAuthority rootCA;

    @BeforeAll
    static void init() throws Exception {
        rootCA = TestCertificateAuthority.create("Root CA");
    }

    @AfterAll
    static void end() {
        GlobalReporter.globalInstance()
            .setLevel(Level.INFO);
    }

    protected static void tamperZipEntry(Path zipPath, String entryName, java.util.function.UnaryOperator<byte[]> modifier) throws Exception {
        Map<String, String> env = Map.of("create", "false");
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), env)) {
            Path target = fs.getPath(entryName);
            byte[] original = Files.readAllBytes(target);
            byte[] modified = modifier.apply(original);
            Files.write(target, modified);
        }
    }

    protected static void addZipEntry(Path zipPath, String entryName, byte[] content) throws Exception {
        Map<String, String> env = Map.of("create", "false");
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), env)) {
            Path target = fs.getPath(entryName);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, content);
        }
    }
}
