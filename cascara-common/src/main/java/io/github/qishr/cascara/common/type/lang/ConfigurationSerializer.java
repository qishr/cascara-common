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

package io.github.qishr.cascara.common.type.lang;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.lang.generic.GenericAstScalar;
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.processor.Serializer;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Type;

public class ConfigurationSerializer extends AbstractTypeDescriptor<Configuration> implements TypeSerializer<Configuration> {

    public ConfigurationSerializer() {
        super(Configuration.class, PrimitiveType.OBJECT);
    }

    @Override
    public GenericAstMap serialize(Configuration jvmInstance, Type jvmType, Serializer.Context ctx) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }

        GenericAstMap node = new GenericAstMap();

        // Serialize parent configuration names/presence
        GenericAstSequence parentsSeq = new GenericAstSequence();
        for (Configuration parent : jvmInstance.parents()) {
            parentsSeq.add(new GenericAstScalar(parent.toString()));
        }
        node.put("parents", parentsSeq);

        // Serialize resolved module names within this configuration
        GenericAstSequence modulesSeq = new GenericAstSequence();
        for (ResolvedModule mod : jvmInstance.modules()) {
            modulesSeq.add(new GenericAstScalar(mod.reference().descriptor().name()));
        }
        node.put("modules", modulesSeq);

        return node;
    }

    @Override
    public Configuration deserialize(AstNode astNode, Type jvmType, Serializer.Context ctx) throws SerializerException {
        // Configurations are tied to live JVM module layers and cannot be directly
        // reconstructed from scratch without the original module finder/layer context.
        // Return null or a lightweight proxy/holder if needed.
        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}