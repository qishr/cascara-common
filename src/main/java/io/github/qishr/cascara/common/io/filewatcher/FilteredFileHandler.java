package io.github.qishr.cascara.common.io.filewatcher;

import java.nio.file.Path;
import java.util.Set;

public class FilteredFileHandler implements FileChangeHandler {
    private final Set<String> extensions;
    private final FileChangeHandler delegate;

    public FilteredFileHandler(FileChangeHandler delegate, String... extensions) {
        this.delegate = delegate;
        this.extensions = Set.of(extensions);
    }

    @Override
    public void handle(FileChangeType type, Path path) {
        String fileName = path.getFileName().toString();
        boolean matches = extensions.stream()
            .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));

        if (matches) {
            delegate.handle(type, path);
        }
    }
}