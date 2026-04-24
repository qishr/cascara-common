package io.github.qishr.cascara.common.io.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import io.github.qishr.cascara.common.io.ResourceStream;
import io.github.qishr.cascara.common.util.ResourceUtil;

public class ResResourceProvider implements ResourceProvider {

    @Override
    public ResourceStream getResourceAsStream(URI uri) throws IOException {
        String path = uri.getSchemeSpecificPart().replace("//", "");
        InputStream is = ResourceUtil.getResourceStream(path);
        if (is == null) throw new IOException("Resource not found: " + path);
        return new ResourceStream(is, null);
    }

}
