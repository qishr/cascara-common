package io.github.qishr.cascara.common.type.util;

import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.type.PrimitiveType;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.type.AbstractScalarDescriptor;

import java.util.Currency;

/// Scalar descriptor for java.util.Currency mapping to/from its ISO 4217 currency code string.
public class CurrencyDescriptor extends AbstractScalarDescriptor<Currency> {

    public CurrencyDescriptor() {
        super(Currency.class, PrimitiveType.STRING);
    }

    @Override
    public Currency toJvmType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Currency.getInstance(text.trim());
    }

    @Override
    public String toPrimitive(Currency jvmInstance) {
        if (jvmInstance == null) {
            return null;
        }
        return jvmInstance.getCurrencyCode();
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            Currency.getInstance(text.trim());
            return true;
        } catch (IllegalArgumentException e) {
            if (collector != null) {
                collector.error(LangMessage.WRONG_FORMAT, text, "ISO 4217 currency code");
            }
            return false;
        }
    }
}