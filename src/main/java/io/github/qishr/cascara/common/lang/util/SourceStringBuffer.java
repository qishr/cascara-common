package io.github.qishr.cascara.common.lang.util;

import java.nio.charset.StandardCharsets;

import io.github.qishr.cascara.common.lang.annotation.Experimental;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

@Experimental
public class SourceStringBuffer implements SimdCapableBuffer, LexemeProvider, CharSequence {

    private final String source;
    private int line = 1;
    private int column = 1;
    private int offset = 0;
    private int windowStartOffset = 0;
    private int windowStartLine = 1;
    private int windowStartColumn = 1;
    private final byte[] raw;


    public SourceStringBuffer(String source) {
        this.source = source != null ? source : "";
        this.raw = this.source.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String slice(int startOffset, int endOffset) {
        return source.substring(startOffset, endOffset);
    }

    @Override
    public int length() {
        return source.length();
    }

    @Override
    public char charAt(int index) {
        return source.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return source.subSequence(start, end);
    }

    @Override
    public char advance() {
        if (isAtEnd()) {
            return '\0';
        }
        char c = source.charAt(offset++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    @Override
    public char peekAhead(int steps) {
        if (offset + steps >= source.length()) return '\0';
        return source.charAt(offset + steps);
    }

    @Override
    public char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(offset);
    }

    @Override
    public char peekNext() {
        if (offset + 1 >= source.length()) return '\0';
        return source.charAt(offset + 1);
    }

    @Override
    public void backup() {
        if (offset == windowStartOffset) {
            throw new IllegalStateException("Cannot backup past the start of the current token window.");
        }
        offset--;
        char c = source.charAt(offset);
        if (c == '\n') {
            line--;
            column = 1; // Safeguard fallback (same as stream buffer)
        } else {
            column--;
        }
    }

    @Override
    public boolean isAtEnd() {
        return offset >= source.length();
    }

    @Override public int line() { return line; }
    @Override public int column() { return column; }
    @Override public int offset() { return offset; }

    @Override
    public String getTokenWindowLexeme() {
        return source.substring(windowStartOffset, offset);
    }

    @Override
    public void startTokenWindow() {
        this.windowStartOffset = this.offset;
        this.windowStartLine = this.line;
        this.windowStartColumn = this.column;
    }

    @Override
    public int windowStartOffset() {
        return windowStartOffset;
    }

    @Override
    public int windowStartLine() {
        return windowStartLine;
    }

    @Override
    public int windowStartColumn() {
        return windowStartColumn;
    }

    @Override
    public void skipWhitespaceSimd() {
        final int len = raw.length;
        int pos = offset;

        final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_128;

        final byte SPACE = (byte)' ';
        final byte TAB   = (byte)'\t';
        final byte CR    = (byte)'\r';
        final byte LF    = (byte)'\n';

        while (pos < len) {
            int remaining = len - pos;

            if (remaining < SPECIES.length()) {
                // Scalar tail
                while (pos < len) {
                    byte b = raw[pos];
                    if (b == SPACE || b == TAB || b == CR) {
                        pos++;
                        column++;
                    } else if (b == LF) {
                        pos++;
                        line++;
                        column = 1;
                    } else {
                        offset = pos;
                        return;
                    }
                }
                offset = pos;
                return;
            }

            // Load 16 bytes
            ByteVector vec = ByteVector.fromArray(SPECIES, raw, pos);

            // Compare against whitespace
            VectorMask<Byte> mSpace = vec.compare(VectorOperators.EQ, SPACE);
            VectorMask<Byte> mTab   = vec.compare(VectorOperators.EQ, TAB);
            VectorMask<Byte> mCr    = vec.compare(VectorOperators.EQ, CR);
            VectorMask<Byte> mLf    = vec.compare(VectorOperators.EQ, LF);

            // Combine masks
            VectorMask<Byte> wsMask = mSpace.or(mTab).or(mCr).or(mLf);

            // Convert mask to a long bitmask
            long maskBits = wsMask.toLong();  // THIS is the correct API

            // If first byte is non-whitespace, stop
            if ((maskBits & 1L) == 0L) {
                offset = pos;
                return;
            }

            // Count leading whitespace bytes
            int leading = Long.numberOfTrailingZeros(~maskBits);
            if (leading > SPECIES.length()) leading = SPECIES.length();

            // Update line/column
            for (int i = 0; i < leading; i++) {
                byte b = raw[pos + i];
                if (b == LF) {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }

            pos += leading;
        }

        offset = pos;
    }

    public int scanDigitsSimd(int pos) {
        final int len = raw.length;
        final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_128;

        final byte ZERO = (byte)'0';
        final byte NINE = (byte)'9';

        while (pos < len) {
            int remaining = len - pos;

            if (remaining < SPECIES.length()) {
                // Scalar tail
                while (pos < len) {
                    byte b = raw[pos];
                    if (b >= ZERO && b <= NINE) {
                        pos++;
                        column++;
                    } else {
                        return pos;
                    }
                }
                return pos;
            }

            ByteVector vec = ByteVector.fromArray(SPECIES, raw, pos);

            VectorMask<Byte> ge0 = vec.compare(VectorOperators.GE, ZERO);
            VectorMask<Byte> le9 = vec.compare(VectorOperators.LE, NINE);
            VectorMask<Byte> digitMask = ge0.and(le9);

            long maskBits = digitMask.toLong();

            // First byte non-digit → stop
            if ((maskBits & 1L) == 0L) {
                return pos;
            }

            // Count leading digits
            int leading = Long.numberOfTrailingZeros(~maskBits);
            if (leading > SPECIES.length()) leading = SPECIES.length();

            column += leading;
            pos += leading;
        }

        return pos;
    }

    @Override
    public void setOffset(int newOffset) {
        if (newOffset < 0 || newOffset > source.length()) {
            throw new IndexOutOfBoundsException("Invalid offset: " + newOffset);
        }

        // Update offset
        this.offset = newOffset;

        // Recompute line/column from scratch
        // (SIMD skips multiple chars, so we must rebuild position)
        int line = 1;
        int column = 1;

        for (int i = 0; i < newOffset; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        this.line = line;
        this.column = column;
    }

    public void advanceBy(int n) {
        for (int i = 0; i < n; i++) {
            advance();
        }
    }
}