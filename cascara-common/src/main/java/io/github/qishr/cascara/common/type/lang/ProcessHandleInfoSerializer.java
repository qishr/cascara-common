package io.github.qishr.cascara.common.type.lang;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.type.TypeSerializer;

public class ProcessHandleInfoSerializer extends AbstractTypeDescriptor<ProcessHandle.Info> implements TypeSerializer<ProcessHandle.Info> {

    public ProcessHandleInfoSerializer() {
        super(ProcessHandle.Info.class, PrimitiveType.OBJECT);
    }

    @Override
    public PlainMapNode serialize(ProcessHandle.Info jvmInstance) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }

        PlainMapNode node = new PlainMapNode();
        jvmInstance.command().ifPresent(c -> node.put("command", c));
        jvmInstance.arguments().ifPresent(args -> {
            PlainSequenceNode seq = new PlainSequenceNode();
            for (String arg : args) {
                seq.add(new PlainScalarNode(arg));
            }
            node.put("arguments", seq);
        });
        jvmInstance.startInstant().ifPresent(inst -> node.put("startInstant", inst.toString()));
        jvmInstance.totalCpuDuration().ifPresent(dur -> node.put("totalCpuDuration", dur.toString()));
        jvmInstance.user().ifPresent(u -> node.put("user", u));

        return node;
    }

    @Override
    public ProcessHandle.Info deserialize(AstNode astNode, Type targetType) throws SerializerException {
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