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

package io.github.qishr.cascara.common.security.cms;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;

// CertificateSet ::= SET OF CertificateChoices

// CertificateChoices ::= CHOICE {
//     certificate Certificate,
//     extendedCertificate [0] IMPLICIT ExtendedCertificate,
//     v1AttrCert [1] IMPLICIT AttributeCertificateV1,
//     v2AttrCert [2] IMPLICIT AttributeCertificateV2,
//     other [3] IMPLICIT OtherCertificateFormat
// }

public final class CertificateSet {

    private final List<Certificate> certificates;

    private CertificateSet(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    public List<Certificate> certificates() {
        return certificates;
    }




public static CertificateSet parse(ASN1Set set) {
    List<Certificate> certificates = new ArrayList<>();

    for (ASN1Object<?> child : set) {
        if (child instanceof ASN1Sequence certSeq) {
            // certSeq is now the COMPLETE X.509 Certificate sequence!
            certificates.add(Certificate.parse(certSeq));
        }
    }

    return new CertificateSet(certificates);
}







    public static CertificateSet parse(
            ASN1Sequence sequence) {

        List<Certificate> certificates =
                new ArrayList<>();

        for (Object child : sequence) {

            if (!(child instanceof ASN1Sequence cert)) {
                continue;
            }

            certificates.add(
                    Certificate.parse(cert)
            );
        }

        return new CertificateSet(certificates);
    }
}