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

package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;

public enum SecurityMessage implements LocalizableMessage {
    NO_ALGORITHM("SEC-101", "No suitable algorithm found for: {0}"),
    UNSUPPORTED_ALGORITHM("SEC-102", "Unsupported algorithm: {0}"),
    UNSUPPORTED_DIGEST("SEC-103", "Unsupported digest: {0}"),
    INVALID_KEY("SEC-104", "Invalid key: {0}"),
    EMPTY_CERT_CHAIN("SEC-105", "Empty certificate chain"),
    EMPTY_TRUST_STORE("SEC-106", "Empty trust store"),
    CERT_PATH_VALIDATION_FAILED("SEC-107", "Certificate path validation failed"),
    ERROR_READING_MANIFEST("SEC-108", "Error reading manifest: {0}"),
    NOT_A_SIGNED_ENTRY("SEC-109", "{0} is not a signed entry"),
    INCORRECT_DIGEST("SEC-110", "Incorrect digest for {0}"),
    INCOMPLETE_SETUP("SEC-111", "Incomplete setup of JAR verifier"),
    MISSING_ATTRIBUTES("SEC-112", "Missing attributes for entry {0} in MANIFEST.MF"),
    MISSING_DIGEST("SEC-113", "Missing digest for {0}"),
    MISSING_KEY_FILE("SEC-114", "Missing key file"),
    EXTRA_KEY_FILE("SEC-115", "Extra key file"),
    VERIFIER_INIT_FAILED("SEC-116", "Jar verifier initialization failed: {0}");

    private final String code;
    private final String format;

    SecurityMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}