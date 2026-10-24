package io.github.qishr.cascara.common.security.asn1.types.other;

import java.io.IOException;
import java.util.Arrays;

import io.github.qishr.cascara.common.security.asn1.ASN1OutputStream;
import io.github.qishr.cascara.common.security.asn1.ASN1ParseException;
import io.github.qishr.cascara.common.security.asn1.ASN1Parser;
import io.github.qishr.cascara.common.security.asn1.ASN1Serializer;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Encoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Encoding;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Primitive;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.ASN1TagClass;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1OctetString;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1String;

public final class ASN1UTCTime extends ASN1String {

    // public static final ASN1Tag<ASN1UTCTime> TAG =
    //     ASN1Tag.register(
    //         ASN1TagClass.UNIVERSAL,
    //         ASN1Encoding.PRIMITIVE,
    //         23,
    //         Parser::new,
    //         Serializer::new
    //     );

    // public ASN1UTCTime(String value) {
    //     super(TAG, value);
    // }




    public ASN1UTCTime(byte[] bytes) {
        super(ASN1Tag.UTC_TIME, bytes);
    }

    public ASN1UTCTime(ASN1Tag<?> tag, byte[] bytes) {
        super(tag, bytes);
    }

    @Override
    public byte[] getValue() {
        return Arrays.copyOf(valueBytes, valueBytes.length);
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return this;
    }

    @Override
    protected String valueString() {
        return Arrays.toString(valueBytes);
    }

    @Override
    public int length() {
        return valueBytes.length;
    }

    public static class Parser extends ASN1Parser<ASN1UTCTime> {

        public Parser(ASN1Decoder decoder) {
            super(decoder);
        }

        @Override
        public ASN1UTCTime parse(ASN1Tag<ASN1UTCTime> asn1Tag, byte[] value) throws ASN1ParseException {
            return new ASN1UTCTime(asn1Tag, value);
        }
    }

    public static class Serializer extends ASN1Serializer<ASN1UTCTime> {

        public Serializer(final ASN1Encoder encoder) {
            super(encoder);
        }

        @Override
        public int serializedLength(final ASN1UTCTime asn1Object) throws IOException {
            return asn1Object.valueBytes.length;
        }

        @Override
        public void serialize(final ASN1UTCTime asn1Object, final ASN1OutputStream stream) throws IOException {
            stream.write(asn1Object.valueBytes);
        }
    }

}