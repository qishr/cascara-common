package io.github.qishr.cascara.common.io;

import io.github.qishr.cascara.common.content.ContentType;

/// Any resource reachable by a URI, not just resource in the Java sense.
public record ResourceContent(String content, ContentType contentType) {}