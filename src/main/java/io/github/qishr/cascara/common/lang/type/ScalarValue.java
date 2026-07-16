package io.github.qishr.cascara.common.lang.type;

public record ScalarValue(
    Object nativeValue,
    PrimitiveType type
) {}
