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

import java.text.MessageFormat;
import java.util.Locale;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.trackable.property.TrackableProperty;

public class NoOpLocalizer extends Localizer {

    public NoOpLocalizer() {}

    public String formatWithDefault(String defaultText, String key, Object... args) {
        return defaultText;
    }

    public String format(String key, Object... args) {
        return key;
    }

    public String format(LocalizableMessage message, Object... details) {
        // System.out.println("NoOpLocalizer: format: " + message.getFormat());
        return MessageFormat.format(get(message), details);
    }

    public String formatException(LocalizableMessage message, Object... details) {
        // System.out.println("NoOpLocalizer: formatException: " + message.getFormat());
        return MessageFormat.format(get(message), details);
    }

    public String formatException(DiagnosticMessage message) {
        return MessageFormat.format(get(message), message.getDetails());
    }

    public boolean hasLanguage(String languageTag) {
        return false;
    }

    public Locale getSystemLocale() {
        return null;
    }

    public Locale getActiveLocale() {
        return null;
    }

    public void setActiveLocale(Locale locale) {}

    public void setActiveLanguage(String languageTag) {}

    public TrackableProperty<Locale> activeLocaleProperty() {
        return null;
    }

    public boolean isRTL(){
        return false;
    }

    public boolean registerTranslations(Class<?> cls, String resourcePath) {
        return false;
    }

    public boolean registerTranslations(LocalizedMessages translation) {
        return false;
    }

    private String get(LocalizableMessage message) {
        return message.getFormat().replace("'", "''");
    }
}