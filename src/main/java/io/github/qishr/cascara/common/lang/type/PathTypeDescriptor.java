package io.github.qishr.cascara.common.lang.type;

import java.nio.file.Path;

public class PathTypeDescriptor extends AbstractScalarDescriptor<Path> {
    public PathTypeDescriptor() {
        super(Path.class, "string", "path");
    }

    @Override
    public Path toJvmType(String text) {
        return Path.of(text);
    }

    @Override
    public Primitive toPrimitive(Path value) {
        return Primitive.of(value.toString());
    }
}
