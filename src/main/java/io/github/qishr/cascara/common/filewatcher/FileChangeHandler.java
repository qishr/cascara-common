package io.github.qishr.cascara.common.filewatcher;

import java.nio.file.Path;

@FunctionalInterface
public interface FileChangeHandler {
    void handle(FileChangeType type, Path path);
}