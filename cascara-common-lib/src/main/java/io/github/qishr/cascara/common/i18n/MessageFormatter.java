package io.github.qishr.cascara.common.i18n;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Function;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.StringUtils;

public class MessageFormatter {
    private static final Reporter REPORTER = Reporter.forClass(MessageFormatter.class);

    private final Function<String,String> lookup;
    private final boolean localizeExceptions;

    public MessageFormatter(Function<String,String> lookup, boolean localizeExceptions) {
        this.lookup = lookup;
        this.localizeExceptions = localizeExceptions;
    }

    private String getPattern(String key) {
        return lookup.apply(key);
    }

    public String formatWithDefault(String defaultText, String key, Object... args) {
        String pattern = getPattern(key);
        if (pattern == null) {
            return defaultText == null ? key : defaultText;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, key, pattern);
            return "!!" + defaultText == null ? key : defaultText;
        }
    }

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    public String format(String key, Object... args) {
        String pattern = getPattern(key);
        if (pattern == null) {
            return key;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, key, pattern);
            return "!!" + key;
        }
    }

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    public String formatException(LocalizableMessage message, Object... details) {
        if (localizeExceptions) {
            return format(message, details);
        } else {
            try {
                if (message instanceof DetailedMessage dm && details.length == 0) {
                    details = dm.getDetails();
                }
                return MessageFormat.format(message.getFormat().replace("'", "''"), details);
            } catch (Exception e) {
                REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, message.getCode(), message.getFormat());
                return "!!" + message.getCode();
            }
        }
    }

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    public String format(LocalizableMessage message, Object... details) {
        if (message.getCode() == null || message.getCode().isEmpty()) {
            REPORTER.error(GenericMessage.MESSAGE_FORMATTING_ERROR, message.getCode(), message.getFormat());
            // return String.format(Localizer.FORMATTING_ERROR, message.getCode(), message.getFormat());
            // return MessageFormat.format(GenericMessage.MESSAGE_FORMATTING_ERROR.getFormat(), message.getCode(), message.getFormat());
            return "!!" + message.toString();
        }

        // String pattern = getPattern("diagnostic/" + message.getCode());

        LocalizableMessage rawMessage = message;
        if (message instanceof DetailedMessage dm && details.length == 0) {
            details = dm.getDetails();
            rawMessage = dm.getMessage();
        }

        String className = StringUtils.toKebabCase(rawMessage.getClass().getSimpleName());
        String moduleName = StringUtils.toKebabCase(getModuleName(rawMessage.getClass()));
        String key = moduleName + "." + className + "." + message.getCode().toLowerCase();

        // Attempt to get a format pattern from translation file
        String pattern = getPattern(key);

        // Use the message's default format pattern if there was none in translation file
        if (pattern == null) {
            pattern = message.getFormat().replace("'", "''");
        }

        try {
            return MessageFormat.format(pattern, details);
        } catch (Exception e) {
            REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, message.getCode(), pattern);
            return "!!" + message.getCode();
        }
    }

    private String getModuleName(Class<?> cls) {
        Module module = cls.getModule();
        if (module.isNamed()) {
            return module.getName();
        }
        try {
            JarManifest manifest = JarManifest.parse(JreUtils.getResourceAsString(cls, "/META-INF/MANIFEST.MF"));
            return manifest.getString("Module-Name");
        } catch (IOException e) {
            return null;
        }
    }
}
