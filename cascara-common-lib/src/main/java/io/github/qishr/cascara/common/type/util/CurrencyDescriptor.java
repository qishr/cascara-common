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

package io.github.qishr.cascara.common.type.util;

import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.type.PrimitiveType;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;

import java.util.Currency;

/// Scalar descriptor for java.util.Currency mapping to/from its ISO 4217 currency code string.
public class CurrencyDescriptor extends AbstractScalarDescriptor<Currency> {

    public CurrencyDescriptor() {
        super(Currency.class, PrimitiveType.STRING);
    }

    @Override
    public Currency toJvmType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Currency.getInstance(text.trim());
    }

    @Override
    public String toPrimitive(Currency jvmInstance) {
        if (jvmInstance == null) {
            return null;
        }
        return jvmInstance.getCurrencyCode();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            Currency.getInstance(text.trim());
            return true;
        } catch (IllegalArgumentException e) {
            if (collector != null) {
                collector.error(LangMessage.WRONG_FORMAT, text, "ISO 4217 currency code");
            }
            return false;
        }
    }
}