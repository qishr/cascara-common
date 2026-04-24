package io.github.qishr.cascara.common.io;

import java.io.IOException;
import java.net.URI;

public interface StreamLoader {
    ResourceStream getContent(URI uri) throws IOException;
}
