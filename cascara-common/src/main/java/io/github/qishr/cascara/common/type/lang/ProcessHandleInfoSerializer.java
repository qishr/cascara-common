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

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.lang.generic.GenericAstScalar;
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.type.TypeSerializer;

public class ProcessHandleInfoSerializer extends AbstractTypeDescriptor<ProcessHandle.Info> implements TypeSerializer<ProcessHandle.Info> {

    public ProcessHandleInfoSerializer() {
        super(ProcessHandle.Info.class, PrimitiveType.OBJECT);
    }

    @Override
    public GenericAstMap serialize(ProcessHandle.Info jvmInstance, Serializer.Context ctx) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }

        GenericAstMap node = new GenericAstMap();
        jvmInstance.command().ifPresent(c -> node.put("command", c));
        jvmInstance.arguments().ifPresent(args -> {
            GenericAstSequence seq = new GenericAstSequence();
            for (String arg : args) {
                seq.add(new GenericAstScalar(arg));
            }
            node.put("arguments", seq);
        });
        jvmInstance.startInstant().ifPresent(inst -> node.put("startInstant", inst.toString()));
        jvmInstance.totalCpuDuration().ifPresent(dur -> node.put("totalCpuDuration", dur.toString()));
        jvmInstance.user().ifPresent(u -> node.put("user", u));

        return node;
    }

    @Override
    public ProcessHandle.Info deserialize(AstNode astNode, Type targetType, Serializer.Context ctx) throws SerializerException {
        if (!(astNode instanceof MapAstNode map)) {
            return null;
        }

        String command = map.getString("command");
        // Reconstruct via a lightweight synthetic holder implementing ProcessHandle.Info
        return new SerializedProcessHandleInfo(command, map);
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }

    public static class SerializedProcessHandleInfo implements ProcessHandle.Info {
        private final String command;
        // Other cached fields mapped from the AST map...

        public SerializedProcessHandleInfo(String command, MapAstNode<?, ?, ?> map) {
            this.command = command;
        }

        @Override public Optional<String> command() { return Optional.ofNullable(command); }
        @Override public Optional<String[]> arguments() { return Optional.empty(); }
        @Override public Optional<Instant> startInstant() { return Optional.empty(); }
        @Override public Optional<Duration> totalCpuDuration() { return Optional.empty(); }
        @Override public Optional<String> user() { return Optional.empty(); }

        @Override
        public Optional<String> commandLine() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'commandLine'");
        }
    }
}