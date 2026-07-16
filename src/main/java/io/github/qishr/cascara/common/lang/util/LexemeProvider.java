package io.github.qishr.cascara.common.lang.util;

import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
public interface LexemeProvider {
    String slice(int startOffset, int endOffset);
}
