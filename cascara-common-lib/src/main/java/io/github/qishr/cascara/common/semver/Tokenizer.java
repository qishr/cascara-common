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


package io.github.qishr.cascara.common.semver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to convert a NPM requirement string into a list of tokens.
 */
public class Tokenizer {
    private static final Map<SemVer.SemVerType, Map<Character, Token>> SPECIAL_CHARS;

    static {
        SPECIAL_CHARS = new HashMap<SemVer.SemVerType, Map<Character, Token>>();

        for (SemVer.SemVerType type : SemVer.SemVerType.values()) {
            SPECIAL_CHARS.put(type, new HashMap<Character, Token>());
        }

        for (TokenType tokenType : TokenType.values()) {
            if (tokenType.character != null) {
                for (SemVer.SemVerType type : SemVer.SemVerType.values()) {
                    if (tokenType.supports(type)) {
                        SPECIAL_CHARS.get(type).put(tokenType.character, new Token(tokenType));
                    }
                }
            }
        }
    }

    /**
     * Takes a NPM requirement string and creates a list of tokens by performing 3 operations:
     * - If the token is a version, it will add the version string
     * - If the token is an operator, it will add the operator
     * - It will insert missing "AND" operators for ranges
     *
     * @param requirement the requirement string
     * @param type the version system used when tokenizing the requirement
     *
     * @return the list of tokens
     */
    protected static List<Token> tokenize(String requirement, SemVer.SemVerType type) {
        Map<Character, Token> specialChars = SPECIAL_CHARS.get(type);

        // Replace the tokens made of 2 chars
        if (type == SemVer.SemVerType.COCOAPODS) {
            requirement = requirement.replace("~>", "~");
        } else if (type == SemVer.SemVerType.NPM) {
            requirement = requirement.replace("||", "|");
        }
        requirement = requirement.replace("<=", "≤").replace(">=", "≥");


        LinkedList<Token> tokens = new LinkedList<Token>();
        Token previousToken = null;

        char[] chars = requirement.toCharArray();
        Token token = null;
        for (char c : chars) {
            if (c == ' ') continue;

            if (specialChars.containsKey(c)) {
                if (token != null) {
                    tokens.add(token);
                    previousToken = token;
                    token = null;
                }

                Token current = specialChars.get(c);
                if (current.type.isUnary() && previousToken != null && previousToken.type == TokenType.VERSION) {
                    // Handling the ranges like "≥1.2.3 <4.5.6" by inserting a "AND" binary operator
                    tokens.add(new Token(TokenType.AND));
                }

                tokens.add(current);
                previousToken = current;
            } else {
                if (token == null) {
                    token = new Token(TokenType.VERSION);
                }
                token.append(c);
            }
        }

        if (token != null) {
            tokens.add(token);
        }

        return tokens;
    }

    /**
     * A token in a requirement string. Has a type and a value if it is of type VERSION
     */
    protected static class Token {
        public final TokenType type;
        public String value;

        public Token(TokenType type) {
            this(type, null);
        }

        public Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        public void append(char c) {
            if (value == null) value = "";
            value += c;
        }
    }

    /**
     * The different types of tokens (unary operators, binary operators, delimiters and versions)
     */
    protected enum TokenType {
        // Unary operators: ~ ^ = < <= > >=
        TILDE('~', true, SemVer.SemVerType.COCOAPODS, SemVer.SemVerType.NPM),
        CARET('^', true, SemVer.SemVerType.NPM),
        EQ('=', true, SemVer.SemVerType.NPM),
        LT('<', true, SemVer.SemVerType.COCOAPODS, SemVer.SemVerType.NPM),
        LTE('≤', true, SemVer.SemVerType.COCOAPODS, SemVer.SemVerType.NPM),
        GT('>', true, SemVer.SemVerType.COCOAPODS, SemVer.SemVerType.NPM),
        GTE('≥', true, SemVer.SemVerType.COCOAPODS, SemVer.SemVerType.NPM),

        // Binary operators: - ||
        HYPHEN('-', false, SemVer.SemVerType.NPM),
        OR('|', false, SemVer.SemVerType.NPM),
        AND(null, false),

        // Delimiters: ( )
        OPENING('(', false, SemVer.SemVerType.NPM),
        CLOSING(')', false, SemVer.SemVerType.NPM),

        // Special
        VERSION(null, false);

        public final Character character;
        private final boolean unary;
        private final SemVer.SemVerType[] supportedTypes;

        TokenType(Character character, boolean unary, SemVer.SemVerType... supportedTypes) {
            this.character = character;
            this.unary = unary;
            this.supportedTypes = supportedTypes;
        }

        public boolean isUnary() {
            return this.unary;
        }

        public boolean supports(SemVer.SemVerType type) {
            for (SemVer.SemVerType t : this.supportedTypes) {
                if (t == type) {
                    return true;
                }
            }
            return false;
        }
    }
}
