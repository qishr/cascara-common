package io.github.qishr.cascara.common.type.lang;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Type;

public class ConfigurationSerializer extends AbstractTypeDescriptor<Configuration> implements TypeSerializer<Configuration> {

    public ConfigurationSerializer() {
        super(Configuration.class, PrimitiveType.OBJECT);
    }

    @Override
    public PlainMapNode serialize(Configuration jvmInstance) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }

        PlainMapNode node = new PlainMapNode();

        // Serialize parent configuration names/presence
        PlainSequenceNode parentsSeq = new PlainSequenceNode();
        for (Configuration parent : jvmInstance.parents()) {
            parentsSeq.add(new PlainScalarNode(parent.toString()));
        }
        node.put("parents", parentsSeq);

        // Serialize resolved module names within this configuration
        PlainSequenceNode modulesSeq = new PlainSequenceNode();
        for (ResolvedModule mod : jvmInstance.modules()) {
            modulesSeq.add(new PlainScalarNode(mod.reference().descriptor().name()));
        }
        node.put("modules", modulesSeq);

        return node;
    }

    @Override
    public Configuration deserialize(AstNode astNode, Type targetType) throws SerializerException {
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