package io.github.qishr.cascara.common.spi;

import java.util.ServiceLoader;

import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.processor.AstConverter;

public class AstConverterFactory {

    @Nullable
    public AstConverter<?> create(String type) throws ServiceException {
        return findEmitter(ServiceLoader.load(AstConverter.class), type);
    }

    private <T extends AstConverter<?>> T findEmitter(ServiceLoader<T> loader, String type) throws ServiceException {
        for (ServiceLoader.Provider<T> provider : loader.stream().toList()) {
            try {
                T converter = provider.get();
                ContentType contentType = converter.getContentType();

                if (contentType.getMimeTypes().contains(type)
                    || contentType.getSuffixes().contains(type)
                    || contentType.getName().equalsIgnoreCase(type)) {
                    return converter;
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
        return null;
    }
}

// public class ParserFactory {

//     @Nullable
//     public Parser<?,?> getParserFor(String type) {
//         var providers = ServiceLoader.load(Parser.class).stream().toList();
//         for (Provider<Parser> provider : providers) {
//             try {
//                 Parser<?,?> parser = provider.get();
//                 ContentType contentType = parser.getContentType();
//                 if (contentType.getMimeTypes().contains(type)
//                     || contentType.getSuffixes().contains(type)
//                     || contentType.getName().equalsIgnoreCase(type)) {

//                     //
//                     return parser;
//                 }
//             } catch (RuntimeException e) {
//             } catch (Exception e) {
//             }
//         }
//         return null;
//     }
// }
