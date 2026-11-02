package io.github.qishr.cascara.test.common.junit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.test.common.jar.SourceCodeExtractor;

public class SourceCodeExtractorTest {
    @Test
    void test_extractModduleInfoName() throws IOException {
        String resPath = "code/ModuleInfoA.txt";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resPath)) {

            // 1.  Read source code
            String sourceText = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // 2. Get class name from source code
            String className = SourceCodeExtractor.extractClassName(sourceText);

            // 3. Verify
            assertEquals("module-info", className);

        }
    }

    @Test
    void test_extractProviderAName() throws IOException {
        String resPath = "code/ProviderA.txt";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resPath)) {

            // 1.  Read source code
            String sourceText = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // 2. Get class name from source code
            String className = SourceCodeExtractor.extractClassName(sourceText);

            // 3. Verify
            assertEquals("ProviderA", className);

        }
    }
}
