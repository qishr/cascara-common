package io.github.qishr.cascara.common.lang.token;

public interface Token {
    /// Returns the basic type of this the token, defined in a class that implements TokenType.
    public TokenType getType();

    /// Returns the exact literal characters scanned from the source file,
    /// including any surrounding quotes or syntax indicators.
    public String getLexeme();

    /// Returns the unescaped, processed string content within the token boundaries.
    String getContent();

    /// Returns the total number of bytes/characters read before start of current token.
    public int getOffset();

    /// Returns the input row on which current token starts; 1-based
    public int getStartLine();

    /// Returns the column on input row that current token starts; 1-based
    public int getStartColumn();

    /// Returns a human readable description of the token.
    public String toString();
}
