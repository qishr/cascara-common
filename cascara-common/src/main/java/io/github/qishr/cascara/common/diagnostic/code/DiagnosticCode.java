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


package io.github.qishr.cascara.common.diagnostic.code;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;

import io.github.qishr.cascara.common.lang.diagnostic.LangDiagnosticCode;

public interface DiagnosticCode {
    String getCode();
    String getMessage();

    static DiagnosticCode forException(Throwable t) {
        if (t instanceof InstantiationException) {
            return LangDiagnosticCode.INSTANTIATION_EXCEPTION;
        }
        // InaccessibleObjectException - if Java language access checks cannot be suppressed.
        else if (t instanceof InaccessibleObjectException) {
            return LangDiagnosticCode.FIELD_NOT_ACCESSIBLE;
        }
        // IllegalAccessException - if this Method object is enforcing Java language access control and the underlying method is inaccessible.
        else if (t instanceof IllegalAccessException) {
            return LangDiagnosticCode.FIELD_NOT_ACCESSIBLE;
        }
        // IllegalArgumentException - if the method is an instance method and the specified object argument is not an instance of the class or interface declaring the underlying method (or of a subclass or implementor thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
        else if (t instanceof IllegalArgumentException) {
            return LangDiagnosticCode.ILLEGAL_ARGUMENT_EXCEPTION;
        }
        // InvocationTargetException - if the underlying method throws an exception.
        else if (t instanceof InvocationTargetException) {
            return LangDiagnosticCode.INVOCATION_TARGET_EXCEPTION;
        }
        else if (t instanceof NoSuchMethodException) {
            return LangDiagnosticCode.NO_SUCH_METHOD;
        }
        // ExceptionInInitializerError - if the initialization provoked by this method fails.
        else if (t instanceof ExceptionInInitializerError) {
            return LangDiagnosticCode.EXCEPTION_IN_INITIALIZER;
        }
        // NullPointerException - if the specified object is null and the method is an instance method.
        else if (t instanceof NullPointerException) {
            return GenericDiagnosticCode.NPE;
        }
        else {
            return null;
        }
    }
}
