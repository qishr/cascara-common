package io.github.qishr.cascara.common.io.provider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.qishr.cascara.common.io.ResourceStream;

public class FileResourceProvider implements ResourceProvider {

    @Override
    public ResourceStream getResourceAsStream(URI uri) throws IOException {
        try {
            Path path = Path.of(uri);
            return new ResourceStream(Files.newInputStream(path), null);
        } catch (java.lang.IllegalArgumentException e) {
            throw new IOException("Invalid URI: " + uri.toString());
        }
    }

}
