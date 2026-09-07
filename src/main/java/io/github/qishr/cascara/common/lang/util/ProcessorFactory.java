// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.lang.util;

import io.github.qishr.cascara.common.annotation.Beta;
import io.github.qishr.cascara.common.lang.processor.AstParser;
import io.github.qishr.cascara.common.lang.processor.PullParser;
import io.github.qishr.cascara.common.lang.processor.PushParser;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.processor.Tokenizer;
import io.github.qishr.cascara.common.service.AbstractServiceProviderFactory;
import io.github.qishr.cascara.common.service.CapabilityQueries;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.util.ContentType;

@Beta
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

    public Tokenizer<?> createTokenizer(ContentType contentType) throws ServiceException {
        return createServiceProvider(
            Tokenizer.class,
            CapabilityQueries.supportsContentType(contentType)
        );
    }

    public AstParser<?,?,?> createAstParser(String contentType) throws ServiceException {
        return createServiceProvider(
            AstParser.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public AstParser<?,?,?> createAstParser(ContentType contentType) throws ServiceException {
        return createServiceProvider(
            AstParser.class,
            CapabilityQueries.supportsContentType(contentType)
        );
    }

    public PullParser createPullParser(String contentType) throws ServiceException {
        return createServiceProvider(
            PullParser.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public PullParser createPullParser(ContentType contentType) throws ServiceException {
        return createServiceProvider(
            PullParser.class,
            CapabilityQueries.supportsContentType(contentType)
        );
    }

    public PushParser createPushParser(String contentType) throws ServiceException {
        return createServiceProvider(
            PushParser.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public PushParser createPushParser(ContentType contentType) throws ServiceException {
        return createServiceProvider(
            PushParser.class,
            CapabilityQueries.supportsContentType( contentType)
        );
    }

    public Serializer<?> createSerializer(String contentType) throws ServiceException {
        return createServiceProvider(
            Serializer.class,
            CapabilityQueries.hasExactValue(CONTENT_TYPE, contentType)
        );
    }

    public Serializer<?> createSerializer(ContentType contentType) throws ServiceException {
        return createServiceProvider(
            Serializer.class,
            CapabilityQueries.supportsContentType(contentType)
        );
    }
}
