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


package io.github.qishr.cascara.common.diagnostic;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class AbstractLocalizableException extends Exception implements LocalizableException {

    private static volatile DiagnosticLocalizer localizer = DiagnosticLocalizer.DEFAULT;

    private final DiagnosticCode code;
    private final Object[] details;

    public static DiagnosticLocalizer getLocalizer() {
        return localizer;
    }

    public static void setLocalizer(DiagnosticLocalizer customLocalizer) {
        localizer = customLocalizer != null ? customLocalizer : DiagnosticLocalizer.DEFAULT;
    }

    public AbstractLocalizableException(DiagnosticCode code, Object... details) {
        super(format(code, details));
        this.code = code;
        this.details = details != null ? details : new Object[0];
    }

    public AbstractLocalizableException(Throwable cause, DiagnosticCode code, Object... details) {
        super(format(code, details));
        this.code = code;
        this.details = details != null ? details : new Object[0];
    }

    /// Returns a diagnostic error code for the error message.
    @Override
	public DiagnosticCode getCode() {
		return code;
	}

    /// Returns the details, if any, to be used in formatting the error message.
    @Override
	public Object[] getDetails() {
		return details;
	}

    /// Returns a localized, formatted error message.
    @Override
    public String getLocalizedMessage() {
        try {
            return localizer.format(code, details);
        } catch (IllegalArgumentException e) {
            return String.format(DiagnosticLocalizer.FORMATTING_ERROR, code.getCode(), code.getMessage());
        }
    }

    /// Returns a localized, formatted error message.
    @Override
    public String getMessage() {
        try {
            return DiagnosticLocalizer.DEFAULT.format(code, details);
        } catch (IllegalArgumentException e) {
            return String.format(DiagnosticLocalizer.FORMATTING_ERROR, code.getCode(), code.getMessage());
        }
    }

    /// Formats a [DiagnosticCode]'s message without localizing it.
    private static String format(DiagnosticCode code, Object... details) {
        return DiagnosticLocalizer.DEFAULT.format(code, details);
    }
}
