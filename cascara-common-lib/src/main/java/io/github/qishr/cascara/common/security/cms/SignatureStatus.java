package io.github.qishr.cascara.common.security.cms;

public enum SignatureStatus {
    VALID,
    NOT_SIGNED,
    INVALID_SIGNATURE,
    UNTRUSTED_SIGNER,
    CERTIFICATE_EXPIRED,
    CERTIFICATE_REVOKED
}