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


package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.LangDiagnosticCode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public abstract class AbstractScalarDescriptor<T> extends AbstractTypeDescriptor<T> implements ScalarDescriptor<T> {
    public static final String KEYWORD_FORMAT = "format";
    public static final String KEYWORD_CONTENT_ENCODING = "contentEncoding";

    private final String format;
    private final String contentEncoding;

    protected AbstractScalarDescriptor(Class<T> jvmType, String schemaType, String format) {
        this(jvmType, schemaType, format, null);
    }

    protected AbstractScalarDescriptor(Class<T> jvmType, String schemaType, String format, String contentEncoding) {
        super(jvmType, schemaType);

        this.format = format;
        this.contentEncoding = contentEncoding;

        if (format != null && !format.isEmpty()) {
            properties.set(KEYWORD_FORMAT, format);
        }
        if (contentEncoding != null && !contentEncoding.isEmpty()) {
            properties.set(KEYWORD_CONTENT_ENCODING, contentEncoding);
        }
    }

    @Override
    public String getFormat() {
        return properties.getString(KEYWORD_FORMAT);
    }

    @Override
    public String getContentEncoding() {
        return properties.getString(KEYWORD_CONTENT_ENCODING);
    }

    @Override
    public void populateSchema(MapAstNode<?,?,?> schema) {
        super.populateSchema(schema);

        // Inject other properties scalars might declare.

        if (format != null && !format.isEmpty()) {
            schema.put(KEYWORD_FORMAT, format);
        }

        if (contentEncoding != null && !contentEncoding.isEmpty()) {
            schema.put(KEYWORD_CONTENT_ENCODING, contentEncoding);
        }
    }

    protected void formatError(String text, Reporter collector) {
        if (collector != null) {
            collector.error(LangDiagnosticCode.WRONG_FORMAT, text, format);
        }
    }
}
