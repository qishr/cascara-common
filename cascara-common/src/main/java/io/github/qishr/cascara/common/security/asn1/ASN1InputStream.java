/*
 *    Copyright 2016 Jeroen van Erp <jeroen@hierynomus.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.qishr.cascara.common.security.asn1;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;

public class ASN1InputStream extends FilterInputStream implements Iterable<ASN1Object> {
    private static final Reporter reporter = GlobalReporter.forClass(ASN1InputStream.class);
    private final ASN1Decoder decoder;

    public ASN1InputStream(ASN1Decoder decoder, InputStream wrapped) {
        super(wrapped);
        this.decoder = decoder;
    }

    public ASN1InputStream(ASN1Decoder decoder, byte[] value) {
        super(new ByteArrayInputStream(value));
        this.decoder = decoder;
    }

    public <T extends ASN1Object> T readObject() {
        try {
            ASN1Tag tag = decoder.readTag(this);
            reporter.trace("Read ASN.1 tag " + tag);
            int length = decoder.readLength(this);
            reporter.trace("Read ASN.1 object length: {}", length);
            byte[] value = decoder.readValue(length, this);

            //noinspection unchecked
            ASN1Object parse = tag.newParser(decoder).parse(tag, value);
            reporter.debug("Read ASN.1 object: " + parse);
            return (T) parse;
        } catch (ASN1ParseException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ASN1ParseException(e, "Cannot parse ASN.1 object from stream");
        }
    }

    public byte[] readValue(int length) {
        return decoder.readValue(length, this);
    }

    public Iterator<ASN1Object> iterator() {
        return new Iterator<ASN1Object>() {
            @Override
            public boolean hasNext() {
                try {
                    return available() > 0;
                } catch (IOException e) {
                    // Or throw an exception?
                    return false;
                }
            }

            @Override
            public ASN1Object next() {
                // try {
                    return readObject();
                // } catch (Exception e) {
                //     throw new NoSuchElementException(e.getMessage());
                // }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove not supported on ASN.1 InputStream iterator");
            }
        };
    }

    public ASN1Tag readTag() {
        return decoder.readTag(this);
    }

    public int readLength() {
        return decoder.readLength(this);
    }
}
