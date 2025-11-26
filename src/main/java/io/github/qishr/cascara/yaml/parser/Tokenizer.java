package io.github.qishr.cascara.yaml.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;

public class Tokenizer {

    public enum TokenType {
        STREAM_START, STREAM_END,
        DOCUMENT_START, DOCUMENT_END,

        INDENT,
        DEDENT,

        KEY_INDICATOR,
        VALUE_INDICATOR,
        SEQUENCE_ENTRY_INDICATOR,
        MAP_START,
        MAP_END,
        SEQUENCE_START,
        SEQUENCE_END,
        COMMA,
        BLOCK_END,

        DIRECTIVE,
        TAG,

        ANCHOR,
        ALIAS,

        SCALAR,

        NEWLINE,
        COMMENT,
        ERROR,
        EOF
    }

    public record Token(
        TokenType type,
        String lexeme,
        Object value,
        int startIndex,
        int line,
        int column) {

        @Override
        public String toString() {
            String displayLexeme = lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"");
            String valuePart = (value != null) ? " (Value: " + value + ")" : "";

            return String.format("[%-20s | '%-15s'%s | L:%d C:%d]",
                type,
                displayLexeme,
                valuePart,
                line,
                column);
        }
    }

    private static final Map<Character, TokenType> FLOW_CONTEXT_SINGLE_CHAR_TOKENS = new HashMap<>();
    static {
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('[', TokenType.SEQUENCE_START);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put(']', TokenType.SEQUENCE_END);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('{', TokenType.MAP_START);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('}', TokenType.MAP_END);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put(',', TokenType.COMMA);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('&', TokenType.ANCHOR);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('*', TokenType.ALIAS);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('!', TokenType.TAG);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('|', TokenType.SCALAR);
        FLOW_CONTEXT_SINGLE_CHAR_TOKENS.put('>', TokenType.SCALAR);
    }

    // --- State Management ---
    private final Deque<Integer> indentationLevels = new ArrayDeque<>();
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;

    private Tokenizer(String source) {
        this.source = source;
        this.indentationLevels.push(0);

        if (!source.isEmpty() && source.charAt(0) == '\uFEFF') {
            this.current = 1;
            this.column = 1;
            this.start = 1;
        }
    }

    public static List<Token> tokenize(String source, boolean verbose) {
        Tokenizer tokenizer = new Tokenizer(source);
        return tokenizer.scanTokens(verbose);
    }

    /* package-private */ List<Token> scanTokens(boolean verbose) {
        addToken(TokenType.STREAM_START, "");

        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        // Handle remaining DEDENTs at EOF
        while (indentationLevels.peek() > 0) {
             // Use addStructuralToken helper
             addStructuralToken(TokenType.DEDENT, column);
             indentationLevels.pop();
        }

        addToken(TokenType.EOF, "");
        addToken(TokenType.STREAM_END, "");

        return tokens;
    }

    private void scanToken() {
        char c = advance();

        // 1. Newlines and Indentation
        if (c == '\n' || c == '\r') {
            handleNewlineAndIndentation(c);
            return;
        }

        // 2. Horizontal Whitespace *Between* Tokens
        if (c == ' ' || c == '\t') {
            start = current;
            return;
        }

        // 3. Structural Markers (Document Start/End)
        if (c == '-' && peek() == '-' && peekNext() == '-') {
            advance(); advance();
            addToken(TokenType.DOCUMENT_START);
            return;
        }

        if (c == '.' && peek() == '.' && peekNext() == '.') {
            advance(); advance();
            addToken(TokenType.DOCUMENT_END);
            return;
        }

        // 4. Comments
        if (c == '#') {
            while (peek() != '\n' && peek() != '\r' && !isAtEnd()) {
                advance();
            }
            addToken(TokenType.COMMENT);
            return;
        }

        // 5. Flow Context Punctuation
        if (FLOW_CONTEXT_SINGLE_CHAR_TOKENS.containsKey(c)) {
            addToken(FLOW_CONTEXT_SINGLE_CHAR_TOKENS.get(c));
            return;
        }

        // 6. Sequence Entry / Key Indicator / Value Indicator
        if (c == '-') {
            if (isWhitespace(peek()) || isAtEnd()) {
                addToken(TokenType.SEQUENCE_ENTRY_INDICATOR);
                return;
            }
        }

        if (c == ':') {
            if (isWhitespace(peek()) || isAtEnd()) {
                addToken(TokenType.KEY_INDICATOR);

                if (peek() == ' ' || peek() == '\t') {
                    advance();
                }
                return;
            }
        }

        // 7. Block/Flow Scalars
        if (c == '\'' || c == '\"') {
            scanQuotedScalar(c);
            return;
        }

        // 8. Plain Scalar (Default)
        scanPlainScalar();
    }

// -----------------------------------------------------------------------------
// INDENTATION LOGIC
// -----------------------------------------------------------------------------

    private void handleNewlineAndIndentation(char c) {
        String newlineLexeme = "\n";

        // Save column before line/column update for token reporting
        int newlineColumn = column - 1;

        // 1. Consume the NEWLINE sequence
        if (c == '\r' && peek() == '\n') {
            advance();
            newlineLexeme = "\r\n";
        }

        // CRITICAL: Emit the NEWLINE token using addExplicitToken
        addExplicitToken(TokenType.NEWLINE, newlineLexeme, newlineColumn);

        line++;
        column = 1;

        // 2. Skip blank lines and comments
        while (true) {
            int checkPos = current;
            while (checkPos < source.length() && (source.charAt(checkPos) == ' ' || source.charAt(checkPos) == '\t')) {
                checkPos++;
            }

            // If line is blank (only whitespace followed by newline/EOF)
            if (checkPos < source.length() && (source.charAt(checkPos) == '\r' || source.charAt(checkPos) == '\n')) {
                current = checkPos; // Advance past spaces
                char nextC = advance(); // Consume \r or \n
                String blankNewlineLexeme = "\n";
                if (nextC == '\r' && peek() == '\n') {
                    advance();
                    blankNewlineLexeme = "\r\n";
                }

                line++; column = 1;
                // Use addExplicitToken for blank line NEWLINE
                addExplicitToken(TokenType.NEWLINE, blankNewlineLexeme, 1);

            // If line is only whitespace followed by a comment
            } else if (checkPos < source.length() && source.charAt(checkPos) == '#') {
                current = checkPos;
                while (peek() != '\n' && peek() != '\r' && !isAtEnd()) advance();
                return; // Comment line is skipped entirely
            } else {
                break; // Found content, proceed to indentation counting
            }
        }

        // 3. Count Indentation Spaces on the next non-blank line
        int spaces = 0;
        int checkPos = current;

        while (checkPos < source.length() && source.charAt(checkPos) == ' ') {
            spaces++;
            checkPos++;
        }

        if (checkPos == source.length()) {
            return; // EOF
        }

        // Consume the counted spaces to update 'current' and 'column'
        while (current < checkPos) {
            advance();
        }

        // 4. Compare Indentation against the stack
        int expectedIndent = indentationLevels.peek();
        int indentColumn = column - spaces; // Column where the indent started

        if (spaces > expectedIndent) {
            // INDENT: New block started
            indentationLevels.push(spaces);
            // CRITICAL: Use addStructuralToken
            addStructuralToken(TokenType.INDENT, indentColumn);

        } else if (spaces < expectedIndent) {
            // DEDENT: One or more blocks ended

            while (spaces < indentationLevels.peek()) {
                indentationLevels.pop();
                // CRITICAL: Use addStructuralToken
                addStructuralToken(TokenType.DEDENT, indentColumn);
            }

            if (spaces != indentationLevels.peek()) {
                System.err.println("Error: Inconsistent indentation level at L:" + line +
                                   " (Found: " + spaces + ", Expected a previous level on stack)");
                addToken(TokenType.ERROR);
            }
        }
    }

// -----------------------------------------------------------------------------
// HELPER METHODS (Centralized Token Addition)
// -----------------------------------------------------------------------------

    /**
     * Helper for standard tokens using 'start' and 'current' (SCALAR, COMMENT, etc.).
     */
    private void addToken(TokenType type) {
        String text = source.substring(start, current);
        int tokenColumn = column - text.length();
        addToken(new Token(type, text, null, start, line, tokenColumn));
    }

    /**
     * Helper for tokens with a known, fixed lexeme (STREAM_START/END, EOF, etc.).
     */
    private void addToken(TokenType type, String lexeme) {
        addToken(new Token(type, lexeme, null, start, line, column));
    }

    /**
     * Helper for tokens like NEWLINE which have a precise, explicitly calculated column.
     */
    private void addExplicitToken(TokenType type, String lexeme, int tokenColumn) {
        addToken(new Token(type, lexeme, null, start, line, tokenColumn));
    }

    /**
     * Helper for structural tokens like INDENT/DEDENT which have empty lexemes.
     */
    private void addStructuralToken(TokenType type, int tokenColumn) {
        addToken(new Token(type, "", null, start, line, tokenColumn));
    }

    private void addToken(Token token) {
        // System.out.println("TokenType " + token.type() + " lexeme: " + token.lexeme());
        tokens.add(token);
    }


    private char advance() {
        char c = source.charAt(current++);
        column++;
        return c;
    }

    public char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    public char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    public boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    // -----------------------------------------------------------------------------
    // SCALAR LOGIC (Remains the same)
    // -----------------------------------------------------------------------------

    private void scanQuotedScalar(char quoteChar) {
        // ... (implementation remains the same as in your provided code) ...
        while (peek() != quoteChar && !isAtEnd()) {
            if (peek() == '\n' || peek() == '\r') {
                advance();
                if (peek() == '\n' && quoteChar == '\'') {
                    advance();
                }
                line++; column = 1;
                continue;
            }
            advance();
        }

        if (isAtEnd()) {
            System.err.println("Error: Unterminated string at L:" + line + " C:" + column);
            addToken(TokenType.ERROR);
            return;
        }

        advance();

        String content = source.substring(start + 1, current - 1);
        String lexeme = source.substring(start, current);
        int tokenColumn = column - lexeme.length();
        tokens.add(new Token(TokenType.SCALAR, lexeme, content, start, line, tokenColumn));
    }

    private void scanPlainScalar() {
        while (!isAtEnd() &&
               !isWhitespace(peek()) &&
               !FLOW_CONTEXT_SINGLE_CHAR_TOKENS.containsKey(peek()) &&
               peek() != '#' ) {

            if (peek() == ':') {
                if (isWhitespace(peekNext()) || peekNext() == '\0') {
                    break;
                }
            }

            advance();
        }

        if (start == current) {
             System.err.println("Error: Unexpected character '" + source.charAt(start) + "' at L:" + line + " C:" + column);
             addToken(TokenType.ERROR);
             return;
        }

        addToken(TokenType.SCALAR);
    }
}