package io.github.qishr.cascara.common.util;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import io.github.qishr.cascara.common.lang.processor.AstParser;

public class ContentTypes {
    private static Set<ContentType> contentTypes;

    public static ContentType find(String type) {
        if (contentTypes == null) enumerateContentTypes();

        for (ContentType contentType : contentTypes) {
            if (contentType.getMimeTypes().contains(type)
                || contentType.getSuffixes().contains(type)
                || contentType.getName().equalsIgnoreCase(type)) {
                return contentType;
            }
        }
        return null;
    }

    private static void enumerateContentTypes() {
        enumerateContentTypes(ServiceLoader.load(AstParser.class));
    }

    private static <P extends AstParser<?, ?>> void enumerateContentTypes(ServiceLoader<P> loader) {
        contentTypes = new HashSet<>();
        for (ServiceLoader.Provider<P> provider : loader.stream().toList()) {
            try {
                P parser = provider.get();
                ContentType contentType = parser.getContentType();
                contentTypes.add(contentType);
            } catch (Exception e) {
                // Ignore load failures
            }
        }
    }
}
