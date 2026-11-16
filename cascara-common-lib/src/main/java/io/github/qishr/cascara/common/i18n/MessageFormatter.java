package io.github.qishr.cascara.common.i18n;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
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
        String moduleName = getModuleName(rawMessage.getClass());
        // String moduleName = StringUtils.toKebabCase(getModuleName(rawMessage.getClass()));
        String code = message.getCode().toLowerCase();

        String key = (moduleName != null && !moduleName.isBlank())
            ? StringUtils.toKebabCase(moduleName) + "." + className + "." + code
            : className + "." + code;

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
        // 1. Active JPMS Named Module
        Module module = cls.getModule();
        if (module.isNamed()) {
            return module.getName();
        }

        // 2. Non-JPMS: Inspect the specific CodeSource location for this class
        try {
            CodeSource cs = cls.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Path codeRoot = Path.of(cs.getLocation().toURI());

                // Case A: Exploded build directory / IDE output
                if (Files.isDirectory(codeRoot)) {
                    Path moduleInfoPath = codeRoot.resolve("module-info.class");
                    if (Files.exists(moduleInfoPath)) {
                        try (InputStream is = Files.newInputStream(moduleInfoPath)) {
                            return ModuleDescriptor.read(is).name();
                        }
                    }

                    Path manifestPath = codeRoot.resolve("META-INF/MANIFEST.MF");
                    if (Files.exists(manifestPath)) {
                        try (InputStream is = Files.newInputStream(manifestPath)) {
                            Manifest manifest = new Manifest(is);
                            String name = getModuleNameFromManifest(manifest);
                            if (name != null) return name;
                        }
                    }
                }
                // Case B: Packaged JAR on standard classpath
                else if (Files.isRegularFile(codeRoot) && codeRoot.toString().endsWith(".jar")) {
                    Set<ModuleReference> refs = ModuleFinder.of(codeRoot).findAll();
                    if (!refs.isEmpty()) {
                        return refs.iterator().next().descriptor().name();
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback for security managers or un-mapped CodeSources
        }

        return null;
    }

    private String getModuleNameFromManifest(Manifest manifest) {
        Attributes attrs = manifest.getMainAttributes();
        String name = attrs.getValue("Automatic-Module-Name");
        if (name == null) name = attrs.getValue("Module-Name");
        if (name == null) name = attrs.getValue("Bundle-SymbolicName");
        if (name != null && !name.isBlank()) {
            int semicolon = name.indexOf(';');
            return semicolon > 0 ? name.substring(0, semicolon).trim() : name;
        }
        return null;
    }
}
