module cascara.common {
    requires java.net.http;

    uses io.github.qishr.cascara.common.lang.processor.AstConverter;
    uses io.github.qishr.cascara.common.lang.processor.Emitter;
    uses io.github.qishr.cascara.common.lang.processor.Parser;
    uses io.github.qishr.cascara.common.service.ServiceProvider;

    exports io.github.qishr.cascara.common.lang.annotation;
    exports io.github.qishr.cascara.common.diagnostic;
    exports io.github.qishr.cascara.common.lang;
    exports io.github.qishr.cascara.common.lang.ast;
    exports io.github.qishr.cascara.common.lang.exception;
    exports io.github.qishr.cascara.common.lang.processor;
    exports io.github.qishr.cascara.common.lang.semantic;
    exports io.github.qishr.cascara.common.lang.simple;
    exports io.github.qishr.cascara.common.lang.token;
    exports io.github.qishr.cascara.common.semver;
    exports io.github.qishr.cascara.common.service;
    exports io.github.qishr.cascara.common.spi;
    exports io.github.qishr.cascara.common.util;

    opens io.github.qishr.cascara.common.util;
}
