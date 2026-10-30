package io.github.qishr.cascara.common.security.asn1.types.string;

import java.io.IOException;
import java.util.Arrays;

import io.github.qishr.cascara.common.security.asn1.ASN1OutputStream;
import io.github.qishr.cascara.common.security.asn1.ASN1ParseException;
import io.github.qishr.cascara.common.security.asn1.ASN1Parser;
import io.github.qishr.cascara.common.security.asn1.ASN1Serializer;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Encoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;

// 19
public final class ASN1PrintableString extends ASN1String<byte[]> {

    // public static final ASN1Tag<ASN1PrintableString> TAG =
    //     register(
    //         ASN1TagClass.UNIVERSAL,
    //         ASN1Encoding.PRIMITIVE,
    //         19,
    //         ASN1PrintableString.Parser::new,
    //         ASN1PrintableString.Serializer::new
    //     );

    public ASN1PrintableString(byte[] bytes) {
        super(ASN1Tag.PRINTABLE_STRING, bytes);
    }

    public ASN1PrintableString(ASN1Tag<?> tag, byte[] bytes) {
        super(tag, bytes);
    }

    @Override
    public byte[] getValue() {
        return Arrays.copyOf(valueBytes, valueBytes.length);
    }

    @Override
    protected String valueString() {
        return Arrays.toString(valueBytes);
    }

    @Override
    public int length() {
        return valueBytes.length;
    }

    @Override
    public String toString() {
        String str = new String(valueBytes);
        return this.getClass().getSimpleName() + " [" + str + "]";
    }

    public static class Parser extends ASN1Parser<ASN1PrintableString> {

        public Parser(ASN1Decoder decoder) {
            super(decoder);
        }

        @Override
        public ASN1PrintableString parse(ASN1Tag<ASN1PrintableString> asn1Tag, byte[] value) throws ASN1ParseException {
            return new ASN1PrintableString(asn1Tag, value);
        }
    }

    public static class Serializer extends ASN1Serializer<ASN1PrintableString> {

        public Serializer(final ASN1Encoder encoder) {
            super(encoder);
        }

        @Override
        public int serializedLength(final ASN1PrintableString asn1Object) throws IOException {
            return asn1Object.valueBytes.length;
        }

        @Override
        public void serialize(final ASN1PrintableString asn1Object, final ASN1OutputStream stream) throws IOException {
            stream.write(asn1Object.valueBytes);
        }
    }
}