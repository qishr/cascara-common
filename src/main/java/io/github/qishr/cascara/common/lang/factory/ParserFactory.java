package io.github.qishr.cascara.common.lang.factory;

import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.processor.Parser;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.util.ContentType;

import java.util.ServiceLoader;


public class ParserFactory {

    @Nullable
    public Parser<?, ?> create(String type) throws ServiceException {
        // We call a generic helper that "captures" the Parser type
        return findParser(ServiceLoader.load(Parser.class), type);
    }

    private <P extends Parser<?, ?>> P findParser(ServiceLoader<P> loader, String type) throws ServiceException {
        if (loader == null) {
            // THis should never happen
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE, "Parser");
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
                throw new ServiceException(e, GenericDiagnosticCode.NPE, e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new ServiceException(e, GenericDiagnosticCode.RUNTIME_EXCEPTION, e.getMessage(), e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceException(e, GenericDiagnosticCode.EXCEPTION, e.getMessage(), e);
            }
        }
        throw new ServiceException(ServiceDiagnosticCode.NO_PROVIDER_REGISTERED_FOR, "Parser", type);
    }
}

