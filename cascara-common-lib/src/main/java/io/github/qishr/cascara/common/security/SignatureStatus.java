package io.github.qishr.cascara.common.security;

public enum SignatureStatus {
    VALID,
    NOT_SIGNED,
    INVALID_SIGNATURE,
    UNTRUSTED_SIGNER,
    CERTIFICATE_EXPIRED,
    CERTIFICATE_REVOKED,
    TIMESTAMP_INVALID,
    UNSUPPORTED_ALGORITHM
    // NO_ALGORITHM ?
}