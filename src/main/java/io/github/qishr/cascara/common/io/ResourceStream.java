package io.github.qishr.cascara.common.io;

import java.io.InputStream;

public final class ResourceStream {
    public final InputStream stream;
    public final String mimeType; // null for non-HTTP

    public ResourceStream(InputStream stream, String mimeType) {
        this.stream = stream;
        this.mimeType = mimeType;
    }
}
