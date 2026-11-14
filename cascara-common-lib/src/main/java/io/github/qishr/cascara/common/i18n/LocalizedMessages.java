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

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.AnyGetter;
import io.github.qishr.cascara.common.annotation.AnySetter;
import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.SchemaDefinition;
import io.github.qishr.cascara.common.annotation.SchemaProperty;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.Property;

@SchemaDefinition
public class LocalizedMessages {

    @SchemaProperty
    private Meta meta;

    @DataIgnore
    private Map<String, Object> translations = new HashMap<>();

    public LocalizedMessages() {}

    public LocalizedMessages(String languageTag, String title) {
        meta = new Meta();
        meta.lang = languageTag;
        meta.title = title;
    }

    @AnySetter
    public void add(String key, Object value) {
        this.translations.put(key, value);
    }

    @AnyGetter
    public Map<String, Object> getTranslations() {
        return translations;
    }

    public String getLanguageTag() {
        return meta.lang;
    }

    public String getTitle() {
        return meta.title;
    }

    @SchemaDefinition
    public static class Meta {
        @SchemaProperty
        private String lang;

        @SchemaProperty
        private String title;
    }

    @SuppressWarnings("unchecked")
    public static LocalizedMessages fromProperties(Properties properties) {
        LocalizedMessages localized = new LocalizedMessages(
            properties.getString("meta.lang"),
            properties.getString("meta.title"));

        for (Property<?> property : properties) {
            String name = property.getName();
            String value = property.asString();
            String[] parts = name.split("\\.");
            Map<String, Object> branch = localized.translations;

            for (int i = 0; i < parts.length; i++) {
                String key = parts[i];
                if (i == parts.length - 1) {
                    // Leaf
                    branch.put(key, value);
                } else {
                    // Branch
                    if (branch.get(key) instanceof Map map) {
                        branch = (Map<String,Object>) map;
                    } else {
                        Map<String,Object> map = new HashMap<>();
                        branch.put(key, map);
                        branch = map;
                    }
                }
            }
        }
        return localized;
    }
}
