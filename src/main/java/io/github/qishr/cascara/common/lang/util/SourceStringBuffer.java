// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.lang.util;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.UnimplementedMethodException;
import io.github.qishr.cascara.common.util.Properties;

@Experimental
public class SourceStringBuffer implements SourceBuffer, LexemeProvider, CharSequence {
    private Properties properties;
    private final String contentType = "text/*";

    private String source;
    private int line = 1;
    private int column = 1;
    private int offset = 0;
    private int windowStartOffset = 0;
    private int windowStartLine = 1;
    private int windowStartColumn = 1;
    private byte[] raw;
    private char previous;

    public SourceStringBuffer() {
    }

    @Override
    public Properties getServiceProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.set("contentType", contentType);
        }
        return properties;
    }

    public SourceStringBuffer open(String source) {
        this.source = source != null ? source : "";
        this.raw = this.source.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    @Override
    public SourceStringBuffer open(byte[] data) {
        throw new UnimplementedMethodException();
    }

    @Override
    public SourceStringBuffer open(Reader reader) {
        throw new UnimplementedMethodException();
    }

    @Override
    public SourceStringBuffer open(InputStream is) {
        throw new UnimplementedMethodException();
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
        previous = peek();
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
    public char previous() {
        return previous;
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