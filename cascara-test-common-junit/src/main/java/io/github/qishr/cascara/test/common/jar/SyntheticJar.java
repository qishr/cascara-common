package io.github.qishr.cascara.test.common.jar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.exec.Locatable;
import io.github.qishr.cascara.test.common.security.TestJarSigner;
import io.github.qishr.cascara.test.common.security.TestJarSigner.SignedJarBundle;

public class SyntheticJar implements Locatable {
    Path path;
    String subprojectName;
    Class<?> hostClass;
    String minCascaraVersion;
    List<String> dependencies;
    Map<String, String> sources;
    Map<String, byte[]> classes;
    SignedJarBundle signedBundle;

    public static SyntheticJar forUseIn(Class<?> hostClass) {
        SyntheticJar testJar = new SyntheticJar();
        testJar.hostClass = hostClass;
        return testJar;
    }

    public static SyntheticJar forUseIn(String subProjectName) {
        SyntheticJar testJar = new SyntheticJar();
        testJar.subprojectName = subProjectName;
        return testJar;
    }

    private SyntheticJar() {
        dependencies = new ArrayList<>();
        sources = new HashMap<>();
        classes = new HashMap<>();
    }

    @Override
    public Path getPath() {
        return path;
    }

    public SyntheticJar setPath(Path path) {
        this.path = path;
        return this;
    }

    public SyntheticJar setMinCascaraVersion(String minCascaraVersion) {
        this.minCascaraVersion = minCascaraVersion;
        return this;
    }

    /// Class name to source code
    public SyntheticJar addSources(Map<String,String> sources) {
        this.sources.putAll(sources);
        return this;
    }

    public SyntheticJar addSources(String... paths) {
        for (String resPath : paths) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resPath)) {
                if (is == null) {
                    throw new IllegalStateException("Failed to read resource: " + resPath);
                }

                // 1.  Read source code
                String sourceText = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                // 2. Get class name from source code
                String className = SourceCodeExtractor.extractClassName(sourceText);

                // 3. call addSources wit the map
                addSources(Map.of(className, sourceText));
            } catch (IOException e) {
                throw new TestJarException(e, "Failed to read resource: " + resPath);
            }
        }
        return this;
    }

    public SyntheticJar addDependencies(List<String> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public SyntheticJar addDependencies(String... dependencies) {
        addDependencies(Arrays.asList(dependencies));
        return this;
    }

    public SyntheticJar packageJar() {
        if (path == null) {
            throw new TestJarException("Path not set");
        }
        SyntheticJar testJar = TestModulePackager.packageJar(this);
        return testJar;
    }

    public SyntheticJar packageJar(Path path) {
        this.path = path;
        return packageJar();
    }

    public SignedJarBundle getSignedBundle() {
        return signedBundle;
    }

    //
    // Signing Methods
    //

    /// The one method for all customized signing needs
    public SyntheticJar sign(Consumer<TestJarSigner> config) {
        TestJarSigner signer = new TestJarSigner();
        config.accept(signer);
        return sign(signer);
    }

    public SyntheticJar sign(TestJarSigner signer) {
        ensureClassFiles();
        this.signedBundle = signer.sign(classes);
        return this;
    }

    public SyntheticJar sign() {
        ensureClassFiles();
        try {
            signedBundle = new TestJarSigner().sign(classes);
        } catch (Exception e) {
            throw new TestJarException(e, "Failed to sign JAR");
        }
        return this;
    }

    //
    //
    //

    private void ensureClassFiles() {
        if (classes.isEmpty()) {
            classes = TestModuleCompiler.compileSources(this);
        }
    }
}
