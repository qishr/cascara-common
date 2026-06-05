package io.github.qishr.cascara.common.lang.token;

public interface Token {
    public TokenType getType();

    /// Returns the exact literal characters scanned from the source file,
    /// including any surrounding quotes or syntax indicators.
    public String getLexeme();

    /// Returns the unescaped, processed string content within the token boundaries.
    String getContent();

    public int getOffset();
    public int getStartLine();
    public int getStartColumn();
    public String toString();
}
