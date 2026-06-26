package io.github.qishr.cascara.common.diagnostic.code;

public enum ServiceDiagnosticCode implements DiagnosticCode {
    NOT_A_SERVICE("SPL-101", "{0} is not a ServiceProvider."),
    NOT_A_SERVICE_PROVIDER("SPL-102", "{0} is not a ServiceProvider."),

    FAILED_TO_REGISTER_MODULE("SPL-201", "Failed to register {0} module."),

    NOARGS_CONSTRUCTOR_REQUIRED("SPL-301", "Class {0} has no no-args constructor."),
    FAILED_TO_INSTANTIATE_CLASS("SPL-302", "Failed to instantiate class {0}. {1}."),
    FAILED_TO_LOAD_CLASS("SPL-304", "Failed to instantiate class {0}. {1}."),
    FAILED_TO_READ_JAR("SPL-305", "Failed to read Jar \"{0}\". {1}."),
    NON_MODULAR_JAR("SPL-306", "Jar \"{0}\" does not contain a module."),
    NO_PROVIDER_REGISTERED("SPL-307", "No {0} providers registered."),
    NO_PROVIDER_REGISTERED_FOR("SPL-301", "No {0} providers registered for {1}.");

    private final String code;
    private final String message;

    ServiceDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}