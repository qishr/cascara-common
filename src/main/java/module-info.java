module cascara.common {
    uses io.github.qishr.cascara.common.service.ServiceProvider;

    exports io.github.qishr.cascara.common.data;
    exports io.github.qishr.cascara.common.data.reference;
    exports io.github.qishr.cascara.common.diagnostic;
    exports io.github.qishr.cascara.common.diagnostic.code;
    exports io.github.qishr.cascara.common.lang.annotation;
    exports io.github.qishr.cascara.common.lang.ast;
    exports io.github.qishr.cascara.common.lang.exception;
    exports io.github.qishr.cascara.common.lang.processor;
    exports io.github.qishr.cascara.common.lang.reference;
    exports io.github.qishr.cascara.common.lang.semantic;
    exports io.github.qishr.cascara.common.lang.token;
    exports io.github.qishr.cascara.common.lang.type;
    exports io.github.qishr.cascara.common.lang.util;
    exports io.github.qishr.cascara.common.semver;
    exports io.github.qishr.cascara.common.service;
    exports io.github.qishr.cascara.common.util;

    opens io.github.qishr.cascara.common.util;

    provides io.github.qishr.cascara.common.lang.type.ScalarDescriptor
        with io.github.qishr.cascara.common.lang.type.ByteArrayDescriptor,
             io.github.qishr.cascara.common.lang.type.InstantTypeDescriptor,
             io.github.qishr.cascara.common.lang.type.LocalDateTimeTypeDescriptor,
             io.github.qishr.cascara.common.lang.type.PathTypeDescriptor,
             io.github.qishr.cascara.common.lang.type.UriTypeDescriptor,
             io.github.qishr.cascara.common.lang.type.UuidTypeDescriptor;
}
