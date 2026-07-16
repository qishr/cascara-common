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


package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.Properties;

public class ServiceMetadata {
    private final Class<? extends ServiceProvider> type;
    private Class<?> capabilityType;
    private final Properties properties;
    private final ContentType contentType;

    public ServiceMetadata(Class<? extends ServiceProvider> type, Properties properties) {
        this(type, properties, null);
    }

    public ServiceMetadata(Class<? extends ServiceProvider> type, Properties properties, ContentType contentType) {
        this.type = type;
        this.properties = properties;
        this.contentType = contentType;
        String capTypeString = properties.getString("javaType");
        try {
            capabilityType = capTypeString == null ? null : Class.forName(capTypeString);
        } catch (Exception e) {
        }
    }

    public boolean getBooleanCapability(String capName) {
        if (capName == null || capName.isEmpty()) return false;
        return properties.getBoolean(capName, false);
    }

    public String getModuleName() {
        return type.getModule().getName();
    }

    // TODO: This should be getProviderTypeName or getProviderClassName
    public String getTypeName() {
        return type.getName();
    }

    // TODO: This should be getProviderType or getProviderClass
    public Class<? extends ServiceProvider> getType() {
        return type;
    }

    public Class<?> getCapabilityType() {
        return capabilityType;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String name) {
        if (properties == null) return null;
        return properties.getString(name);
    }

    public String getJarPath() {
        return getProperty("jarPath");
    }

    public String getTitle() {
        return getProperty("title");
    }

    public ContentType getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceMetadata honesty = (ServiceMetadata) o;
        return java.util.Objects.equals(type, honesty.type);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type);
    }

    @Override
    public String toString() {
        return type.getSimpleName();
    }
}
