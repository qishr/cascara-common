package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ModulePathTests {
    public static abstract class TestBase {
        public String x;
    }

    public static class TestImpl extends TestBase {
        public String y;
    }

    @Test
    void testModulePath() {
        String mouleName = getClass().getModule().getName();
        if (mouleName == null || mouleName.isEmpty()) {
            System.out.println("Skipping testModulePath in non-JPMS environment");
        } else {
            ModulePath mp = new ModulePath();
            assertTrue(mp.containsClass(TestBase.class.getName()));
        }
    }

        // @Mock static Reporter reporter = new LocalReporter();
//    {
//        @Override
//        public void print(Kind kind, String message) {
//            System.out.println(kind + ": " + message);
//        }
//
//        @Override
//        public void print(Kind kind, DocTreePath path, String message) {
//            // Do nothing
//        }
//
//        @Override
//        public void print(Kind kind, Element element, String message) {
//            // Do nothing
//        }
//    };

    @Disabled
    @Test
    void processDirectoryUrl_callsProcessClassFileForClassFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-processdir");
        File dir = tempDir.toFile();
        try {
            // create a dummy class file in the directory
            File classFile = new File(dir, "MyClass.class");
            assertTrue(classFile.createNewFile(), "create dummy class file");

            URL[] urls = new URL[] { dir.toURI().toURL() };

            ModulePath modulePath = spy(ModulePath.class);
            // modulePath.ctx = Context.getInstance();
            // modulePath.ctx.setReporter(reporter);

            when(modulePath.scanClassFile(any(Path.class), any(Path.class), any(URLClassLoader.class), any(), any())).thenAnswer(invocation -> {
                File directoryArg = invocation.getArgument(0);
                File fileArg = invocation.getArgument(1);
                // assert the arguments are as expected inside the stub
                assertEquals(dir.getAbsoluteFile(), directoryArg.getAbsoluteFile());
                assertEquals(classFile.getName(), fileArg.getName());
                return null;
            });

            Set<String> discovered = new HashSet<>();
            modulePath.scanDirectoryUrl(tempDir, urls, "");
            verify(modulePath).scanClassFile(any(), any(), any(), any(), discovered);
        } finally {
            // cleanup
            Files.deleteIfExists(tempDir.resolve("MyClass.class"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void processClassFile_readsModuleInfoAndSetsSiblingModuleName() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-moduleinfo");
        File dir = tempDir.toFile();
        File moduleInfo = new File(dir, "module-info.class");
        ModulePath modulePath = new ModulePath("");
        try {
            assertTrue(moduleInfo.createNewFile(), "create module-info.class placeholder");

            // Mock ModuleDescriptor.read to avoid parsing actual class bytes
            ModuleDescriptor fakeDescriptor = mock(ModuleDescriptor.class);
            when(fakeDescriptor.name()).thenReturn("fake.module");

            try (MockedStatic<ModuleDescriptor> mdStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
                mdStatic.when(() -> ModuleDescriptor.read(any(InputStream.class))).thenReturn(fakeDescriptor);

                // classLoader not needed for module-info branch; pass null
                Set<String> discovered = new HashSet<>();
                String r = modulePath.scanClassFile(tempDir, moduleInfo.toPath(), null, null, discovered);

                assertEquals("fake.module", r);
            }
        } finally {
            Files.deleteIfExists(moduleInfo.toPath());
            Files.deleteIfExists(tempDir);
        }
    }
}
