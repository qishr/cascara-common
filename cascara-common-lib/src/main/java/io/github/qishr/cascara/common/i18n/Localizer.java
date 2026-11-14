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

package io.github.qishr.cascara.common.i18n;

import java.text.Bidi;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.i18n.detect.HybridLocaleDetector;
import io.github.qishr.cascara.common.trackable.property.TrackableProperty;
import io.github.qishr.cascara.common.util.StringUtils;

public class Localizer {
    public static final String FORMATTING_ERROR = "Problem encountered while formatting message with code %s: %s";

    public static final String AUTOMATIC = "System";

    private static final Reporter REPORTER = Reporter.forClass(Localizer.class);

    // TODO: Should the caller should be able to set these?
    private static final String fallbackLanguageTag = "en-US";
    private static final Locale fallbackLocale = Locale.forLanguageTag(fallbackLanguageTag);

    private final TrackableProperty<Locale> activeLocale = new TrackableProperty<>();

    private final Map<String, Map<String, Object>> translationsByLanguage = new HashMap<>();

    private String activeLanguageTag;

    private boolean usingAutoLocale;

    private static final Localizer INSTANCE = new Localizer();

    private Localizer() {
        setActiveLanguage(AUTOMATIC);
    }

    public static Localizer instance() {
        return INSTANCE;
    }

    public boolean hasLanguage(String languageTag) {
        return translationsByLanguage.containsKey(languageTag);
    }

    public Locale getSystemLocale() {
        return HybridLocaleDetector.detectOSLocale();
    }

    public Locale getActiveLocale() {
        return activeLocale.getValue();
    }

    public void setActiveLocale(Locale locale) {
        // In Locale:
        //
        // Language is an ISO 639 alpha-2 or alpha-3 code or registered language subtag.
        // Region is an ISO 3166 alpha-2 country code or UN numeric-3 area code.
        // Variant is a case-sensitive value or set of values specifying a variation of a Locale.
        // Script must be a valid ISO 15924 alpha-4 code.
        // Extensions is a map which consists of single character keys and String values.
        //
        // IANA Language Subtag Registry: Language, Region, Variant, Script
        // https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry

        String languageTag = locale == null ? null : locale.toLanguageTag();
        setActiveLanguage(languageTag);
    }

    /// @param languageTag IETF BCP 47 language tag string
    public void setActiveLanguage(String languageTag) {
        boolean automatic = shouldUseAutoLocale(languageTag);
        if (automatic) {
            if (!usingAutoLocale) {
                Locale locale = getSystemLocale();
                String systemLanguageTag = locale.toLanguageTag();
                if (translationsByLanguage.containsKey(systemLanguageTag)) {
                    activeLanguageTag = systemLanguageTag;
                    activeLocale.setValue(locale);
                } else {
                    activeLanguageTag = fallbackLanguageTag;
                    activeLocale.setValue(fallbackLocale);
                }
            }
        } else {
            if (!languageTag.equals(activeLanguageTag)) {
                Locale locale = Locale.forLanguageTag(languageTag);

                // activeLanguageTag must be set before the activeLocale
                activeLanguageTag = languageTag;
                activeLocale.setValue(locale);
            }
        }
        usingAutoLocale = automatic;
    }

    /// {@inheritDoc}
    public TrackableProperty<Locale> activeLocaleProperty() {
        // TODO: This should be read-only
        // return activeLocale.getReadOnlyProperty();
        return activeLocale;
    }

    public boolean isRTL() {
        Locale locale = activeLocale.getValue();
        if (locale == null) {
            return false;
        }
        String displayLanguage = locale.getDisplayLanguage(locale);
        Bidi bidi = new Bidi(displayLanguage, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        return !bidi.isLeftToRight();
    }

    //
    // Formatting
    //

    public String formatWithDefault(String defaultText, String key, Object... args) {
        String pattern = getPattern(key);
        if (pattern == null) {
            return defaultText == null ? key : defaultText;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException e) {
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
        } catch (IllegalArgumentException e) {
            REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, key, pattern);
            return "!!" + key;
        }
    }

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    public String format(LocalizableMessage message, Object... details) {
        if (message.getCode() == null || message.getCode().isEmpty()) {
            return String.format(Localizer.FORMATTING_ERROR, message.getCode(), message.getFormat());
        }

        // String pattern = getPattern("diagnostic/" + message.getCode());

        LocalizableMessage rawMessage = message;
        if (message instanceof DetailedMessage dm && details.length == 0) {
            details = dm.getDetails();
            rawMessage = dm.getMessage();
        }

        String packageName = rawMessage.getClass().getPackageName();
        String className = rawMessage.getClass().getSimpleName();
        String kebabPackageName = StringUtils.toKebabCase(packageName);
        String kebabclassName = StringUtils.toKebabCase(className);

        String key = kebabPackageName + "." + kebabclassName + "." + message.getCode();

        String pattern = getPattern(key);

        if (pattern == null) {
            try {
                return MessageFormat.format(message.getFormat(), details);
            } catch (IllegalArgumentException e) {
                REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, message.getCode(), message.getFormat());
                return "!!" + message.getCode();
            }
        }
        try {
            return MessageFormat.format(pattern, details);
        } catch (IllegalArgumentException e) {
            REPORTER.error(e, GenericMessage.MESSAGE_FORMATTING_ERROR, message.getCode(), pattern);
            return "!!" + message.getCode();
        }
    }

    //
    // Registration
    //

    public boolean registerTranslations(LocalizedMessages translation) {
        try {
            String languageTag = translation.getLanguageTag();
            if (languageTag == null) {
                REPORTER.error(GenericMessage.ERROR, "No `lang` key specified in translations file");
                return false;
            }
            Map<String, Object> translations = translationsByLanguage.get(languageTag);
            if (translations == null) {
                translations = new HashMap<>();
                translationsByLanguage.put(languageTag, translations);
            }
            mergeMaps(translations, translation.getTranslations());
            if (usingAutoLocale) {
                setActiveLocale(HybridLocaleDetector.detectOSLocale());
            }
            return true;
        } catch (Exception e) {
            REPORTER.error(e, GenericMessage.ERROR, "Failed to load translations: " + e.getMessage());
            return false;
        }
    }

    //
    // Private Methods
    //

    private boolean shouldUseAutoLocale(String languageTag) {
        // If languageTag is null or "System", detect the system one
        if (languageTag == null || languageTag.toLowerCase().equals(AUTOMATIC)) {
            return true;
        } else {
            // Otherwise, check if the language is registered
            return !translationsByLanguage.containsKey(languageTag);
        }
    }

    private String get(String key) {
        Object value = resolveKey(key, translationsByLanguage.get(activeLanguageTag));
        if (value == null) {
            value = resolveKey(key, translationsByLanguage.get(fallbackLanguageTag));
        }
        return value == null ? null : value.toString();
    }

    private String getPattern(String key) {
        String pattern = get(key);
        if (pattern == null) {
            return null;
        }
        return pattern.replace("'", "''");
    }

    private Object resolveKey(String key, Map<String,Object> languageTree) {
        if (key.startsWith("#/")) {
            key = key.substring(2);
        } else if (key.startsWith("/")) {
            key = key.substring(1);
        }

        String[] parts;
        if (key.contains(".")) {
            parts = key.split("\\.");
        } else {
            parts = key.split("/");
        }

        Object current = languageTree;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /// Merges a new set of translations into the existing pool.
    /// If a key already exists, the new value overwrites the old one.
    /// This allows modules to "patch" or add to the global dictionary.
    @SuppressWarnings("unchecked")
    private void mergeMaps(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();

            if (sourceValue instanceof Map && target.get(key) instanceof Map) {
                // Both are maps, so we need to dive deeper and merge them
                mergeMaps((Map<String, Object>) target.get(key), (Map<String, Object>) sourceValue);
            } else {
                // It's a leaf node (String/List) or the target doesn't have this branch yet.
                // Just put the source value in.
                target.put(key, sourceValue);
            }
        }
    }
}