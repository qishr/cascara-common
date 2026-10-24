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


package io.github.qishr.cascara.common.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.semver.SemVer;

public class JarManifest extends Properties {
    private final Attributes attr = new Attributes();
    private final JarFile.Verifier jv;

    // Charset UTF_8 = StandardCharsets.UTF_8;

    // manifest entries
    private final Map<String, Attributes> entries = new HashMap<>();

    private JarManifest() {
        // No public constructor
        jv = null;
    }

    private JarManifest(JarFile.Verifier jv) {
        // No public constructor
        this.jv = jv;
    }

    public JarManifest(JarManifest man) {
        attr.putAll(man.getMainAttributes());
        entries.putAll(man.getEntries());
        jv = man.jv;
    }

    /// Parses the content of a MANIFEST.MF file string, handles line continuations,
    /// and prints the resulting property names and values.
    ///
    /// @param manifest The string containing the MANIFEST.MF file content.
    public static JarManifest parse(String manifest) {
        JarManifest manifestProperties = new JarManifest();
        Map<String, StringBuilder> mf = new LinkedHashMap<>();
        String currentKey = null;
        try (BufferedReader reader = new BufferedReader(new StringReader(manifest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    // Empty line signifies the end of the main section or an entry block
                    currentKey = null;
                    continue;
                }
                if (line.startsWith(" ")) {
                    // A line starting with a space (0x20) is a continuation of the previous line's value.
                    if (currentKey != null) {
                        // Append the continuation line, trimming the leading space (0x20)
                        String continuation = line.substring(1);
                        mf.get(currentKey).append(continuation);
                    }
                    // Note: If currentKey is null here, it's an improperly formatted continuation line, so we ignore it.
                } else {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        currentKey = key;
                        mf.put(key, new StringBuilder(value));
                    }
                }
            }
        } catch (Exception e) {
            throw new LocalizableRuntimeException(e, GenericMessage.MANIFEST_READ, e.getMessage());
        }

        for (Map.Entry<String, StringBuilder> entry : mf.entrySet()) {
            manifestProperties.set(entry.getKey(), entry.getValue().toString());
        }

        return manifestProperties;
    }

    public static JarManifest of(Class<?> clazz) {
        InputStream is;
        try {
            is = JreUtils.getResourceAsStream(clazz, "/META-INF/MANIFEST.MF");
        } catch (LocalizableIOException e) {
            throw new LocalizableRuntimeException(
                e,
                GenericMessage.MANIFEST_READ,
                clazz.getSimpleName()
            );
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String content = br.lines().collect(Collectors.joining("\n"));
            return parse(content);
        } catch (Exception e) {
            throw new LocalizableRuntimeException(
                e,
                GenericMessage.MANIFEST_READ,
                clazz.getSimpleName()
            );
        }
    }

    public SemVer getVersion() {
        return new SemVer(getString("Implementation-Version", "0.0.0"));
    }

    public Attributes getMainAttributes() {
        return attr;
    }

    public Map<String,Attributes> getEntries() {
        return entries;
    }

    public Attributes getAttributes(String name) {
        return getEntries().get(name);
    }

    // TODO: Make this NonNull
    @Nullable
    Attributes getTrustedAttributes(String name) {
        // Note: Before the verification of MANIFEST.MF/.SF/.RSA files is done,
        // jv.isTrustedManifestEntry() isn't able to detect MANIFEST.MF change.
        // Users of this method should call SharedSecrets.javaUtilJarAccess()
        // .ensureInitialization() first.
        Attributes result = getAttributes(name);
        if (result != null && jv != null && ! jv.isTrustedManifestEntry(name)) {
            throw new SecurityException("Untrusted manifest entry: " + name);
        }
        return result;
    }

        static String getErrorPosition(String filename, final int lineNumber) {

        // TODO: Localized exceptions

        // if (filename == null ||
        //         !SecurityProperties.INCLUDE_JAR_NAME_IN_EXCEPTIONS) {
        //     return "line " + lineNumber;
        // }

        if (filename == null) {
            return "line " + lineNumber;
        }

        return "manifest of " + filename + ":" + lineNumber;
    }

    // /**
    //  * Reads the Manifest from the specified InputStream. The entry
    //  * names and attributes read will be merged in with the current
    //  * manifest entries.
    //  *
    //  * @param is the input stream
    //  * @throws    IOException if an I/O error has occurred
    //  */
    // public void read(InputStream is) throws IOException {
    //     read(is, null);
    // }

    // private void read(InputStream is, String jarFilename) throws IOException {
    //     // Buffered input stream for reading manifest data
    //     FastInputStream fis = new FastInputStream(is);
    //     // Line buffer
    //     byte[] lbuf = new byte[512];
    //     // Read the main attributes for the manifest
    //     int lineNumber = attr.read(fis, lbuf, jarFilename, 0);
    //     // Total number of entries, attributes read
    //     int ecount = 0, acount = 0;
    //     // Average size of entry attributes
    //     int asize = 2;
    //     // Now parse the manifest entries
    //     int len;
    //     String name = null;
    //     boolean skipEmptyLines = true;
    //     byte[] lastline = null;

    //     while ((len = fis.readLine(lbuf)) != -1) {
    //         byte c = lbuf[--len];
    //         lineNumber++;

    //         if (c != '\n' && c != '\r') {
    //             throw new IOException("manifest line too long ("
    //                        + getErrorPosition(jarFilename, lineNumber) + ")");
    //         }
    //         if (len > 0 && lbuf[len-1] == '\r') {
    //             --len;
    //         }
    //         if (len == 0 && skipEmptyLines) {
    //             continue;
    //         }
    //         skipEmptyLines = false;

    //         if (name == null) {
    //             name = parseName(lbuf, len);
    //             if (name == null) {
    //                 throw new IOException("invalid manifest format ("
    //                           + getErrorPosition(jarFilename, lineNumber) + ")");
    //             }
    //             if (fis.peek() == ' ') {
    //                 // name is wrapped
    //                 lastline = new byte[len - 6];
    //                 System.arraycopy(lbuf, 6, lastline, 0, len - 6);
    //                 continue;
    //             }
    //         } else {
    //             // continuation line
    //             byte[] buf = new byte[lastline.length + len - 1];
    //             System.arraycopy(lastline, 0, buf, 0, lastline.length);
    //             System.arraycopy(lbuf, 1, buf, lastline.length, len - 1);
    //             if (fis.peek() == ' ') {
    //                 // name is wrapped
    //                 lastline = buf;
    //                 continue;
    //             }
    //             name = new String(buf, StandardCharsets.UTF_8);
    //             lastline = null;
    //         }
    //         Attributes attr = getAttributes(name);
    //         if (attr == null) {
    //             attr = new Attributes(asize);
    //             entries.put(name, attr);
    //         }
    //         lineNumber = attr.read(fis, lbuf, jarFilename, lineNumber);
    //         ecount++;
    //         acount += attr.size();
    //         //XXX: Fix for when the average is 0. When it is 0,
    //         // you get an Attributes object with an initial
    //         // capacity of 0, which tickles a bug in HashMap.
    //         asize = Math.max(2, acount / ecount);

    //         name = null;
    //         skipEmptyLines = true;
    //     }
    // }

    private String parseName(byte[] lbuf, int len) {
        if (toLower(lbuf[0]) == 'n' && toLower(lbuf[1]) == 'a' &&
            toLower(lbuf[2]) == 'm' && toLower(lbuf[3]) == 'e' &&
            lbuf[4] == ':' && lbuf[5] == ' ') {
            return new String(lbuf, 6, len - 6, StandardCharsets.UTF_8);
        }
        return null;
    }

    private int toLower(int c) {
        return (c >= 'A' && c <= 'Z') ? 'a' + (c - 'A') : c;
    }

    /**
     * Returns true if the specified Object is also a Manifest and has
     * the same main Attributes and entries.
     *
     * @param o the object to be compared
     * @return true if the specified Object is also a Manifest and has
     * the same main Attributes and entries
     */
    public boolean equals(Object o) {
        return o instanceof JarManifest m
                && attr.equals(m.getMainAttributes())
                && entries.equals(m.getEntries());
    }

    /**
     * Returns the hash code for this Manifest.
     */
    public int hashCode() {
        return attr.hashCode() + entries.hashCode();
    }

    /**
     * Returns a shallow copy of this Manifest.  The shallow copy is
     * implemented as follows:
     * <pre>
     *     public Object clone() { return new Manifest(this); }
     * </pre>
     * @return a shallow copy of this Manifest
     */
    public Object clone() {
        return new JarManifest(this);
    }

    /*
     * A fast buffered input stream for parsing manifest files.
     */
    static class FastInputStream extends FilterInputStream {
        private byte buf[];
        private int count = 0;
        private int pos = 0;

        FastInputStream(InputStream in) {
            this(in, 8192);
        }

        FastInputStream(InputStream in, int size) {
            super(in);
            buf = new byte[size];
        }

        public int read() throws IOException {
            if (pos >= count) {
                fill();
                if (pos >= count) {
                    return -1;
                }
            }
            return Byte.toUnsignedInt(buf[pos++]);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int avail = count - pos;
            if (avail <= 0) {
                if (len >= buf.length) {
                    return in.read(b, off, len);
                }
                fill();
                avail = count - pos;
                if (avail <= 0) {
                    return -1;
                }
            }
            if (len > avail) {
                len = avail;
            }
            System.arraycopy(buf, pos, b, off, len);
            pos += len;
            return len;
        }

        /*
         * Reads 'len' bytes from the input stream, or until an end-of-line
         * is reached. Returns the number of bytes read.
         */
        public int readLine(byte[] b, int off, int len) throws IOException {
            byte[] tbuf = this.buf;
            int total = 0;
            while (total < len) {
                int avail = count - pos;
                if (avail <= 0) {
                    fill();
                    avail = count - pos;
                    if (avail <= 0) {
                        return -1;
                    }
                }
                int n = len - total;
                if (n > avail) {
                    n = avail;
                }
                int tpos = pos;
                int maxpos = tpos + n;
                byte c = 0;
                // jar.spec.newline: CRLF | LF | CR (not followed by LF)
                while (tpos < maxpos && (c = tbuf[tpos++]) != '\n' && c != '\r');
                if (c == '\r' && tpos < maxpos && tbuf[tpos] == '\n') {
                    tpos++;
                }
                n = tpos - pos;
                System.arraycopy(tbuf, pos, b, off, n);
                off += n;
                total += n;
                pos = tpos;
                c = tbuf[tpos-1];
                if (c == '\n') {
                    break;
                }
                if (c == '\r') {
                    if (count == pos) {
                        // try to see if there is a trailing LF
                        fill();
                        if (pos < count && tbuf[pos] == '\n') {
                            if (total < len) {
                                b[off++] = '\n';
                                total++;
                            } else {
                                // we should always have big enough lbuf but
                                // just in case we don't, replace the last CR
                                // with LF.
                                b[off - 1] = '\n';
                            }
                            pos++;
                        }
                    }
                    break;
                }
            }
            return total;
        }

        public byte peek() throws IOException {
            if (pos == count)
                fill();
            if (pos == count)
                return -1; // nothing left in buffer
            return buf[pos];
        }

        public int readLine(byte[] b) throws IOException {
            return readLine(b, 0, b.length);
        }

        public long skip(long n) throws IOException {
            if (n <= 0) {
                return 0;
            }
            long avail = count - pos;
            if (avail <= 0) {
                return in.skip(n);
            }
            if (n > avail) {
                n = avail;
            }
            pos += (int) n;
            return n;
        }

        public int available() throws IOException {
            return (count - pos) + in.available();
        }

        public void close() throws IOException {
            if (in != null) {
                in.close();
                in = null;
                buf = null;
            }
        }

        private void fill() throws IOException {
            count = pos = 0;
            int n = in.read(buf, 0, buf.length);
            if (n > 0) {
                count = n;
            }
        }
    }



    public static class ManifestDigester {

        /**
         * The part "{@code Manifest-Main-Attributes}" of the main attributes
         * digest header name in a signature file as described in the jar
         * specification:
         * <blockquote>{@code x-Digest-Manifest-Main-Attributes}
         * (where x is the standard name of a {@link MessageDigest} algorithm):
         * The value of this attribute is the digest value of the main attributes
         * of the manifest.</blockquote>
         * @see <a href="{@docRoot}/../specs/jar/jar.html#signature-file">
         * JAR File Specification, section Signature File</a>
         * @see #getMainAttsEntry
         */
        public static final String MF_MAIN_ATTRS = "Manifest-Main-Attributes";

        /** the raw bytes of the manifest */
        private final byte[] rawBytes;

        private final Entry mainAttsEntry;

        /** individual sections by their names */
        private final HashMap<String, Entry> entries = new HashMap<>();

        /** state returned by findSection */
        static class Position {
            int endOfFirstLine; // not including newline character

            int endOfSection; // end of section, not including the blank line
                              // between sections
            int startOfNext;  // the start of the next section
        }

        /**
         * find a section in the manifest.
         *
         * @param offset should point to the starting offset with in the
         * raw bytes of the next section.
         *
         * @pos set by
         *
         * @return false if end of bytes has been reached, otherwise returns
         *          true
         */
        @SuppressWarnings("fallthrough")
        private boolean findSection(int offset, Position pos)
        {
            int i = offset, len = rawBytes.length;
            int last = offset - 1;
            boolean allBlank = true;

            /* denotes that a position is not yet assigned.
             * As a primitive type int it cannot be null
             * and -1 would be confused with (i - 1) when i == 0 */
            final int UNASSIGNED = Integer.MIN_VALUE;

            pos.endOfFirstLine = UNASSIGNED;

            while (i < len) {
                byte b = rawBytes[i];
                switch(b) {
                case '\r':
                    if (pos.endOfFirstLine == UNASSIGNED)
                        pos.endOfFirstLine = i-1;
                    if (i < len - 1 && rawBytes[i + 1] == '\n')
                        i++;
                    /* fall through */
                case '\n':
                    if (pos.endOfFirstLine == UNASSIGNED)
                        pos.endOfFirstLine = i-1;
                    if (allBlank || (i == len-1)) {
                        pos.endOfSection = allBlank ? last : i;
                        pos.startOfNext = i+1;
                        return true;
                    }
                    else {
                        // start of a new line
                        last = i;
                        allBlank = true;
                    }
                    break;
                default:
                    allBlank = false;
                    break;
                }
                i++;
            }
            return false;
        }

        public ManifestDigester(byte[] bytes)
        {
            rawBytes = bytes;

            Position pos = new Position();

            if (!findSection(0, pos)) {
                mainAttsEntry = null;
                return; // XXX: exception?
            }

            // create an entry for main attributes
            mainAttsEntry = new Entry().addSection(new Section(
                    0, pos.endOfSection + 1, pos.startOfNext, rawBytes));

            int start = pos.startOfNext;
            while(findSection(start, pos)) {
                int len = pos.endOfFirstLine-start+1;
                int sectionLen = pos.endOfSection-start+1;
                int sectionLenWithBlank = pos.startOfNext-start;

                if (len >= 6) { // 6 == "Name: ".length()
                    if (isNameAttr(bytes, start)) {
                        ByteArrayOutputStream nameBuf = new ByteArrayOutputStream();
                        nameBuf.write(bytes, start+6, len-6);

                        int i = start + len;
                        if ((i-start) < sectionLen) {
                            if (bytes[i] == '\r'
                                    && i + 1 - start < sectionLen
                                    && bytes[i + 1] == '\n') {
                                i += 2;
                            } else {
                                i += 1;
                            }
                        }

                        while ((i-start) < sectionLen) {
                            if (bytes[i++] == ' ') {
                                // name is wrapped
                                int wrapStart = i;
                                while (((i-start) < sectionLen)
                                        && (bytes[i] != '\r')
                                        && (bytes[i] != '\n')) i++;
                                int wrapLen = i - wrapStart;
                                if (i - start < sectionLen) {
                                    i++;
                                    if (bytes[i - 1] == '\r'
                                        && i - start < sectionLen
                                        && bytes[i] == '\n')
                                            i++;
                                }

                                nameBuf.write(bytes, wrapStart, wrapLen);
                            } else {
                                break;
                            }
                        }

                        entries.computeIfAbsent(nameBuf.toString(StandardCharsets.UTF_8),
                                                dummy -> new Entry())
                                .addSection(new Section(start, sectionLen,
                                        sectionLenWithBlank, rawBytes));
                    }
                }
                start = pos.startOfNext;
            }
        }

        private boolean isNameAttr(byte[] bytes, int start)
        {
            return ((bytes[start] == 'N') || (bytes[start] == 'n')) &&
                   ((bytes[start+1] == 'a') || (bytes[start+1] == 'A')) &&
                   ((bytes[start+2] == 'm') || (bytes[start+2] == 'M')) &&
                   ((bytes[start+3] == 'e') || (bytes[start+3] == 'E')) &&
                   (bytes[start+4] == ':') &&
                   (bytes[start+5] == ' ');
        }

        public static class Entry {

            // One Entry for one name, and one name can have multiple sections.
            // According to the JAR File Specification: "If there are multiple
            // individual sections for the same file entry, the attributes in
            // these sections are merged."
            private final List<Section> sections = new ArrayList<>();
            boolean oldStyle;

            private Entry addSection(Section sec)
            {
                sections.add(sec);
                return this;
            }

            /**
             * Check if the sections (particularly the last one of usually only one)
             * are properly delimited with a trailing blank line so that another
             * section can be correctly appended and return {@code true} or return
             * {@code false} to indicate that reproduction is not advised and should
             * be carried out with a clean "normalized" newly-written manifest.
             *
             * @see #reproduceRaw
             */
            public boolean isProperlyDelimited() {
                return sections.stream().allMatch(
                        Section::isProperlySectionDelimited);
            }

            public void reproduceRaw(OutputStream out) throws IOException {
                for (Section sec : sections) {
                    out.write(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                }
            }

            public byte[] digest(MessageDigest md)
            {
                md.reset();
                for (Section sec : sections) {
                    if (oldStyle) {
                        Section.doOldStyle(md, sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                    } else {
                        md.update(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                    }
                }
                return md.digest();
            }

            /** Netscape doesn't include the new line. Intel and JavaSoft do */

            public byte[] digestWorkaround(MessageDigest md)
            {
                md.reset();
                for (Section sec : sections) {
                    md.update(sec.rawBytes, sec.offset, sec.length);
                }
                return md.digest();
            }
        }

        private static class Section {
            int offset;
            int length;
            int lengthWithBlankLine;
            byte[] rawBytes;

            public Section(int offset, int length,
                         int lengthWithBlankLine, byte[] rawBytes)
            {
                this.offset = offset;
                this.length = length;
                this.lengthWithBlankLine = lengthWithBlankLine;
                this.rawBytes = rawBytes;
            }

            /**
             * Returns {@code true} if the raw section is terminated with a blank
             * line so that another section can possibly be appended resulting in a
             * valid manifest and {@code false} otherwise.
             */
            private boolean isProperlySectionDelimited() {
                return lengthWithBlankLine > length;
            }

            private static void doOldStyle(MessageDigest md,
                                    byte[] bytes,
                                    int offset,
                                    int length)
            {
                // this is too gross to even document, but here goes
                // the 1.1 jar verification code ignored spaces at the
                // end of lines when calculating digests, so that is
                // what this code does. It only gets called if we
                // are parsing a 1.1 signed signature file
                int i = offset;
                int start = offset;
                int max = offset + length;
                int prev = -1;
                while(i <max) {
                    if ((bytes[i] == '\r') && (prev == ' ')) {
                        md.update(bytes, start, i-start-1);
                        start = i;
                    }
                    prev = bytes[i];
                    i++;
                }
                md.update(bytes, start, i-start);
            }
        }

        /**
         * @see #MF_MAIN_ATTRS
         */
        public Entry getMainAttsEntry() {
            return mainAttsEntry;
        }

        /**
         * @see #MF_MAIN_ATTRS
         */
        public Entry getMainAttsEntry(boolean oldStyle) {
            if (mainAttsEntry != null) {
                mainAttsEntry.oldStyle = oldStyle;
                return mainAttsEntry;
            } else {
                return null;
            }
        }

        public Entry get(String name) {
            return entries.get(name);
        }

        public Entry get(String name, boolean oldStyle) {
            Entry e = get(name);
            if (e == null && MF_MAIN_ATTRS.equals(name)) {
                e = getMainAttsEntry();
            }
            if (e != null) {
                e.oldStyle = oldStyle;
            }
            return e;
        }

        public byte[] manifestDigest(MessageDigest md) {
            md.reset();
            md.update(rawBytes, 0, rawBytes.length);
            return md.digest();
        }

    }

}
