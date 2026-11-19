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

package io.github.qishr.cascara.common.trackable;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;

public enum TrackingMessage implements LocalizableMessage {
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

    private final String code;
    private final String format;

    TrackingMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}