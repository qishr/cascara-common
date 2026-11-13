package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

public class ClassesTests {
    private static Set<String> allClasses ;

    @BeforeAll
    static void obtainClasses() {
        allClasses = Classes.getAll();
    }

    @Test
    void test_findsCascaraClass() {
        assertTrue(allClasses.contains(Cascara.class.getName()));
    }

    @Test
    void test_findsJreClass() {
        assertTrue(allClasses.contains("java.util.HashSet"));
    }

    @Test
    void test_doesNotFindExcludedClasses() {
        Set<String> excluded = Set.of(
            "org.mockito.", "jdk.", "sun.", "com.sun.", "net.bytebuddy.",
            "worker.org.gradle.", "org.objenesis.", "org.opentest4j.",
            "org.jcp.xml.dsig.internal.", "org.junit",
            "com.apple", "apple.laf", "apple.security",
            "java.awt.", "javax.swing."
        );

        for (String ex : excluded) {
            assertFalse(allClasses.contains(ex), ex + " should have been excluded");
        }
    }

}
