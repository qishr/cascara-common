package io.github.qishr.cascara.common.spi;

import java.util.ServiceLoader;

import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.processor.Parser;

public class ParserFactory {

    @Nullable
    public Parser<?, ?> create(String type) throws ServiceException {
        // We call a generic helper that "captures" the Parser type
        return findParser(ServiceLoader.load(Parser.class), type);
    }

    private <P extends Parser<?, ?>> P findParser(ServiceLoader<P> loader, String type) throws ServiceException {
        if (loader == null) {
            throw new ServiceException("ServiceLoader failed for Parser");
        }
        for (ServiceLoader.Provider<P> provider : loader.stream().toList()) {
            try {
                P parser = provider.get();
                ContentType contentType = parser.getContentType();

                if (contentType.getMimeTypes().contains(type)
                    || contentType.getSuffixes().contains(type)
                    || contentType.getName().equalsIgnoreCase(type)) {
                    return parser;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw new ServiceException(e.getMessage(), e);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new ServiceException(e.getMessage(), e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceException(e.getMessage(), e);
            }
        }
        throw new ServiceException("No parser found for " + type);
    }
}
