package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.diagnostic.message.HttpMessage;
import io.github.qishr.cascara.common.diagnostic.message.InetMessage;
import io.github.qishr.cascara.common.i18n.DetailedMessage;
import io.github.qishr.cascara.common.i18n.LocalizedMessages;
import io.github.qishr.cascara.common.i18n.Localizer;
import io.github.qishr.cascara.common.property.Properties;

@Isolated
public class InternationalizationTests {

    private void loadFromResources(String languageTag) throws IOException {
        Localizer.instance().registerTranslations(getClass(), "/" + languageTag + ".properties");
    }

    @BeforeEach
    void setUp() {
        Localizer.instance().setActiveLanguage("en-US");
    }

    @Test
    void test_translationConstructed() {
        Localizer localizer = Localizer.instance();

        LocalizedMessages translation = new LocalizedMessages("en-US", "English");
        Map<String,Object> titleMap = new HashMap<>();
        titleMap.put("example", "Example Title");
        translation.add("title", titleMap);

        localizer.registerTranslations(translation);

        assertEquals("Example Title", localizer.format("title.example"));
    }

    @Test
    void test_translationFromProperties() {
        Localizer localizer = Localizer.instance();

        Properties properties = new Properties()
            .add("meta.lang", "en-US")
            .add("meta.title", "English")
            .add("title.example", "Example Title");

        LocalizedMessages translation = LocalizedMessages.fromProperties(properties);

        localizer.registerTranslations(translation);

        assertEquals("Example Title", localizer.format("title.example"));
    }

    @Test
    void test_translationFromEnglishResources() throws IOException {
        Localizer localizer = Localizer.instance();

        loadFromResources("en-US");

        assertEquals("Example Title", localizer.format("title.example"));
    }

    @Test
    void test_translationFromArabicResources() throws IOException {
        Localizer localizer = Localizer.instance();

        loadFromResources("ar-AE");

        localizer.setActiveLanguage("ar-AE");
        assertEquals("عنوان مثال", localizer.format("title.example"));
    }

    @Test
    void test_translationFromMessageEnum() {
        Localizer localizer = Localizer.instance();

        DetailedMessage message = new DetailedMessage(HttpMessage.NOT_FOUND, "nonexistentfile");

        assertEquals("Not found: nonexistentfile", localizer.format(message));
    }

    @Test
    void test_messageEnumFallbackToDefaultLanguage() throws IOException {
        Localizer localizer = Localizer.instance();

        DetailedMessage message = new DetailedMessage(InetMessage.CONNECTION_REFUSED, "example.com");

        localizer.setActiveLanguage("es-ES");
        assertEquals("Connection refused: example.com", localizer.format(message));
    }

    @Test
    void test_resourceFileFallbackToDefaultLanguage() throws IOException {
        Localizer localizer = Localizer.instance();

        loadFromResources("en-US");
        loadFromResources("fr-FR");

        localizer.setActiveLanguage("en-US");
        assertEquals("Missing Title", localizer.format("title.missing"));

        localizer.setActiveLanguage("fr-FR");
        assertEquals("Missing Title", localizer.format("title.missing"));
    }

    /// Resource file overrides default Enum message
    @Test
    void test_resourceFileOverridesEnumMessage() throws IOException {
        Localizer localizer = Localizer.instance();

        DetailedMessage message = new DetailedMessage(InetMessage.CONNECTION_REFUSED, "example.com");

        localizer.setActiveLanguage("en-US");
        assertEquals("Connection refused: example.com", localizer.format(message));

        loadFromResources("ar-AE");

        localizer.setActiveLanguage("ar-AE");
        assertEquals("رفض الاتصال: example.com", localizer.format(message));
    }

    @Test
    void test_acceptSetLanguageWithNoTranslations() throws IOException {
        Localizer localizer = Localizer.instance();

        // There are no translations for Trinidad and Tobago at the moment.
        localizer.setActiveLanguage("en-TT");

        assertEquals("en-TT", localizer.getActiveLocale().toLanguageTag());
    }
}
