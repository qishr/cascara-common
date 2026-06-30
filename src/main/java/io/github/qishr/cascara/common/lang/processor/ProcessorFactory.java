package io.github.qishr.cascara.common.lang.processor;

import io.github.qishr.cascara.common.service.AbstractServiceProviderFactory;
import io.github.qishr.cascara.common.service.CapabilityQueries;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;

public class ProcessorFactory extends AbstractServiceProviderFactory {
    public static final String CONTENT_TYPE = "contentType";

    public ProcessorFactory() {
        super();
    }

    public ProcessorFactory(ServiceProviderLayer layer) {
        super(layer);
    }

    public Tokenizer<?> createTokenizer(String contentType) throws ServiceException {
        return createServiceProvider(
            Tokenizer.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public AstParser<?, ?> createAstParser(String contentType) throws ServiceException {
        return createServiceProvider(
            AstParser.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public PullParser createPullParser(String contentType) throws ServiceException {
        return createServiceProvider(
            PullParser.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public PushParser createPushParser(String contentType) throws ServiceException {
        return createServiceProvider(
            PushParser.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public Serializer<?> createSerializer(String contentType) throws ServiceException {
        return createServiceProvider(
            Serializer.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }
}
