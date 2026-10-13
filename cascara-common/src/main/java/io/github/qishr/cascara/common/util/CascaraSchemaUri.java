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


package io.github.qishr.cascara.common.util;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;

public class CascaraSchemaUri {
    public static final String SCHEMA_SERVICE_URI = "cascara://core/schema-service";

    public static enum Lifecycle {
        DYNAMIC,
        DRAFT,
        RESOURCE
    }

    private final Lifecycle lifecycle;
    private final String moduleName;
    private final String schemaName;
    private final String version;

    public static CascaraSchemaUri of(Class<?> clazz) {
        try {
            Field schemaField = clazz.getField("schema");
            Object fieldValue = schemaField.get(null);
            if (fieldValue instanceof URI uri) {
                return CascaraSchemaUri.of(uri);
            }
            if (fieldValue instanceof String string) {
                return CascaraSchemaUri.of(URI.create(string));
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {}
        return new CascaraSchemaUri(Lifecycle.DYNAMIC, clazz.getModule().getName(), clazz.getName(), null);
    }

    public CascaraSchemaUri(String moduleName, String schemaName, String version) {
        this(Lifecycle.DRAFT, moduleName, schemaName, version);
    }

    public CascaraSchemaUri(String moduleName, String schemaName) {
        this(Lifecycle.RESOURCE, moduleName, schemaName, null);
    }

    public CascaraSchemaUri(String schemaName) {
        this(Lifecycle.DYNAMIC, "-", schemaName, null);
    }

    private CascaraSchemaUri(Lifecycle lifecycle, String moduleName, String schemaName, String version) {
        this.lifecycle = lifecycle;
        this.moduleName = moduleName;
        this.schemaName = schemaName;
        this.version = version;
    }

    public static CascaraSchemaUri of(URI uri) {
        if (!uri.getHost().equalsIgnoreCase("core")) {
            throw new LocalizableRuntimeException(GenericMessage.INVALID_SCHEMA_URI, uri);
        }

        Queue<String> segmentQueue = new LinkedList<>();
        String[] segments = uri.getPath().split("/");
        for (String s : segments) {
            // TODO: Is this right? Update DocumentService to match
            // segmentQueue.add(URLDecoder.decode(s, StandardCharsets.UTF_8));
            segmentQueue.add(s);
        }

        segmentQueue.poll(); // Remove the empty one

        if (!segmentQueue.poll().equalsIgnoreCase("schema-service")) {
            throw new LocalizableRuntimeException(GenericMessage.INVALID_SCHEMA_URI, uri);
        }

        Lifecycle lifecycle;
        String lifecycleString = segmentQueue.poll();

        if (lifecycleString == null) {
            throw new LocalizableRuntimeException(GenericMessage.INVALID_SCHEMA_URI, uri);
        }
        else if (lifecycleString.equals("dynamic")) {
            // Runtime Generation: dynamic/<module-name>/<schema-name>
            lifecycle = Lifecycle.DYNAMIC;
        }
        else if (lifecycleString.equals("draft")) {
            // Versioned Disk Assets: draft/<module-name>/<schema-name>/<version>
            lifecycle = Lifecycle.DRAFT;
        }
        else if (lifecycleString.equals("resource")) {
            // The "Latest" Alias: draft/<module-name>/<schema-name>
            lifecycle = Lifecycle.RESOURCE;
        } else {
            throw new LocalizableRuntimeException(GenericMessage.UNRECOGNIZED_LIFECYCLE, lifecycleString);
        }

        String moduleName = segmentQueue.poll();
        if (moduleName == null) {
            throw new LocalizableRuntimeException(GenericMessage.MISSING_MODULE_NAME, uri);
        }

        String schemaName = segmentQueue.poll();
        if (schemaName == null) {
            throw new LocalizableRuntimeException(GenericMessage.MISSING_SCHEMA_NAME, uri);
        }

        String version = null;
        if (lifecycle == Lifecycle.DRAFT) {
            // Versioned Disk Assets: draft/<module-name>/<schema-name>/<version>
            version = segmentQueue.poll();
            if (version == null) {
                throw new LocalizableRuntimeException(GenericMessage.MISSING_VERSION, uri);
            }
        }

        return new CascaraSchemaUri(lifecycle, moduleName, schemaName, version);
    }

    public URI toUri() {
        String uriString = switch (lifecycle) {
            case DRAFT: yield String.format(
                "%s/%s/%s/%s/%s",
                SCHEMA_SERVICE_URI,
                "draft",
                moduleName, schemaName, version
            );
            case RESOURCE: yield String.format(
                "%s/%s/%s/%s",
                SCHEMA_SERVICE_URI,
                "resource",
                moduleName, schemaName
            );
            default: yield String.format(
                "%s/%s/%s/%s",
                SCHEMA_SERVICE_URI,
                "dynamic",
                moduleName, schemaName
            );
        };
        return URI.create(uriString);
    }

    public Lifecycle getLifecycle() { return lifecycle; }
    public String getModuleName() { return moduleName; }
    public String getSchemaName() { return schemaName; }
    public String getVersion() { return version; }

    @Override
    public String toString() {
        return toUri().toString();
    }
}
