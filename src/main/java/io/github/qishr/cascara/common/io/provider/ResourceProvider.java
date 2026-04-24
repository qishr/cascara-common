package io.github.qishr.cascara.common.io.provider;

import java.io.IOException;
import java.net.URI;

import io.github.qishr.cascara.common.io.ResourceStream;

public interface ResourceProvider {
    ResourceStream getResourceAsStream(URI uri) throws IOException;
}
