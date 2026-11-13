package io.github.qishr.cascara.common.diagnostic;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ServiceConfigurationError;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;

public class DetailedMessage implements LocalizableMessage {
    private final LocalizableMessage message;
    private final Object[] details;

    public DetailedMessage(LocalizableMessage message, Object... details) {
        this.message = message;
        this.details = details;
    }

    public String name() {
        return message.name();
    }

    public String getCode() {
        return message.getCode();
    }

    public String getFormat() {
        return message.getFormat();
    }

    public Object[] getDetails() {
        return details;
    }

    public LocalizableMessage getMessage() {
        return message;
    }

    public static DetailedMessage forException(Throwable t) {
        return switch (t) {
            // NullPointerException - if the specified object is null and the method is an instance method.
            case NullPointerException e -> new DetailedMessage(GenericMessage.NPE, e.getMessage());

            case NoSuchMethodError e -> new DetailedMessage(GenericMessage.NO_SUCH_METHOD, e.getMessage());

            // InaccessibleObjectException - if Java language access checks cannot be suppressed.
            case InaccessibleObjectException e -> new DetailedMessage(GenericMessage.INACCESSIBLE_OBJECT_EXCEPTION, e.getMessage());

            // IllegalAccessException - if this Method object is enforcing Java language access control and the underlying method is inaccessible.
            case IllegalAccessException e -> new DetailedMessage(GenericMessage.ILLEGAL_ACCESS_EXCEPTION, e.getMessage());

            // IllegalArgumentException - if the method is an instance method and the specified object argument is not an instance of the class or interface declaring the underlying method (or of a subclass or implementor thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
            case IllegalArgumentException e -> new DetailedMessage(GenericMessage.ILLEGAL_ARGUMENT_EXCEPTION, e.getMessage());

            // InvocationTargetException - if the underlying method throws an exception.
            case InvocationTargetException e -> new DetailedMessage(GenericMessage.INVOCATION_TARGET_EXCEPTION, e.getMessage());

            case NoSuchMethodException e -> new DetailedMessage(GenericMessage.NO_SUCH_METHOD, e.getMessage());

            // ExceptionInInitializerError - if the initialization provoked by this method fails.
            case ExceptionInInitializerError e -> new DetailedMessage(GenericMessage.EXCEPTION_IN_INITIALIZER, e.getMessage());

            // AbstractMethodError - when an application tries to call an abstract method.
            case AbstractMethodError e -> new DetailedMessage(GenericMessage.ABSTRACT_METHOD_ERROR, e.getMessage());



            // NoClassDefFoundError - when trying to load in the definition of a class and no definition of the class could be found.
            case NoClassDefFoundError e -> new DetailedMessage(GenericMessage.CLASS_NOT_FOUND, e.getMessage());

            case ClassNotFoundException e -> new DetailedMessage(GenericMessage.CLASS_NOT_FOUND, e.getMessage());



            // ServiceConfigurationError - when something goes wrong while locating, loading, or instantiating a service provider.
            case ServiceConfigurationError e -> new DetailedMessage(GenericMessage.SERVICE_CONFIG_ERROR, e.getMessage());


            // Security exceptions
            case CertificateException e -> new DetailedMessage(GenericMessage.CERTIFICATE_EXCEPTION, e.getMessage());
            case InvalidAlgorithmParameterException e -> new DetailedMessage(GenericMessage.INVALID_ALGORITHM_PARAMETER_EXCEPTION, e.getMessage());
            case NoSuchAlgorithmException e -> new DetailedMessage(GenericMessage.NO_SUCH_ALGORITHM_EXCEPTION, e.getMessage());
            case SecurityException e -> new DetailedMessage(GenericMessage.SECURITY_EXCEPTION, e.getMessage());



            case UnsatisfiedLinkError e -> new DetailedMessage(GenericMessage.UNSATISFIED_LINK_ERROR, e.getMessage());
            case InternalError e -> new DetailedMessage(GenericMessage.INTERNAL_ERROR, e.getMessage());
            case Error e -> new DetailedMessage(GenericMessage.ERROR, e.getMessage());

            default -> new DetailedMessage(GenericMessage.ERROR, t.getMessage());
        };



    }

    // Instantiation / Invocation errors:
    // InaccessibleObjectException
    // IllegalAccessException
    // IllegalArgumentException
    // InvocationTargetException
    // NoSuchMethodException
    // ExceptionInInitializerError
    // AbstractMethodError
    //

    // Class loading errors:
    // java.lang.Error
    // java.lang.InternalError
    // java.lang.UnsatisfiedLinkError
    // java.lang.ClassNotFoundException
    // java.lang.ExceptionInInitializerError
    // java.lang.NoClassDefFoundError
    // java.lang.IllegalAccessError

    // Others:
    // NullPointerException
    // ServiceConfigurationError
    // SecurityException
    // IllegalAccessException
    // UnsatisfiedLinkError
    // InternalError
    // Error
}
