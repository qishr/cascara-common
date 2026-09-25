package io.github.qishr.cascara.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;

public class ClassHierarchyTestBase {
    protected void outputHierarchy(Map<String, Set<String>> hierarchy) {
        StandardReporter reporter = new StandardReporter()
            .setLevel(Level.DEBUG);

        hierarchy.keySet().stream().sorted().forEach(className -> {
            reporter.debug(className);
            hierarchy.get(className).stream().sorted().forEach(subclassName -> {
                reporter.debug("  " + subclassName);
            });
        });
    }

    protected void outputClasses(Map<String, Set<String>> hierarchy) {
        StandardReporter reporter = new StandardReporter()
            .setLevel(Level.DEBUG);

        StringBuilder sb = new StringBuilder();
        hierarchy.keySet().stream().sorted().forEach(className -> {
            reporter.debug(className);
            sb.append(className);
            sb.append("\n");
        });
        try {
            Files.writeString(Path.of("/tmp/classes"), sb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
