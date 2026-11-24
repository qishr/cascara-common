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

import java.io.IOException;
import java.io.InputStream;
import java.text.Bidi;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.qishr.cascara.common.i18n.detect.HybridLocaleDetector;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.trackable.property.TrackableProperty;
import io.github.qishr.cascara.common.util.JreUtils;

public class MessageLocalizer extends Localizer {
    // DO NOT use Reporter in this class, it would get into an infinite loop.

    private static final String fallbackLanguageTag = "en-US";

    private final TrackableProperty<Locale> activeLocale = new TrackableProperty<>();

    private final Map<String, Map<String, Object>> translationsByLanguage = new HashMap<>();

    private String activeLanguageTag;

    private boolean usingAutoLocale;

    private MessageFormatter formatter;

    MessageLocalizer() {
        I18nConfig config = I18nConfig.get();
        formatter = new MessageFormatter(this::getPattern, config.localizeExceptions());

        registerTranslations(Localizer.class, "/META-INF/fr-FR.properties");
        setActiveLanguage(AUTOMATIC);

        String languageTagProp = config.getLocale();

        if (languageTagProp != null && !languageTagProp.isBlank()) {
            setActiveLanguage(languageTagProp);
        }
    }

    public String formatWithDefault(String defaultText, String key, Object... args) {
        return formatter.formatWithDefault(defaultText, key, args);
    }

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    public String format(String key, Object... args) {
        return formatter.format(key, args);
    }

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    public String format(LocalizableMessage message, Object... details) {
        return formatter.format(message, details);
    }

    public String formatException(LocalizableMessage message, Object... details) {
        return formatter.formatException(message, details);
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
        if (languageTag == null || languageTag.equalsIgnoreCase(AUTOMATIC)) {
            usingAutoLocale = true;
            Locale locale = getSystemLocale();
            languageTag = locale.toLanguageTag();
        } else {
            usingAutoLocale = false;
        }

        if (!languageTag.equals(activeLanguageTag)) {
            Locale locale = Locale.forLanguageTag(languageTag);

            // activeLanguageTag must be set before the activeLocale
            activeLanguageTag = languageTag;
            activeLocale.setValue(locale);
        }
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
    // Registration
    //

    public boolean registerTranslations(Class<?> cls, String resourcePath) {
        try {
            InputStream is = JreUtils.getResourceAsStream(cls, resourcePath);
            Properties properties = Properties.fromStream(is);
            LocalizedMessages translations = LocalizedMessages.fromProperties(properties);
            registerTranslations(translations);
            return true;
        } catch (IOException e) {
            error(e);
            return false;
        }
    }

    public boolean registerTranslations(LocalizedMessages translation) {
        try {
            String languageTag = translation.getLanguageTag();
            if (languageTag == null) {
                error("No `lang` key specified in translations file");
                return false;
            }
            Map<String, Object> translations = translationsByLanguage.get(languageTag);
            if (translations == null) {
                translations = new HashMap<>();
                translationsByLanguage.put(languageTag, translations);
            }
            mergeMaps(translations, translation.getTranslations());
            if (usingAutoLocale) {
                setActiveLanguage(AUTOMATIC);
            }
            return true;
        } catch (Exception e) {
            error(e);
            return false;
        }
    }

    //
    // Private Methods
    //

    private String getPattern(String key) {
        String lowerCaseKey = key.toLowerCase();
        Object value = resolveKey(lowerCaseKey, translationsByLanguage.get(activeLanguageTag));
        if (value == null) {
            value = resolveKey(lowerCaseKey, translationsByLanguage.get(fallbackLanguageTag));
        }
        return value == null ? null : value.toString().replace("'", "''");
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
            String key = entry.getKey().toLowerCase();
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
