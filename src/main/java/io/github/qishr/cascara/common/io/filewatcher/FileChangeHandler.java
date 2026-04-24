package io.github.qishr.cascara.common.io.filewatcher;

import java.nio.file.Path;

@FunctionalInterface
public interface FileChangeHandler {
    void handle(FileChangeType type, Path path);
}