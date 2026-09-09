package io.github.qishr.cascara.common.trackable.diagnostic;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public enum TrackingDiagnosticCode implements DiagnosticCode {
    // Data
    UI_DATA_ERROR("UI-101", "UI data error."),
    PROPERTY_NOT_RECOGNIZED("UI-102","Unrecognized property name: {0}."),
    PROPERTY_NOT_FOUND_IN_MAP("UI-103","Object {0} is missing {1} from its observables map."),
    CANNOT_SET_VALUE("UI-104","Unable to set value for '{0}': {1}."),
    SCHEMA_NOT_SPECIFIED("UI-105", "Schema not specified."),
    SCHEMA_GENERATION_ERROR("UI-106", "ObservableObject failed to generate schema for {0}."),
    SCHEMA_COMPILATION_ERROR("UI-107", "ObservableObject failed to compile schema."),
    EMPTY_ENUM("UI-108", "Enum must not be empty"),
    OPTION_PROVIDER_INIT_ERROR("UI-109", "Option provider {0} failed to initialize: {0}");

    // // Render
    // UI_RENDER_ERROR("UI-201", "UI render error."),
    // NO_RENDERER_SET("UI-202", "No renderer set for property '{0}' with schema type {1} and content type {2}."),

    // // Controls
    // ADD_UNSUPPORTED("UI-301", "createMenuItem is unsupported for root."),

    // // Theme
    // INVALID_THEMING_TARGET("UI-401", "Target must be a Scene or a Parent."),
    // LOAD_DEFAULT_FAILURE("UI-402", "Failed to load default theme.");


    private final String code;
    private final String message;

    TrackingDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}