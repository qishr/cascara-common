package io.github.qishr.cascara.common.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;
import io.github.qishr.cascara.common.lang.type.ScalarDescriptor;
import io.github.qishr.cascara.common.lang.util.SourceBuffer;
import io.github.qishr.cascara.common.util.Cascara;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class SplVfsTests {

    private FileSystem vFs;
    private Path vHome;

    @BeforeEach
    void setUp(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
        // Create an isolated zip filesystem backed by a temporary file
        Path zipFile = tempDir.resolve("cascara-home-vfs.zip");
        URI uri = URI.create("jar:" + zipFile.toUri());

        // Create the virtual file system
        vFs = FileSystems.newFileSystem(uri, Map.of("create", "true"));
        vHome = vFs.getPath("/.cascara/0.10");
        Files.createDirectories(vHome);

        // Point Cascara to the virtual home root
        Cascara.setHomePath(vHome);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (vFs != null && vFs.isOpen()) {
            vFs.close();
        }
    }

    @Test
    void loadsPreferredProviderFromVirtualHome() throws IOException {
        Path propsFile = Cascara.getSplPropertiesPath();
        Files.writeString(propsFile,
            "io.github.qishr.cascara.common.io.ResourceProvider=" +
            "io.github.qishr.cascara.common.io.provider.FileResourceProvider\n"
        );

        // Verify SPL / Cascara reads correctly from NIO Path operations

        Reporter reporter = new StandardReporter().setLevel(Level.DEBUG);
        ServiceProviderRoot spl = ServiceProviderLayer.getRoot(reporter);
        // ServiceProviderRoot root = ServiceProviderLayer.getRoot();
        assertNotNull(spl);

        // Verify SPL works
        SourceBuffer buf = ServiceProviderRoot.loadDefault(SourceBuffer.class);
        assertNotNull(buf);

        // Verify Service Provider factories work
        ServiceProviderFactory spf = new ServiceProviderFactory();
        ScalarDescriptor<?> td = (ScalarDescriptor<?>)spf.createTypeDescriptor(byte[].class);
        assertNotNull(td);
        assertEquals(PrimitiveType.STRING, td.getSchemaType());
        assertEquals("base64", td.getContentEncoding());
    }
}