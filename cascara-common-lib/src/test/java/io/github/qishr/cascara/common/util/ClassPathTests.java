package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class ClassPathTests {
    @Test
    void test_doesNotFindExcludedClass() {

        // TODO: add containsClass to ClassPath
        // assertFalse(cp.getClasses().contains(Test.class.getName()));

        Set<String> excluded = Set.of(
            "org.mockito.", "jdk.", "sun.", "com.sun.", "net.bytebuddy.",
            "worker.org.gradle.", "org.objenesis.", "org.opentest4j.",
            "org.jcp.xml.dsig.internal.", "org.junit",
            "com.apple", "apple.laf", "apple.security",
            "java.awt.", "javax.swing."
        );

        ClassPath cp = new ClassPath(null, excluded, null);

        // TODO: add containsClass to ClassPath
        Set<String> allClasses = cp.getClasses();

        for (String ex : excluded) {
            assertFalse(allClasses.contains(ex), ex + " should have been excluded");
        }
    }
}
