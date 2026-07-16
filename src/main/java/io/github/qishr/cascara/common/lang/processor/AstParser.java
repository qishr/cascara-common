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

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.token.Token;

@Beta
public interface AstParser<N extends AstNode, T extends Token> extends Processor {
    /// Entry point for parsing a source string.
    ///
    /// @param text The raw text source.
    /// @return The root [AstNode].
    N parse(String text);

    N parse(Reader reader);

    /// Entry point for parsing an `InputStream`.
    ///
    /// @param is is An input stream of the raw text source.
    /// @return The root [AstNode].
    N parse(InputStream is);

    /// Primary parsing core driven directly by the Tokenizer interface structure.
    ///
    /// @param tokenizer the tokenizer instance.
    /// @return The root [AstNode].
    N parse(Tokenizer<T> tokenizer);

    /// Entry point for parsing a list of tokens.
    ///
    /// @param tokens A list of tokens representing the tokenized text source.
    /// @return The root [AstNode].
    N parse(List<T> tokens);
}
