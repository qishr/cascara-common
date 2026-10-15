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

import java.time.Instant;
import java.util.Date;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

/// java.util.Date Descriptor (mapping to ISO-8601 string or epoch millis—let's use ISO-8601 string format via Instant for consistency)
public class DateDescriptor extends AbstractScalarDescriptor<Date> {
    public DateDescriptor() {
        super(Date.class, PrimitiveType.STRING);
    }

    @Override
    public Date toJvmType(String text) {
        if (text == null) return null;
        return Date.from(Instant.parse(text));
    }

    @Override
    public String toPrimitive(Date jvmInstance) {
        return jvmInstance.toInstant().toString();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Instant.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
