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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface ArchiveFile<T> extends Closeable {
    T getEntry(String name);
    List<T> getEntries();
    List<T> getEntries(String entryName);
    List<T> getEntries(String entryName, boolean subDirContents);
    Stream<T> stream();
    int size();
    String getComment();
    InputStream getInputStream(T entry) throws IOException;
    InputStream getInputStream(String entryName) throws IOException;
    byte[] extractFile(String entryName) throws IOException;
    ArchiveFile<T> addDirectory(Path entryName) throws IOException;
    ArchiveFile<T> addDirectory(Path sourcePath, String entryName) throws IOException;
    ArchiveFile<T> addFile(Path sourcePath, String entryName) throws IOException;
    ArchiveFile<T> addFile(byte[] content, String entryName) throws IOException;
    ArchiveFile<T> addFile(String content, String entryName) throws IOException;
    ArchiveFile<T> remove(String entryName) throws IOException;
    void close() throws IOException;
}
