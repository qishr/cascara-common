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

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Map;

import io.github.qishr.cascara.common.security.exception.UnsupportedAlgorithmException;
import io.github.qishr.cascara.common.security.exception.UnsupportedDigestException;

public final class SignatureAlgorithms {

    private SignatureAlgorithms() {}

    // Signature Algorithm OIDs -> JCA Algorithm Name
    private static final Map<String, String> OID_TO_JCA = Map.ofEntries(
            // RSA with SHA-1 / SHA-2
            Map.entry("1.2.840.113549.1.1.5", "SHA1withRSA"),
            Map.entry("1.2.840.113549.1.1.11", "SHA256withRSA"),
            Map.entry("1.2.840.113549.1.1.12", "SHA384withRSA"),
            Map.entry("1.2.840.113549.1.1.13", "SHA512withRSA"),
            Map.entry("1.2.840.113549.1.1.14", "SHA224withRSA"),

            // ECDSA with SHA-1 / SHA-2
            Map.entry("1.2.840.10045.4.1", "SHA1withECDSA"),
            Map.entry("1.2.840.10045.4.3.1", "SHA224withECDSA"),
            Map.entry("1.2.840.10045.4.3.2", "SHA256withECDSA"),
            Map.entry("1.2.840.10045.4.3.3", "SHA384withECDSA"),
            Map.entry("1.2.840.10045.4.3.4", "SHA512withECDSA"),

            // DSA
            Map.entry("1.2.840.10040.4.3", "SHA1withDSA"),
            Map.entry("2.16.840.1.101.3.4.3.1", "SHA224withDSA"),
            Map.entry("2.16.840.1.101.3.4.3.2", "SHA256withDSA"),

            // EdDSA (RFC 8410 / modern JDK 15+)
            Map.entry("1.3.101.112", "Ed25519"),
            Map.entry("1.3.101.113", "Ed448")
    );

    // JCA Name -> Signature OID
    private static final Map<String, String> JCA_TO_OID = Map.ofEntries(
            Map.entry("SHA1WITHRSA", "1.2.840.113549.1.1.5"),
            Map.entry("SHA256WITHRSA", "1.2.840.113549.1.1.11"),
            Map.entry("SHA384WITHRSA", "1.2.840.113549.1.1.12"),
            Map.entry("SHA512WITHRSA", "1.2.840.113549.1.1.13"),

            Map.entry("SHA1WITHECDSA", "1.2.840.10045.4.1"),
            Map.entry("SHA224WITHECDSA", "1.2.840.10045.4.3.1"),
            Map.entry("SHA256WITHECDSA", "1.2.840.10045.4.3.2"),
            Map.entry("SHA384WITHECDSA", "1.2.840.10045.4.3.3"),
            Map.entry("SHA512WITHECDSA", "1.2.840.10045.4.3.4"),

            Map.entry("SHA1WITHDSA", "1.2.840.10040.4.3"),
            Map.entry("SHA256WITHDSA", "2.16.840.1.101.3.4.3.2"),

            Map.entry("ED25519", "1.3.101.112"),
            Map.entry("ED448", "1.3.101.113")
    );

    // Digest Algorithm OID -> Standard Digest Name
    private static final Map<String, String> DIGEST_OID_TO_NAME = Map.of(
            "1.3.14.3.2.26", "SHA-1",
            "2.16.840.1.101.3.4.2.4", "SHA-224",
            "2.16.840.1.101.3.4.2.1", "SHA-256",
            "2.16.840.1.101.3.4.2.2", "SHA-384",
            "2.16.840.1.101.3.4.2.3", "SHA-512"
    );

    public static String getSignatureAlgorithmName(String oid) throws NoSuchAlgorithmException {
        String name = OID_TO_JCA.get(oid);
        if (name == null) {
            // throw new NoSuchAlgorithmException("Unsupported signature algorithm OID: " + oid);
            throw new UnsupportedAlgorithmException(oid);
        }
        return name;
    }

    public static String getSignatureOid(String jcaName) throws NoSuchAlgorithmException {
        String oid = JCA_TO_OID.get(jcaName.toUpperCase().replace("-", ""));
        if (oid == null) {
            // throw new NoSuchAlgorithmException("No known OID for signature algorithm: " + jcaName);
            throw new UnsupportedAlgorithmException(jcaName);
        }
        return oid;
    }

    public static String getDigestName(String digestOid) throws NoSuchAlgorithmException {
        String name = DIGEST_OID_TO_NAME.get(digestOid);
        if (name == null) {
            // throw new NoSuchAlgorithmException("Unsupported digest algorithm OID: " + digestOid);
            throw new UnsupportedDigestException(digestOid);
        }
        return name;
    }

    /**
     * Resolves the matching signature algorithm for a given private/public key,
     * picking the standard default digest (matching Java's default curve/key sizes).
     */
    public static String getDefaultSignatureAlgorithm(PublicKey publicKey) {
        String keyAlg = publicKey.getAlgorithm().toUpperCase();

        if ("EC".equals(keyAlg) && publicKey instanceof ECKey ecKey) {
            int bitLength = ecKey.getParams().getCurve().getField().getFieldSize();
            if (bitLength <= 256) return "SHA256withECDSA";
            if (bitLength <= 384) return "SHA384withECDSA";
            return "SHA512withECDSA";
        }

        if ("RSA".equals(keyAlg) && publicKey instanceof RSAKey rsaKey) {
            int bitLength = rsaKey.getModulus().bitLength();
            if (bitLength < 3072) return "SHA256withRSA";
            if (bitLength <= 4096) return "SHA384withRSA";
            return "SHA512withRSA";
        }

        if ("DSA".equals(keyAlg)) {
            return "SHA256withDSA";
        }

        if ("ED25519".equals(keyAlg) || "ED448".equals(keyAlg) || "EDDSA".equals(keyAlg)) {
            return keyAlg;
        }

        // Fallback default
        return "SHA256with" + keyAlg;
    }

    /**
     * Combines a digest algorithm (e.g. from PKCS#7 SignerInfo) with a key algorithm.
     */
    public static String resolveSignatureAlgorithm(String digestOid, PublicKey publicKey) throws NoSuchAlgorithmException {
        // If the OID in SignerInfo is already the full signature algorithm (e.g. SHA384withECDSA):
        if (OID_TO_JCA.containsKey(digestOid)) {
            return OID_TO_JCA.get(digestOid);
        }

        String digestName = getDigestName(digestOid).replace("-", ""); // e.g. "SHA384"
        String keyAlg = publicKey.getAlgorithm().toUpperCase();

        if ("EC".equals(keyAlg)) {
            return digestName + "withECDSA";
        } else if ("RSA".equals(keyAlg)) {
            return digestName + "withRSA";
        } else if ("DSA".equals(keyAlg)) {
            return digestName + "withDSA";
        }

        throw new NoSuchAlgorithmException("Cannot combine digest " + digestName + " with key algorithm " + keyAlg);
    }
}