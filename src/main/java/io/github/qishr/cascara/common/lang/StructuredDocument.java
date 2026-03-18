package io.github.qishr.cascara.common.lang;

import java.net.URI;

import io.github.qishr.cascara.common.lang.ast.AstNode;

/// Represents a top-level language resource that contains structured data.
public interface StructuredDocument extends AstNode {
    /// Returns the root data node (usually a Map or Sequence).
    AstNode getRoot();

    /// Returns the origin URI of the document
    URI getOriginUri();

    /// Returns the URI of the schema that defines this document's structure,
    /// typically extracted from a header comment or metadata field.
    URI getSchemaUri();
}