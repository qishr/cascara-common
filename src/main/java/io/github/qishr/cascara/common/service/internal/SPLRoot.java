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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.qishr.cascara.common.annotation.SingletonInitializer;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.filewatcher.FileWatcher;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.service.ServiceProviderRoot;
import io.github.qishr.cascara.common.trackable.TrackableArray;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.ContentTypeResolver;

public class SPLRoot extends SPLBranch implements ServiceProviderRoot {
    private static final Properties EMPTY_PROPERTIES = new Properties();

    final Set<String> bootProviders = new HashSet<>();
    private final TrackableArray<ServiceMetadata> userProviders = new TrackableArray<>();
    private final Map<ServiceMetadata, Object> singletonCache = new ConcurrentHashMap<>();
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
            // TODO: Make sure ContentTypeStore in common.io is a perfect neo-singleton
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

    public String getPreferredProviderClassName(Class<?> serviceType) {
        return getProperties().getString(serviceType.getName());
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateSingleton(ServiceMetadata meta, Supplier<T> factory) {
        // Fast-path read (no locking)
        Object existing = singletonCache.get(meta);
        if (existing != null) {
            return (T) existing;
        }

        // Synchronize on the metadata instance to initialize atomically per service
        synchronized (meta) {
            existing = singletonCache.get(meta);
            if (existing != null) {
                return (T) existing;
            }

            T instance = factory.get();
            initializeSingleton(instance);
            singletonCache.put(meta, instance);
            return instance;
        }
    }

    public static void initializeSingleton(Object instance) {
        if (instance == null) return;
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(SingletonInitializer.class)) {
                if (method.getParameterCount() > 0) {
                    throw new ServiceException(
                        ServiceDiagnosticCode.INVALID_SINGLETON_INITIALIZER,
                        clazz.getName() + "." + method.getName(), "Method must take zero arguments"
                    );
                }
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new ServiceException(
                        cause,
                        DiagnosticCode.forException(cause),
                        clazz.getSimpleName() + "." + method.getName()
                    );
                } catch (Exception e) {
                    throw new ServiceException(
                        e,
                        DiagnosticCode.forException(e),
                        clazz.getSimpleName() + "." + method.getName()
                    );
                }
                break;
            }
        }
    }

    public void removeSingleton(ServiceMetadata meta) {
        singletonCache.remove(meta);
    }

    public void clearSingletons() {
        singletonCache.clear();
    }

    public TrackableArray<ServiceMetadata> getUserProviders() {
        return userProviders;
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
