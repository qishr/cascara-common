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

import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;

// SignedData ::= SEQUENCE {
//     version INTEGER,
//     digestAlgorithms SET,
//     contentInfo ContentInfo,
//     certificates [0] IMPLICIT CertificateSet OPTIONAL,
//     crls [1] IMPLICIT ...
//     signerInfos SET
// }

public final class SignedData {
    private final ASN1Integer version;
    private final ASN1Set digestAlgorithms;
    private final ASN1Sequence contentInfo;
    private final ASN1TaggedObject certificates;
    private final ASN1Set signerInfos;

    public SignedData(
        ASN1Integer version,
        ASN1Set digestAlgorithms,
        ASN1Sequence contentInfo,
        ASN1TaggedObject certificates,
        ASN1Set signerInfos
    ) {

        this.version = version;
        this.digestAlgorithms = digestAlgorithms;
        this.contentInfo = contentInfo;
        this.certificates = certificates;
        this.signerInfos = signerInfos;
    }

    public ASN1Integer version() {
        return version;
    }

    public ASN1Set digestAlgorithms() {
        return digestAlgorithms;
    }

    public ASN1Sequence contentInfo() {
        return contentInfo;
    }

    public ASN1TaggedObject certificates() {
        return certificates;
    }

    public ASN1Set signerInfos() {
        return signerInfos;
    }

    public static SignedData parse(ASN1Sequence seq) {
        ASN1Integer version = (ASN1Integer) seq.get(0);
        ASN1Set digestAlgorithms = (ASN1Set) seq.get(1);
        ASN1Sequence contentInfo = (ASN1Sequence) seq.get(2);
        ASN1TaggedObject certificates = (ASN1TaggedObject) seq.get(3);
        ASN1Set signerInfos = (ASN1Set) seq.get(4);
        return new SignedData(
                version,
                digestAlgorithms,
                contentInfo,
                certificates,
                signerInfos
        );
    }
}
