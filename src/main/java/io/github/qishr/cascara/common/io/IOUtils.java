package io.github.qishr.cascara.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.content.ContentType;
import io.github.qishr.cascara.common.io.provider.CascaraResourceProvider;
import io.github.qishr.cascara.common.io.provider.FileResourceProvider;
import io.github.qishr.cascara.common.io.provider.HttpResourceProvider;
import io.github.qishr.cascara.common.io.provider.ResResourceProvider;
import io.github.qishr.cascara.common.io.provider.ResourceProvider;

public class IOUtils {

    private static final Map<UriScheme, ResourceProvider> providers = new HashMap<>();

    static {
        // TODO: Add ZIP, JAR, etc
        providers.put(UriScheme.CASCARA, new CascaraResourceProvider());
        providers.put(UriScheme.FILE, new FileResourceProvider());
        providers.put(UriScheme.HTTP, new HttpResourceProvider());
        providers.put(UriScheme.HTTPS, new HttpResourceProvider());
        providers.put(UriScheme.RES, new ResResourceProvider());
    }

    public static void setResourceProvider(UriScheme uriScheme, ResourceProvider provider) {
        providers.put(uriScheme, provider);
    }

    public static InputStream getContentAsStream(URI uri) throws IOException {
        return getResourceAsStream(uri).stream;
    }

    public static ResourceContent getResource(URI uri) throws IOException {
        ResourceStream res = getResourceAsStream(uri);
        String content = new String(res.stream.readAllBytes(), StandardCharsets.UTF_8);
        ContentType ct = null;

        // TODO: Might want to have all of content type code in common...

        // try {
        //     if (res.mimeType != null) {
        //         // HTTP(S) case — use MIME header
        //         ct = contentTypeService.findContentType(res.mimeType);
        //     } else {
        //         // file:// or res:// — infer from filename
        //         ct = contentTypeService.findContentTypeForFile(uri.toString());
        //     }
        // } catch (Exception _) {
        // }

        if (ct == null && res.mimeType != null) {
            ct = new ContentType().withMimeType(res.mimeType);
        }

        return new ResourceContent(content, ct);
    }

    public static ResourceStream getResourceAsStream(URI uri) throws IOException {
        UriScheme scheme = UriScheme.of(uri);
        if (scheme == null || scheme == UriScheme.UNKNOWN) {
            throw new IOException("Unknown URI scheme: " + uri);
        }

        ResourceProvider provider = providers.get(scheme);
        if (provider == null) {
            throw new IOException("No ResourceProvider available for " + uri);
        }

        return provider.getResourceAsStream(uri);
    }
}
