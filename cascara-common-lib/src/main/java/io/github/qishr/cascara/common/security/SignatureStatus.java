package io.github.qishr.cascara.common.security;

public enum SignatureStatus {
    VALID,
    NOT_SIGNED,
    INVALID_SIGNATURE,
    UNTRUSTED_SIGNER,
    CERTIFICATE_EXPIRED,
    CERTIFICATE_REVOKED,
    NOT_YET_VALID,
    TIMESTAMP_INVALID,
    UNSUPPORTED_ALGORITHM,
    // UNSUPPORTED_DIGEST,
    INVALID_KEY
    // NO_ALGORITHM ?
}