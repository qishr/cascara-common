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

package io.github.qishr.cascara.common.service.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.filewatcher.FileWatcher;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.service.ServiceProviderRoot;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.ContentTypeResolver;

public class SPLRoot extends SPLBranch implements ServiceProviderRoot {
    private static final Properties EMPTY_PROPERTIES = new Properties();

    final Set<String> bootProviders = new HashSet<>();

    private ContentTypeResolver contentTypeStore;
    private Set<ContentType> contentTypes;

    private FileWatcher propsFileWatcher;
    private Properties properties;

    private SPLRoot(Reporter reporter) {
        isBooting = true;
        rootLayer = this;
        name = "root";
        contentTypes = new HashSet<>();
        setReporter(reporter);
        loadPreferences();

        ModuleLayer boot = ModuleLayer.boot();
        boot.modules().forEach((module) -> {
            final String moduleName = module.getName();
            try {
                reporter.trace("Found module " + moduleName);
                registerModule(module);
            } catch (Exception e) {
                bootError(e,
                    ServiceDiagnosticCode.FAILED_TO_REGISTER_MODULE,
                    moduleName);
            }
        });

        // Fallback: classic ServiceLoader scanning for classpath/unnamed-module usage,
        // and to pick up any providers using META-INF/services even when modular.
        registerViaServiceLoader();

        isBooting = false;

        try {
            contentTypeStore = ServiceProviderLayer.loadDefault(ContentTypeResolver.class);
            // TODO: use addAll
            if (contentTypeStore != null) {
                for (ContentType contentType : contentTypes) {
                    contentTypeStore.add(contentType);
                }
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured.
    public static ServiceProviderRoot instance() {
        return instance(null);
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured with a specified Reporter.
    /// This reporter is used for non-fatal error and warning reporting.
    public static ServiceProviderRoot instance(Reporter reporter) {
        if (reporter == null) {
            reporter = new NoOpReporter();
        }
        if (rootLayer == null) {
            rootLayer = new SPLRoot(reporter);
        }
        return rootLayer;
    }

    public Set<ContentType> getContentTypes() {
        return contentTypes;
    }

    public void storeContentType(ContentType contentType) {
        contentTypes.add(contentType);
        if (!isBooting && contentTypeStore != null) {
            contentTypeStore.add(contentType);
        }
    }

    public Set<SPLBranch> allLayers() {
        Set<SPLBranch> collected = new HashSet<>();
        collectLayers(this, collected);
        return collected;
    }

    private void collectLayers(SPLBranch layer, Set<SPLBranch> collected) {
        collected.add(layer);
        for (SPLBranch descendant : layer.children) {
            collectLayers(descendant, collected);
        }
    }

    public String getPreferredProviderClassName(Class<?> serviceType) {
        return getProperties().getString(serviceType.getName());
    }

    public Properties getProperties() {
        Path propsFile = Cascara.getSplPropertiesPath();

        if (!Cascara.isFileTimeSupported()) {
            if (Files.exists(propsFile)) {
                try {
                    properties = Properties.load(propsFile);
                } catch (LocalizableIOException e) {
                    // Ignore and fall through to EMPTY_PROPERTIES
                }
            }
        }

        return properties == null ? EMPTY_PROPERTIES : properties;
    }

    private void loadPreferences() {
        Path propsFile = Cascara.getSplPropertiesPath();

        if (Cascara.isFileTimeSupported()) {
            if (!Files.exists(propsFile)) {
                try {
                    Files.createFile(propsFile);
                } catch (IOException e) {
                    reporter.error(e, FileDiagnosticCode.WRITE_ERROR, propsFile);
                    return;
                }
            }

            propsFileWatcher = new FileWatcher();
            try {
                propsFileWatcher.watchFile(propsFile, () -> {
                    try {
                        properties = Properties.load(propsFile);
                    } catch (LocalizableIOException e) {}
                });
            } catch (IOException e) {}

            try {
                properties = Properties.load(propsFile);
            } catch (LocalizableIOException e) {}
        }
    }
}
