// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package io.github.qishr.cascara.common.security.cert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.security.SecurityConfig;
import io.github.qishr.cascara.common.security.exception.TrustStoreException;

public final class TrustStore {

    private final Set<X509Certificate> trustedCertificates;

    public TrustStore(Set<X509Certificate> trustedCertificates) {
        this.trustedCertificates = Collections.unmodifiableSet(new HashSet<>(trustedCertificates));
    }

    public static TrustStore empty() {
        return new TrustStore(Set.of());
    }

    /// Loads a TrustStore from standard Cascara system properties:
    /// -Dcascara.truststore=/path/to/truststore.jks
    /// -Dcascara.truststore.password=secret
    /// @throws IOException
    public static TrustStore load() {
        SecurityConfig config = SecurityConfig.get();
        if (config.getTrustStorePath() == null) {
            return empty();
        }
        return load(config.getTrustStorePath(), config.getTrustStorePassword().toCharArray());
    }

    public static TrustStore load(Path path, char[] password) {
        Set<X509Certificate> certs = new HashSet<>();
        try (InputStream is = Files.newInputStream(path)) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(is, password);

            var aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isCertificateEntry(alias) && ks.getCertificate(alias) instanceof X509Certificate x509) {
                    certs.add(x509);
                }
            }
        } catch (IOException e) {
            // TODO: Test this, improve error reporting
            throw new TrustStoreException(e, FileMessage.READ_ERROR, path.toString());
        } catch (KeyStoreException e) {
            // from ks.aliases and ks.getCertificate
            // TODO: Test this, improve error reporting
            throw new TrustStoreException(e, GenericMessage.ERROR, e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            // from ks.load
            // TODO: Test this, improve error reporting
            throw new TrustStoreException(e, GenericMessage.ERROR, e.getMessage());
        } catch (CertificateException e) {
            // from ks.load
            // TODO: Test this, improve error reporting
            throw new TrustStoreException(e, GenericMessage.ERROR, e.getMessage());
        }
        return new TrustStore(certs);
    }

    public boolean contains(X509Certificate cert) {
        return trustedCertificates.contains(cert);
    }

    public boolean isEmpty() {
        return trustedCertificates.isEmpty();
    }

    public boolean isTrusted(X509Certificate cert, boolean checkExpiry) {
        if (checkExpiry) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                return false;
            }
        }
        return trustedCertificates.contains(cert);
    }

    public Set<X509Certificate> getCertificates() {
        return trustedCertificates;
    }
}