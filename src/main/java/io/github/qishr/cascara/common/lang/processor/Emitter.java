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


package io.github.qishr.cascara.common.lang.processor;

import io.github.qishr.cascara.common.util.ContentType;

/// A generic interface for emitting structured text.
public interface Emitter extends Processor {
    /// Writes a literal scalar value.
    void emitScalar(String value);

    /// Signals the start of a mapping/object.
    void emitMapStart();

    /// Signals the end of a mapping/object.
    void emitMapEnd();

    /// Signals the start of a sequence/list.
    void emitSequenceStart();

    /// Signals the end of a sequence/list.
    void emitSequenceEnd();

    /// Writes a separator between key and value.
    void emitPropertySeparator();

    /// Writes a separator between items in a collection.
    void emitItemSeparator();

    /// Manages white-space and indentation.
    void emitNewLine();
    void indent();
    void dedent();

    /// Returns the final produced string.
    String getOutput();

    ContentType getContentType();
}