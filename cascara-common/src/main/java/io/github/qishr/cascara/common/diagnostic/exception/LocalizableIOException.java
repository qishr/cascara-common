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


package io.github.qishr.cascara.common.diagnostic.exception;

import java.io.IOException;

import io.github.qishr.cascara.common.diagnostic.DiagnosticLocalizer;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class LocalizableIOException extends IOException implements LocalizableException {

    private final DiagnosticMessage diagnosticMessage;
    private final Object[] details;

    public LocalizableIOException(DiagnosticMessage code, Object... details) {
        this(null, code, details);
    }

    public LocalizableIOException(Throwable cause, DiagnosticMessage code, Object... details) {
        super(cause);
        this.diagnosticMessage = code;
        this.details = details;
    }

    /// Returns a diagnostic error code for the error message.
    @Override
	public DiagnosticMessage getDiagnosticMessage() {
		return diagnosticMessage;
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
            return AbstractLocalizableException.getLocalizer().format(diagnosticMessage, details);
        } catch (IllegalArgumentException e) {
            return String.format(DiagnosticLocalizer.FORMATTING_ERROR, diagnosticMessage.getCode(), diagnosticMessage.getFormat());
        }
    }

    /// Returns a localized, formatted error message.
    @Override
    public String getMessage() {
        try {
            return DiagnosticLocalizer.DEFAULT.format(diagnosticMessage, details);
        } catch (IllegalArgumentException e) {
            return String.format(DiagnosticLocalizer.FORMATTING_ERROR, diagnosticMessage.getCode(), diagnosticMessage.getFormat());
        }
    }
}
