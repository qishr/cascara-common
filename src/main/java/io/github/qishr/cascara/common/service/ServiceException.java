package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.util.CascaraRuntimeException;

public class ServiceException extends CascaraRuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable e) {
        super(message, e);
    }
}
