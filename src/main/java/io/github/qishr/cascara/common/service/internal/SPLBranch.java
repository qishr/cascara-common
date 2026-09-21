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

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.UnimplementedMethodException;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.service.ContentTypeProvider;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.annotation.SingletonInitializer;
import io.github.qishr.cascara.common.diagnostic.DiagnosticLocalizer;
import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ClassHierarchy;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.ModulePath;

public class SPLBranch implements ServiceProviderLayer {
    protected static SPLRoot rootLayer;

    // TODO: These belong in the root layer...
    // protected static ContentTypeResolver contentTypeStore;
    // protected static Set<ContentType> contentTypes;

    protected Reporter reporter;

    protected boolean ownsReporter = false;
    protected boolean isBooting = false;

    protected String name;
    protected boolean isPublic;
    protected ModulePath modulePath;
    protected ModuleLayer moduleLayer;
    protected SPLBranch parent;

    protected List<Path> jarPaths = new ArrayList<>();
    protected List<SPLBranch> children = new ArrayList<>();
    protected Map<String,SPLBranch> namedChildren = new HashMap<>();

    protected List<ServiceMetadata> orderedProviders = new ArrayList<>();

    protected Map<String,ServiceMetadata> providersByFqcn = new HashMap<>();
    protected Map<String,ServiceMetadata> servicesByFqcn = new HashMap<>();
    protected Map<Class<ServiceProvider>, Set<ServiceMetadata>> providersByServiceType = new HashMap<>();

    protected SPLBranch() { }

    // public Set<ContentType> getContentTypes() {
    //     return contentTypes;
    // }

    /// Sets the reporter for communicating mapping warnings or errors in this layer.
    @Override
    public ServiceProviderLayer setReporter(Reporter reporter) {
        if (reporter == null) {
            reporter = new NoOpReporter();
        } else {
            this.reporter = reporter;
            this.ownsReporter = true;
        }
        return this;
    }

    //
    // Layer metadata, hierarchy, creation and deletion
    //

    @Override
    public String getName() { return name; }

    @Override
    public Path getModulePath(String name) { return modulePath.getPathForModule(name); }

    @Override
    public boolean isPublic() { return isPublic; }

    @Override
    public void setPublic(boolean v) { isPublic = v; }

    @Override
    public ServiceProviderLayer getParent() { return parent; }

    @Override
    public List<ServiceProviderLayer> getChildren() {
        return children.stream().map(layer -> {
            return (ServiceProviderLayer)layer;
        }).toList();
    }

    @Override
    public ServiceProviderLayer getChild(String name) { return namedChildren.get(name); }

    @Override
    public boolean hasChild(String name) { return namedChildren.containsKey(name); }

    @Override
    public Collection<ServiceMetadata> getProvidersByFqcn() { return providersByFqcn.values(); }

    //
    // Find in All Layers
    //

    /// Returns a list of all known service types.
    @Override
    public Set<Class<ServiceProvider>> findServiceTypes() {
        Set<Class<ServiceProvider>> found = new HashSet<>();
        found.addAll(getServiceTypes());
        for (SPLBranch layer : children) {
            found.addAll(layer.findServiceTypes());
        }
        return found;
    }

    @Override
    public Set<ServiceMetadata> findServices() {
        Set<ServiceMetadata> found = new HashSet<>();
        found.addAll(getServices());
        for (SPLBranch layer : children) {
            found.addAll(layer.findServices());
        }
        return found;
    }

    /// Retrieves metadata of the nearest known provider of the specified service type.
    @Override
    public ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType) {
        List<ServiceMetadata> all = internalFindAllProviders(serviceType, null, null);
        return all.isEmpty() ? null : all.getFirst();
    }

    /// Retrieves metadata of the nearest known provider whose capabilities satisfy the given predicate.
    @Override
    public ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate) {
        List<ServiceMetadata> all = internalFindAllProviders(serviceType, capabilityPredicate, null);
        return all.isEmpty() ? null : all.getFirst();
    }

    /// Retrieves metadata of all known providers of the specified service type.
    @Override
    public List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType) {
        return internalFindAllProviders(serviceType, null, null);
    }

    /// Retrieves metadata of all known providers whose capabilities satisfy the given predicate.
    @Override
    public List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate) {
        return internalFindAllProviders(serviceType, capabilityPredicate, null);
    }

    //
    // Get from Specific Layer
    //

    @Override
    public boolean hasProvider(String name) { return providersByFqcn.containsKey(name); }

    /// Retrieves metadata of the specified provider if it exists in this layer.
    @Override
    public ServiceMetadata getProvider(String providerName) {
        return providersByFqcn.get(providerName);
    }

    /// Retrieves metadata of providers of the specified service type in this layer.
    @Override
    public Collection<ServiceMetadata> getProviders() {
        return providersByFqcn.values();
    }

    /// Retrieves metadata of providers of the specified service type in this layer.
    @Override
    public List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType) {
        List<ServiceMetadata> found = new ArrayList<>();
        if (providersByServiceType.get(serviceType) != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    reportFinding(provider, 0);
                    found.add(provider);
                }
            }
        }
        return found;
    }

    /// Retrieves metadata of providers in this layer whose capabilities satisfy the given predicate.
    @Override
    public List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate) {
        List<ServiceMetadata> found = new ArrayList<>();
        if (providersByServiceType.get(serviceType) != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    if (capabilityPredicate.test(provider)) {
                        found.add(provider);
                        reportFinding(provider, 0);
                    }
                }
            }
        }
        return found;
    }

    //
    // Provider Registration in Specific Layer
    //

    @Override
    public SPLBranch create() {
        return create(null);
    }

    @Override
    public SPLBranch create(String name) {
        SPLBranch layer = new SPLBranch();
        layer.parent = this;
        children.add(layer);
        if (name != null) {
            layer.name = name;
            namedChildren.put(name, layer);
        }
        return layer;
    }

    @Override
    public void remove(String layerName) {
        for (SPLBranch layer : children) {
            if (layer.getName().equals(layerName)) {
                children.remove(layer);
                namedChildren.remove(layerName);
                return;
            }
        }
        ClassHierarchy.invalidate();
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public void registerModule(Module module) {
        String moduleName = module.getName();
        if (moduleName.startsWith("java.") ||
            moduleName.startsWith("javax.") ||
            moduleName.startsWith("jdk.") ||
            moduleName.startsWith("jfx.") ||
            moduleName.startsWith("javafx.")) {
            // These modules will never contain a Cascara ServiceProvider
            return;
        }

        getReporter().trace("Checking " + moduleName); // TODO: Version check
        ClassLoader classLoader = module.getClassLoader();
        ModuleDescriptor desc = module.getDescriptor();
        Set<Provides> services = desc.provides();

        if (!services.isEmpty()) {
            getReporter().debug("Discovering providers in " + moduleName);
            for (Provides service : services) {
                for (String providerClassName : service.providers()) {
                    getReporter().trace("  Attempting to reigster " + providerClassName);
                    try {
                        Class<?> type = classLoader.loadClass(providerClassName);
                        registerClass((Class)type);
                    } catch (ClassNotFoundException | ServiceException e) {
                        getReporter().warn(ServiceDiagnosticCode.FAILED_TO_LOAD_CLASS, providerClassName, e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void registerClass(Class<?> type) {
        if (type == null || !ServiceProvider.class.isAssignableFrom(type)) {
            return;
        }
        String providerFqcn = type.getName();
        if (!isRegisteres(providerFqcn)) {
            ServiceProvider instance = (ServiceProvider) ServiceProviderLayer.instantiateProvider(type);
            registerProvider(instance, null);
            if (isBooting) {
                rootLayer.bootProviders.add(providerFqcn);
            }
        }
    }

    @Override
    public void registerJar(Path jarPath) {
        String moduleName;
        JarManifest manifest;
        try (JarFile jar = JarFile.open(jarPath)) {
            moduleName = jar.getModuleName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new ServiceException(ServiceDiagnosticCode.NON_MODULAR_JAR, jarPath);
            }
            manifest = jar.getManifest();
        } catch (Exception e) {
            throw new ServiceException(e, ServiceDiagnosticCode.FAILED_TO_READ_JAR, jarPath, e.getMessage());
        }

        SemVer minVersion = new SemVer(manifest.getString("Min-Cascara-Version", "0.0.0"));
        SemVer cascaraVersion = Cascara.getVersion();

        if (cascaraVersion.isLowerThan(minVersion)) {
            throw new ServiceException(
                ServiceDiagnosticCode.INCOMPATIBLE_MODULE_VERSION,
                moduleName, minVersion, cascaraVersion
            );
        }

        getReporter().debug("Discovering providers in \"%s\"", jarPath);

        jarPaths.add(jarPath);
        String paths = String.join(":", getJarPathStrings());

        modulePath = new ModulePath(paths, jarPath);

        // 1. Create a finder for modules in this layer
        Path[] jarPathsArray = getJarPaths();
        ModuleFinder finder = ModuleFinder.of(jarPathsArray);

        // 2. Resolve the module(s) found against the current boot layer
        Set<String> roots = finder.findAll().stream()
                .map(m -> m.descriptor().name())
                .collect(Collectors.toSet());

        ModuleLayer parent = ModuleLayer.boot();
        Configuration cf = parent.configuration().resolve(finder, ModuleFinder.of(), roots);

        // 3. (re-)create the layer.
        moduleLayer = parent.defineModulesWithManyLoaders(cf, ClassLoader.getSystemClassLoader());

        enumerateProviders();
        ClassHierarchy.invalidate();
    }

    //
    // Private Methods
    //

    protected void registerViaServiceLoader() {
        try {
            ServiceLoader<ServiceProvider> loader = ServiceLoader.load(ServiceProvider.class);
            for (ServiceProvider provider : loader) {
                if (!providersByFqcn.containsKey(provider.getClass().getName())) {
                    registerProvider(provider, null);
                }
            }
        } catch (ServiceConfigurationError e) {
            bootError(e, ServiceDiagnosticCode.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    /// Returns the Reporter of this layer or the nearest ancetor that has one.
    private Reporter getReporter() {
        if (ownsReporter || parent == null) { return reporter; }
        return parent.getReporter();
    }

    /// Use SPI to find the service implementations inside this layer
    private void enumerateProviders() {
        // providersByFqcn.clear();
        // providersByServiceType.clear();
        var loader = ServiceLoader.load(moduleLayer, ServiceProvider.class);
        loader.forEach(provider -> {
            if (!isRegisteres(provider.getClass().getName())) {
                String moduleName = provider.getClass().getModule().getName();
                Path jarPath = modulePath.getPathForModule(moduleName);
                try {
                    registerProvider(provider, jarPath);
                } catch (Exception e) {
                    registrationError("Failed to query module " + moduleName + ".", null, e);
                } catch(AbstractMethodError e) {
                    registrationError("Incompatible module.", jarPath, e);
                } catch (NoClassDefFoundError e) {
                    registrationError("Incompatible module.", jarPath, e);
                } catch (ServiceConfigurationError e) {
                    registrationError("Incompatible module.", jarPath, e);
                }
            }
        });
    }

    private boolean isRegisteres(String providerFqcn) {
        return rootLayer.bootProviders.contains(providerFqcn) || providersByFqcn.containsKey(providerFqcn);
    }

    private void registerProvider(ServiceProvider instance, Path jarPath) {
        getReporter().trace("  Registering %s", instance.getClass().getName());
        try {
            Class<? extends ServiceProvider> providerClass = instance.getClass();

            List<Class<ServiceProvider>> interfaceHierarchy = new ArrayList<>();

            if (collectCascaraModuleInterfaces(providerClass, interfaceHierarchy)) {

                // Experimental:
                // Store the rich ContentTypes that services support
                ContentType contentType = null;
                if (instance instanceof ContentTypeProvider ctp) {
                    contentType = ctp.getContentType();
                    rootLayer.storeContentType(contentType);
                }

                boolean isSingleton = false;
                List<Method> methods = JreUtils.getAllMethods(providerClass);
                for (Method method : methods) {
                    if (method.isAnnotationPresent(SingletonInitializer.class)) {
                        isSingleton = true;
                    }
                }

                ServiceMetadata provider = new ServiceMetadata(providerClass, getProviderProperties(instance, jarPath), contentType, isSingleton);

                orderedProviders.add(provider);
                providersByFqcn.put(providerClass.getName(), provider);

                for (Class<ServiceProvider> serviceInterface : interfaceHierarchy) {

                    ServiceMetadata service = new ServiceMetadata(serviceInterface, getServiceProperties(serviceInterface));
                    servicesByFqcn.put(serviceInterface.getName(), service);

                    Set<ServiceMetadata> providers = providersByServiceType.get(serviceInterface);
                    if (providers == null) {
                        providers = new HashSet<>();
                        providersByServiceType.put(serviceInterface, providers);
                    }
                    providers.add(provider);
                }

                if (contentType == null) {
                    getReporter().debug("  Registered " + providerClass.getName());
                } else {
                    getReporter().debug("  Registered " + providerClass.getName() + " with content types:");
                    for (String type : contentType.getMimeTypes()) {
                        getReporter().debug("    " + type);
                    }
                }

                rootLayer.getUserProviders().add(provider);
            }
        } catch(AbstractMethodError e) {
            registrationError("Incompatible module: " + instance.getClass().getName() + ".", jarPath, e);
        } catch (NoClassDefFoundError e) {
            registrationError("Incompatible module: " + instance.getClass().getName() + ".", jarPath, e);
        } catch (ServiceConfigurationError e) {
            registrationError("Incompatible module: " + instance.getClass().getName() + ".", jarPath, e);
        }
    }

    private Properties getServiceProperties(Class<ServiceProvider> serviceInterface) {
        Properties properties = new Properties();
        setModuleProperties(properties, serviceInterface);
        properties.set("serviceName", serviceInterface.getSimpleName());
        return properties;
    }

    private Properties getProviderProperties(ServiceProvider provider, Path jarPath) {
        Properties properties = new Properties();
        setModuleProperties(properties, provider.getClass());
        if (jarPath != null) {
            properties.set("jarPath", jarPath.toString());
        }
        properties.set("providerName", provider.getClass().getSimpleName());
        Properties declaredCapabilities = provider.getServiceProperties();
        if (declaredCapabilities != null) {
            properties.addAll(declaredCapabilities);
        }
        return properties;
    }

    private void setModuleProperties(Properties properties, Class<?> type) {
        Module module = type.getModule();
        properties.set("moduleName", module.getName());
        ModuleDescriptor descriptor = module.getDescriptor();
        if (descriptor != null) {
            descriptor.rawVersion().ifPresent(moduleVersion -> {
                properties.set("moduleVersion", moduleVersion);
            });
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean collectCascaraModuleInterfaces(Class<?> type, List<Class<ServiceProvider>> collected) {
        if (type.equals(ServiceProvider.class)) {
            return true;
        }

        // Check all interfaces
        boolean found = false;
        List<Class<?>> interfaces = List.of(type.getInterfaces());
        for (Class<?> interfaceType : interfaces) {
            if (ServiceProvider.class.isAssignableFrom(interfaceType)) {
                if (collectCascaraModuleInterfaces(interfaceType, collected)) {
                    collected.add((Class)interfaceType);
                    found = true;
                }
            }
        }

        // Check the superclass, if any
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            if (ServiceProvider.class.isAssignableFrom(superClass)) {
                if (collectCascaraModuleInterfaces(superClass, collected)) {
                    found = true;
                }
            }
        }

        return found;
    }

    private void registrationError(String message, Path location, Throwable t) {
        String logMessage = message;
        if (location != null) {
            logMessage = logMessage + " " + location;
        }
        if (t != null) {
            logMessage = logMessage + " " + t.getMessage();
        }
        getReporter().error(GenericDiagnosticCode.ERROR, logMessage);
    }

    private Path[] getJarPaths() {
        return jarPaths.toArray(new Path[]{});
    }

    private String[] getJarPathStrings() {
        String[] strings = new String[jarPaths.size()];
        for (int i = 0; i < jarPaths.size(); i++) {
            strings[i] = jarPaths.get(i).toString();
        }
        return strings;
    }

    private List<ServiceMetadata> internalFindAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate, SPLBranch previous) {
        String startLayer = (name == null ? "unnamed layer" : "layer " + name);
        getReporter().debug("Searching for " + serviceType.getSimpleName() + " starting at " + startLayer);
        List<ServiceMetadata> found = new ArrayList<>();

        if (providersByServiceType.get(serviceType) != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    if (capabilityPredicate == null) {
                        found.add(provider);
                        reportFinding(provider, 0);
                    } else {
                        if (capabilityPredicate.test(provider)) {
                            found.add(provider);
                            reportFinding(provider, 0);
                        }
                    }
                }
            }
        }

        if (parent == null) {
            // Branch out from root. `previous` is used to avoid going down the branch we just came from
            for (SPLBranch layer : children) {
                if (layer != previous && layer.isPublic) {
                    found.addAll(layer.findProvidersInBranches(serviceType, capabilityPredicate, 0));
                }
            }
        } else if (parent != previous) {
            // Go towards root
            getReporter().trace("⬆ " + parent.name);
            found.addAll(parent.internalFindAllProviders(serviceType, capabilityPredicate, this));
        }

        return found;
    }

    private List<ServiceMetadata> findProvidersInBranches(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate, int depth) {
        List<ServiceMetadata> found = new ArrayList<>();
        getReporter().trace("" + "  ".repeat(depth) + "⬇ " + name);

        if (providersByServiceType.get(serviceType) != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    if (capabilityPredicate == null) {
                        found.add(provider);
                        reportFinding(provider, depth);
                    } else {
                        if (capabilityPredicate.test(provider)) {
                            found.add(provider);
                            reportFinding(provider, depth);
                        }
                    }
                }
            }
        }

        for (SPLBranch layer : children) {
            if (layer.isPublic) {
                found.addAll(layer.findProvidersInBranches(serviceType, capabilityPredicate, depth + 1));
            }
        }

        return found;
    }

    /// Returns a list of service types in this layer.
    private Collection<Class<ServiceProvider>> getServiceTypes() {
        return providersByServiceType.keySet();
    }

    private Collection<ServiceMetadata> getServices() {
        return servicesByFqcn.values();
    }

    //
    // Diagnostics
    //

    protected void reportFinding(ServiceMetadata item, int depth) {
        getReporter().debug("[" + name + "] " + "  ".repeat(depth) + item.getType().getName() +
            (item.getJarPath() == null ? "" : " from " + item.getJarPath()));
    }

    protected static void bootError(Throwable e, ServiceDiagnosticCode code, Object... details) {
        final Reporter reporter = rootLayer.reporter;
        if (reporter.isSilent()) {
            System.err.println(
                DiagnosticLocalizer.DEFAULT.format(code, details)
            );
        } else {
            reporter.error(e, code, details);
        }
    }

    public void setParent(ServiceProviderLayer parent) {
        throw new UnimplementedMethodException();
    }
}
