package io.github.qishr.cascara.common.io.provider;

import java.io.IOException;
import java.net.URI;

import io.github.qishr.cascara.common.io.ResourceStream;

public class CascaraResourceProvider implements ResourceProvider {

    @Override
    public ResourceStream getResourceAsStream(URI uri) throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'getContentAsStream'");
    }

}
