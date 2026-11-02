module cascara.test.common.junit {
    requires java.compiler;
    requires java.logging;
    requires transitive cascara.common;
    requires transitive org.junit.jupiter.api;
    requires org.junit.platform.commons;

    exports io.github.qishr.cascara.test.common.jar;
    exports io.github.qishr.cascara.test.common.junit.base;
    exports io.github.qishr.cascara.test.common.junit.extension;
    exports io.github.qishr.cascara.test.common.util;
    exports io.github.qishr.cascara.test.common.security;

    opens io.github.qishr.cascara.test.common.jar to org.junit.platform.commons;
    opens io.github.qishr.cascara.test.common.junit.base to org.junit.platform.commons;
    opens io.github.qishr.cascara.test.common.junit.extension to org.junit.platform.commons;
    opens io.github.qishr.cascara.test.common.util to org.junit.platform.commons;
    opens io.github.qishr.cascara.test.common.security to org.junit.platform.commons;
}
