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

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;

import java.util.SimpleTimeZone;

/// SimpleTimeZone Scalar Descriptor preserving raw offset and time zone ID.
public class SimpleTimeZoneDescriptor extends AbstractScalarDescriptor<SimpleTimeZone> {

    public SimpleTimeZoneDescriptor() {
        super(SimpleTimeZone.class, PrimitiveType.STRING);
    }

    @Override
    public SimpleTimeZone toJvmType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Format expected: "ID:rawOffsetInMillis" or fallback to parsing ID with default offset
        String[] parts = text.split(":");
        String id = parts[0];
        int rawOffset = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        return new SimpleTimeZone(rawOffset, id);
    }

    @Override
    public String toPrimitive(SimpleTimeZone jvmInstance) {
        if (jvmInstance == null) {
            return null;
        }
        // Encode ID and raw offset to ensure precise reconstruction of SimpleTimeZone
        return jvmInstance.getID() + ":" + jvmInstance.getRawOffset();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            String[] parts = text.split(":");
            if (parts.length > 1) {
                Integer.parseInt(parts[1]);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}