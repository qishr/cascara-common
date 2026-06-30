package io.github.qishr.cascara.common.lang.util;

public interface SourceBuffer {
    char advance();
    char peek();
    char peekNext();
    char peekAhead(int steps);
    void backup();
    boolean isAtEnd();
    
    char charAt(int index);
    CharSequence subSequence(int start, int end);
    int length();

    int line();
    int column();
    int offset();

    // The window tracking state metrics needed by token factories
    void startTokenWindow();
    String getTokenWindowLexeme();
    int windowStartOffset();
    int windowStartLine();
    int windowStartColumn();
}