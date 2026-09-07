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


package io.github.qishr.cascara.common.util;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.DataField;

/// A stable, persisted canonical content type used throughout Cascara.
/// This class represents the authoritative identity of a content type,
/// including its canonical ID, canonical name, and the full set of MIME
/// types and filename suffixes associated with it.
///
/// ContentType instances are loaded from and saved to the
/// canonical-content-types.yaml registry file. They provide stable,
/// user‑facing identifiers that remain consistent across application runs,
/// module changes, and plugin installations.
///
/// All editor selection, syntax highlighting, file associations, and
/// user preferences should reference ContentType.
public class ContentType {
    @DataField
    /// Unique ID, used for menu items etc
    protected String canonicalId = "";

    @DataField
    protected String name = "";

    @DataField
    protected List<String> mimeTypes = new ArrayList<>();

    @DataField
    protected List<String> suffixes = new ArrayList<>();

    public ContentType() {}

    public ContentType(String name) {
        this.name = name;
    }

    public ContentType withType(String mimeType) {
        mimeTypes.add(mimeType);
        return this;
    }

    public ContentType withSuffix(String suffix) {
        suffixes.add(suffix);
        return this;
    }

    public String getCanonicalId() { return canonicalId; }

    public void setCanonicalId(String id) { this.canonicalId = id; }

    public String getName() {
        return name;
    }

    public void setName(String canonicalName) {
        this.name = canonicalName;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public List<String> getSuffixes() {
        return suffixes;
    }

    public void setSuffixes(List<String> suffixes) {
        this.suffixes = suffixes;
    }

    //
    //
    //

    public boolean isText() {
        for (String mimeType : mimeTypes) {
            if (mimeType.startsWith("text/")) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(String mimeType) {
        if (mimeTypes.contains(mimeType)) {
            return true;
        }
        for (String localType : mimeTypes) {
            if (localType.endsWith("/*")) {
                int slash = localType.indexOf("/");
                String baseType = localType.substring(0, slash);
                if (mimeType.startsWith(baseType + "/")) {
                    return true;
                }
            }
            if (mimeType.endsWith("/*")) {
                int slash = mimeType.indexOf("/");
                String baseType = mimeType.substring(0, slash);
                if (localType.startsWith(baseType + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matches(ContentType contentType) {
        if (contentType == null) {
            return false;
        }
        for (String metaMimeType : contentType.getMimeTypes()) {
            if (matches(metaMimeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (mimeTypes.isEmpty()) {
            return canonicalId;
        }
        return mimeTypes.getFirst();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}
