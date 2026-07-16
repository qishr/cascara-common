package io.github.qishr.cascara.common.lang.util;

import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
public interface SimdCapableBuffer extends SourceBuffer {

    /// Skips whitespace only (space, tab, CR, LF)
    void skipWhitespaceSimd();

    int scanDigitsSimd(int pos);

}
