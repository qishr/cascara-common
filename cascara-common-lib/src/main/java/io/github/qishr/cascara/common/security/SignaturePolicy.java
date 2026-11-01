package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.util.JarFile;

public interface SignaturePolicy {
    SignatureStatus evaluate(JarFile jar);
}
