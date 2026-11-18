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

package io.github.qishr.cascara.common.type.cascara;

import java.lang.reflect.Type;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.i18n.DetailedMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.lang.generic.GenericAstScalar;
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.type.TypeSerializer;

public class DetailedMessageSerializer extends AbstractTypeDescriptor<DetailedMessage> implements TypeSerializer<DetailedMessage> {
    private LocalizableMessageDescriptor lmd = new LocalizableMessageDescriptor();

    public DetailedMessageSerializer() {
        super(DetailedMessage.class, PrimitiveType.OBJECT);
    }

    @Override
    public GenericAstMap serialize(DetailedMessage jvmInstance, Type jvmType, Serializer.Context ctx) throws SerializerException {
        GenericAstSequence details = new GenericAstSequence();
        for (Object detail : jvmInstance.getDetails()) {
            details.add(new GenericAstScalar(detail));
        }
        String message = lmd.toPrimitive(jvmInstance.getMessage());
        return new GenericAstMap()
            .put("message", message)
            .put("details", details);
    }

    @Override
    public DetailedMessage deserialize(AstNode astNode, Type targetType, Serializer.Context ctx) throws SerializerException {
        if (astNode instanceof MapAstNode map) {
            SequenceAstNode<?> detailsSeq = map.getSequence("details");
            Object[] details = new Object[detailsSeq.size()];
            for (int i = 0; i < detailsSeq.size(); i++) {
                Object detail = detailsSeq.get(i);
                details[i] = detail;
            }
            String msg = map.getString("message");
            LocalizableMessage message = lmd.toJvmType(msg);
            return new DetailedMessage(message, details);
        }
        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}
