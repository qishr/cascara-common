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
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;

public class StackTraceElementTypeSerializer extends AbstractTypeDescriptor<StackTraceElement> implements TypeSerializer<StackTraceElement> {

    public StackTraceElementTypeSerializer() {
        super(StackTraceElement.class, PrimitiveType.STRING);
    }

    @Override
    public PlainMapNode serialize(StackTraceElement jvmInstance) throws SerializerException {
        return new PlainMapNode()
            .put("classLoaderName", jvmInstance.getClassLoaderName())
            .put("moduleName", jvmInstance.getModuleName())
            .put("moduleVersion", jvmInstance.getModuleVersion())
            .put("className", jvmInstance.getClassName())
            .put("methodName", jvmInstance.getMethodName())
            .put("fileName", jvmInstance.getFileName())
            .put("lineNumber", jvmInstance.getLineNumber());
    }

    @Override
    public StackTraceElement deserialize(AstNode astNode) throws SerializerException {
        if (astNode instanceof MapAstNode map) {
            // String classLoaderName = map.getString("classLoaderName");
            // String moduleName = map.getString("moduleName");
            // String moduleVersion = map.getString("moduleVersion");
            // String className = map.getString("className");
            // String methodName = map.getString("methodName");
            // String fileName = map.getString("fileName");
            // int lineNumber = map.getInteger("lineNumber");
            return new StackTraceElement(
                map.getString("classLoaderName"),
                map.getString("moduleName"),
                map.getString("moduleVersion"),
                map.getString("className"),
                map.getString("methodName"),
                map.getString("fileName"),
                map.getInteger("lineNumber")
            );
        }
        return null;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }
}
