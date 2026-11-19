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

package io.github.qishr.cascara.common.diagnostic.message;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ServiceConfigurationError;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.qishr.cascara.common.i18n.DetailedMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;

public class DiagnosticMessage extends DetailedMessage {

    private Throwable cause;

    public DiagnosticMessage(LocalizableMessage message, Object... details) {
        super(message, details);
    }

    public DiagnosticMessage(Throwable cause, LocalizableMessage message, Object... details) {
        super(message, details);
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }

    public static DiagnosticMessage forException(Throwable cause) {
        return switch (cause) {
            // NullPointerException - if the specified object is null and the method is an instance method.
            case NullPointerException e -> new DiagnosticMessage(cause, GenericMessage.NPE, e.getMessage());

            case NumberFormatException e -> new DiagnosticMessage(cause, GenericMessage.NUMBER_FORMAT_EXCEPTION, e.getMessage());



            // NoClassDefFoundError - when trying to load in the definition of a class and no definition of the class could be found.
            case NoClassDefFoundError e -> new DiagnosticMessage(cause, GenericMessage.CLASS_NOT_FOUND, e.getMessage());

            case ClassNotFoundException e -> new DiagnosticMessage(cause, GenericMessage.CLASS_NOT_FOUND, e.getMessage());

            case ClassCastException e -> {
                // class java.net.URI cannot be cast to class java.lang.String (java.net.URI and java.lang.String are in module java.base of loader 'bootstrap')

                Pattern pattern = Pattern.compile(
                    "class\\s+([\\w.$]+)\\s+cannot be cast to class\\s+([\\w.$]+)"
                );

                Matcher matcher = pattern.matcher(e.getMessage());

                if (!matcher.find()) {
                    yield new DiagnosticMessage(cause, GenericMessage.JRE_CLASS_CAST_EXCEPTION, e.getMessage());
                }

                String source = matcher.group(1); // java.net.URI
                String target = matcher.group(2); // java.lang.String

                yield new DiagnosticMessage(cause, GenericMessage.CLASS_CAST_EXCEPTION, source, target);
            }



            case NoSuchMethodError e -> new DiagnosticMessage(cause, GenericMessage.NO_SUCH_METHOD, e.getMessage());

            // InaccessibleObjectException - if Java language access checks cannot be suppressed.
            case InaccessibleObjectException e -> new DiagnosticMessage(cause, GenericMessage.INACCESSIBLE_OBJECT_EXCEPTION, e.getMessage());

            // IllegalAccessException - if this Method object is enforcing Java language access control and the underlying method is inaccessible.
            case IllegalAccessException e -> new DiagnosticMessage(cause, GenericMessage.ILLEGAL_ACCESS_EXCEPTION, e.getMessage());

            // IllegalArgumentException - if the method is an instance method and the specified object argument is not an instance of the class or interface declaring the underlying method (or of a subclass or implementor thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
            case IllegalArgumentException e -> new DiagnosticMessage(cause, GenericMessage.ILLEGAL_ARGUMENT_EXCEPTION, e.getMessage());

            // InvocationTargetException - if the underlying method throws an exception.
            case InvocationTargetException e -> new DiagnosticMessage(cause, GenericMessage.INVOCATION_TARGET_EXCEPTION, e.getMessage());

            case NoSuchMethodException e -> new DiagnosticMessage(cause, GenericMessage.NO_SUCH_METHOD, e.getMessage());

            // ExceptionInInitializerError - if the initialization provoked by this method fails.
            case ExceptionInInitializerError e -> new DiagnosticMessage(cause, GenericMessage.EXCEPTION_IN_INITIALIZER, e.getMessage());

            // AbstractMethodError - when an application tries to call an abstract method.
            case AbstractMethodError e -> new DiagnosticMessage(cause, GenericMessage.ABSTRACT_METHOD_ERROR, e.getMessage());

            case ReflectiveOperationException e -> new DiagnosticMessage(cause, GenericMessage.REFLECTIVE_OPERATION_EXCEPTION, e.getMessage());



            // ServiceConfigurationError - when something goes wrong while locating, loading, or instantiating a service provider.
            case ServiceConfigurationError e -> new DiagnosticMessage(cause, GenericMessage.SERVICE_CONFIG_ERROR, e.getMessage());



            // Security exceptions
            case CertificateException e -> new DiagnosticMessage(cause, GenericMessage.CERTIFICATE_EXCEPTION, e.getMessage());
            case InvalidAlgorithmParameterException e -> new DiagnosticMessage(cause, GenericMessage.INVALID_ALGORITHM_PARAMETER_EXCEPTION, e.getMessage());
            case NoSuchAlgorithmException e -> new DiagnosticMessage(cause, GenericMessage.NO_SUCH_ALGORITHM_EXCEPTION, e.getMessage());
            case SecurityException e -> new DiagnosticMessage(cause, GenericMessage.SECURITY_EXCEPTION, e.getMessage());



            case UnsatisfiedLinkError e -> new DiagnosticMessage(cause, GenericMessage.UNSATISFIED_LINK_ERROR, e.getMessage());
            case InternalError e -> new DiagnosticMessage(cause, GenericMessage.INTERNAL_ERROR, e.getMessage());
            case Error e -> new DiagnosticMessage(cause, GenericMessage.ERROR, e.getMessage());

            default -> new DiagnosticMessage(cause, GenericMessage.ERROR, cause.getMessage());
        };
    }
}
