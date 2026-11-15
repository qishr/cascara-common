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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.i18n.exception.LocalizableException;
import io.github.qishr.cascara.common.trackable.property.TrackableProperty;

public abstract class Localizer {
    public static final String AUTOMATIC = "System";

    private static final Reporter REPORTER = Reporter.forClass(Localizer.class);

    private static Localizer INSTANCE;
    private static boolean isInitializing;

    // Collect initialization errors instead of reporting
    // immediately as their localization code will cause
    // an infinite loop.
    private static List<Throwable> initializationErrors = new ArrayList<>();

    public static Localizer instance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpLocalizer();
            isInitializing = true;
            MessageLocalizer localizer = new MessageLocalizer();
            INSTANCE = localizer;
            isInitializing = false;

            for (Throwable t : initializationErrors) {
                if (t instanceof LocalizableException e) {
                    REPORTER.error(t, e.getLocalizableMessage(), e.getDetails());
                } else {
                    REPORTER.error(t, GenericMessage.ERROR, "Failed to load translations: " + t.getLocalizedMessage());
                }
            }
        }
        return INSTANCE;
    }

    public abstract String formatWithDefault(String defaultText, String key, Object... args);
    public abstract String format(String key, Object... args);
    public abstract String format(LocalizableMessage message, Object... details);
    public abstract String formatException(LocalizableMessage message, Object... details);
    public abstract boolean hasLanguage(String languageTag);
    public abstract Locale getSystemLocale();
    public abstract Locale getActiveLocale();
    public abstract void setActiveLocale(Locale locale);
    public abstract void setActiveLanguage(String languageTag);
    public abstract TrackableProperty<Locale> activeLocaleProperty();
    public abstract boolean isRTL();
    public abstract boolean registerTranslations(Class<?> cls, String resourcePath);
    public abstract boolean registerTranslations(LocalizedMessages translation);

    protected void error(Throwable e) {
        if (isInitializing) {
            initializationErrors.add(e);
        } else {
            REPORTER.error(e, GenericMessage.ERROR, "Failed to load translations: " + e.getMessage());
        }
    }

    protected void error(String message) {
        if (isInitializing) {
            initializationErrors.add(new Exception(message));
        } else {
            REPORTER.error(GenericMessage.ERROR, message);
        }
    }
}