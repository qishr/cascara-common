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
package io.github.qishr.cascara.common.security.asn1.types.primitive;

import java.io.IOException;
import io.github.qishr.cascara.common.security.asn1.ASN1OutputStream;
import io.github.qishr.cascara.common.security.asn1.ASN1Parser;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.ASN1Serializer;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Encoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Primitive;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.util.Checks;

public class ASN1Boolean extends ASN1PrimitiveValue<Boolean> {

    private boolean value;

    public ASN1Boolean(boolean value) {
        super(ASN1Tag.BOOLEAN);
        this.value = value;
    }

    private ASN1Boolean(byte[] valueBytes, boolean value) {
        super(ASN1Tag.BOOLEAN, valueBytes);
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return this;
    }

    @Override
    protected int valueHash() {
        return value ? 1231 : 1237;
    }

    public static class Parser extends ASN1Parser<ASN1Boolean> {
        public Parser(ASN1Decoder decoder) {
            super(decoder);
        }

        @Override
        public ASN1Boolean parse(ASN1Tag<ASN1Boolean> asn1Tag, byte[] value) {
            Checks.checkState(value.length == 1, "Value of ASN1Boolean should have length 1, but was %s", value.length);
            return new ASN1Boolean(value, value[0] != 0x0);
        }
    }

    public static class Serializer extends ASN1Serializer<ASN1Boolean> {
        public Serializer(final ASN1Encoder encoder) {
            super(encoder);
        }

        @Override
        public int serializedLength(final ASN1Boolean asn1Object) {
            return 1;
        }

        @Override
        public void serialize(final ASN1Boolean asn1Object, final ASN1OutputStream stream) throws IOException {
            stream.write(asn1Object.value ? 0x01 : 0x00);
        }
    }

}
