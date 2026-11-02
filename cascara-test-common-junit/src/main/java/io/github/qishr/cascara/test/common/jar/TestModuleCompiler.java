package io.github.qishr.cascara.test.common.jar;

import javax.tools.*;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.ReportWriter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.test.common.util.ArtifactResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestModuleCompiler {
    private static final Reporter REPORTER = Reporter.forClass(TestModuleCompiler.class);

    public static Map<String, byte[]> compileSources(SyntheticJar testJar) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java compiler not available. Ensure JDK is used.");
        }

        boolean success = false;
        Map<String, byte[]> compiled = null;
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(diagnostics, null, null))) {

            List<JavaFileObject> compilationUnits = testJar.sources.entrySet().stream()
                    .map(e -> new StringJavaFileObject(e.getKey(), e.getValue()))
                    .map(jfo -> (JavaFileObject) jfo)
                    .toList();

            List<String> options = new ArrayList<>();



            Pair<String,String> mp = ArtifactResolver.buildModulePath(testJar.dependencies, testJar.hostClass);



            String argModulePath = mp.getL();
            String argAddModules = mp.getR();

            if (!argModulePath.isEmpty()) {
                options.add("--module-path");
                options.add(argModulePath);
            }

            if (!argAddModules.isEmpty()) {
                options.add("--add-modules");
                options.add(argAddModules);
            }


            // System.out.println("CompilationTask Options:");
            // for (String s : options) {
            //     System.out.println("  " + s);
            // }



            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits
            );

            success = task.call();
            compiled = fileManager.getCompiledBytes();
        } catch (Exception e) {
            // e.printStackTrace();
            throw new RuntimeException("File manage exception: " + e.getMessage());
        }

        if (success) {
            return compiled;
        } else {
            REPORTER.error(GenericMessage.ERROR, "Compilation failed");
            ReportWriter writer = REPORTER.getWriter(Level.ERROR);
            // StringBuilder sb = new StringBuilder("Compilation failed\n");
            if (!diagnostics.getDiagnostics().isEmpty()) {
                writer.write(0,"Diagnostics:\n");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    writer.write(2, d.toString() + "\n");
                }
            }
            // System.err.println(sb.toString());
            throw new TestJarException(GenericMessage.ERROR, "Compilation failed");
        }

            // fileManager.close();

            // return fileManager.getCompiledBytes();
    }

    private static class StringJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        protected StringJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, byte[]> compiledBytes = new HashMap<>();

        protected InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        public Map<String, byte[]> getCompiledBytes() {
            return compiledBytes;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                @Override
                public OutputStream openOutputStream() {
                    return new ByteArrayOutputStream() {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            compiledBytes.put(className.replace('.', '/') + ".class", toByteArray());
                        }
                    };
                }
            };
        }
    }
}