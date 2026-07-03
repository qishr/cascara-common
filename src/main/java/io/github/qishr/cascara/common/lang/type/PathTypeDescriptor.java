package io.github.qishr.cascara.common.lang.type;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import io.github.qishr.cascara.common.diagnostic.Reporter;

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

    @Override
    public boolean validate(String text, Reporter collector) {
        try {
            Path.of(text);
            return true;
        } catch (InvalidPathException e) {
            formatError(text, collector);
            return false;
        }
    }
}
