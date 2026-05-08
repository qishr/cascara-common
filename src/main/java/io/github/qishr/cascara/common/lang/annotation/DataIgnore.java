package io.github.qishr.cascara.common.lang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Indicates that a field should be ignored during serialization
/// and deserialization.
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataIgnore {
}