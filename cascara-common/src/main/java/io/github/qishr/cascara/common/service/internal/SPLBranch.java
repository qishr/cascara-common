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
import java.lang.reflect.InvocationTargetException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.annotation.SingletonInitializer;
import io.github.qishr.cascara.common.diagnostic.DiagnosticLocalizer;
import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.UnimplementedMethodException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.service.ContentTypeProvider;
import io.github.qishr.cascara.common.service.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.trackable.TrackableArray;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ClassHierarchy;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.ModulePath;

public class SPLBranch implements ServiceProviderLayer {
    protected static SPLRoot rootLayer;

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

    private final Map<String, Object> singletonCache = new ConcurrentHashMap<>();

    Set<SPLBranch> visibleLayers = new HashSet<>();
    final TrackableArray<ServiceMetadata> visibleProviders = new TrackableArray<>();
    private final TrackableArray<ServiceMetadata> declaredProviders = new TrackableArray<>();

    // TODO: Remove modules from list when they're no longer loaded
    private TrackableArray<String> modules = new TrackableArray<>();

    protected SPLBranch() { }

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

    public TrackableArray<String> getModules() {
        return modules;
    }

    public TrackableArray<ServiceMetadata> getDeclaredProviders() {
        return declaredProviders;
    }

    public TrackableArray<ServiceMetadata> getVisibleProviders() {
        return visibleProviders;
    }

    @Override
    public boolean isPublic() { return isPublic; }

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

    public Set<SPLBranch> ancestors() {
        Set<SPLBranch> ancestors = new HashSet<>();
        SPLBranch ancestor = this.parent;
        while (ancestor != null) {
            ancestors.add(ancestor);
            ancestor = ancestor.parent;
        }
        return ancestors;
    }

    public Set<SPLBranch> publicSiblings() {
        Set<SPLBranch> siblings = new HashSet<>();
        if (parent != null) {
            for (SPLBranch sibling : parent.children) {
                if (sibling != this && sibling.isPublic) {
                    siblings.add(sibling);
                }
            }
        }
        return siblings;
    }

    public Set<SPLBranch> publicDescendants() {
        Set<SPLBranch> collected = new HashSet<>();
        collectPublicDescendants(this, collected);
        return collected;
    }

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

    @Override
    public Collection<ServiceMetadata> getProvidersByFqcn() { return providersByFqcn.values(); }

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
    // Neo-singletons
    //

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateSingleton(ServiceMetadata meta, Supplier<T> factory) {
        // Fast-path read (no locking)
        String singletonClassName = meta.getTypeName();
        Object existing = singletonCache.get(singletonClassName);
        if (existing != null) {
            return (T) existing;
        }

        // Synchronize on the metadata instance to initialize atomically per service
        synchronized (meta) {
            existing = singletonCache.get(singletonClassName);
            if (existing != null) {
                return (T) existing;
            }

            T instance = factory.get();
            initializeSingleton(instance);
            singletonCache.put(singletonClassName, instance);
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
        singletonCache.remove(meta.getTypeName());
    }

    //
    // Provider Registration in Specific Layer
    //

    @Override
    public SPLBranch create() {
        return createInternal(null, true);
    }

    @Override
    public SPLBranch create(String name) {
        return createInternal(name, true);
    }

    @Override
    public SPLBranch createPrivate(String name) {
        return createInternal(name, false);
    }

    @Override
    public void remove(String name) {
        SPLBranch layerToRemove = namedChildren.get(name);
        if (layerToRemove == null) {
            return;
        }
        removeInternal(layerToRemove);
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
        ClassHierarchy.invalidate();
    }

    //
    // Provider Registration in Specific Layer
    //

    @SuppressWarnings({ "rawtypes" })
    @Override
    public void registerModule(Module module) {
        String moduleName = module.getName();

        // These modules will never contain a Cascara ServiceProvider
        if (moduleName.startsWith("java.") ||
            moduleName.startsWith("javax.") ||
            moduleName.startsWith("jdk.") ||
            moduleName.startsWith("jfx.") ||
            moduleName.startsWith("javafx.")) {
            return;
        }

        getReporter().trace("Checking " + moduleName); // TODO: Version check
        ClassLoader classLoader = module.getClassLoader();
        ModuleDescriptor desc = module.getDescriptor();

        // TODO: Get versions from module's manifest
        // SemVer moduleBuildCascaraVersion = new SemVer(manifest.getString("Cascara-Version", "0.0.0"));
        // SemVer moduleMinCascaraVersion = new SemVer(manifest.getString("Min-Cascara-Version", moduleBuildCascaraVersion.toString()));
        // verifyModuleVersionCompatibility(moduleName, moduleBuildCascaraVersion, moduleMinCascaraVersion);

        Set<Provides> services = desc.provides();

        if (!services.isEmpty()) {
            getReporter().debug("Discovering providers in " + moduleName);
            for (Provides service : services) {
                for (String providerClassName : service.providers()) {
                    getReporter().trace("  Attempting to reigster " + providerClassName);
                    try {
                        Class<?> type = classLoader.loadClass(providerClassName);
                        registerClassInternal((Class)type);
                    } catch (ServiceException e) {
                        getReporter().trace("Class \"" + providerClassName + "\" is not a Cascara ServiceProvider");
                    } catch (ClassNotFoundException e) {
                        getReporter().warn(ServiceDiagnosticCode.FAILED_TO_LOAD_CLASS, providerClassName, e.getMessage());
                    }
                }
            }
        }
        addModuleToMap(module.getName());
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
    }

    @Override
    public void registerClass(Class<?> type) {
        if (type == null || !ServiceProvider.class.isAssignableFrom(type)) {
            return;
        }
        registerClassInternal(type);
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
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

        SemVer moduleBuildCascaraVersion = new SemVer(manifest.getString("Cascara-Version", "0.0.0"));
        SemVer moduleMinCascaraVersion = new SemVer(manifest.getString("Min-Cascara-Version", moduleBuildCascaraVersion.toString()));
        verifyModuleVersionCompatibility(moduleName, moduleBuildCascaraVersion, moduleMinCascaraVersion);

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
        // moduleLayer = parent.defineModulesWithOneLoader(cf, ClassLoader.getSystemClassLoader());

        enumerateProviders();
        addModuleToMap(moduleName);
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
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

    private SPLBranch createInternal(String name, boolean isPublic) {
        SPLBranch layer = new SPLBranch();
        layer.parent = this;
        layer.isPublic = isPublic;
        children.add(layer);
        if (name != null) {
            layer.name = name;
            namedChildren.put(name, layer);
        }
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
        return layer;
    }

    /// Recursively removes a child layer and all descendant layers.
    private void removeInternal(SPLBranch layerToRemove) {
        for (SPLBranch childLayer : layerToRemove.children) {
            removeInternal(childLayer);
        }
        layerToRemove.delete();
        children.remove(layerToRemove);
        namedChildren.remove(layerToRemove.getName());
    }

    /// Removes everything allocated in this layer
    private void delete() {
        List<String> modulesCopy = new ArrayList<>(modules);
        for (String moduleName : modulesCopy) {
            removeModuleFromMap(moduleName);
        }
    }

    private void collectPublicDescendants(SPLBranch layer, Set<SPLBranch> collected) {
        for (SPLBranch descendant : layer.children) {
            if (descendant.isPublic) {
                collected.add(descendant);
                collectPublicDescendants(descendant, collected);
            }
        }
    }

    private void registerClassInternal(Class<?> type) {
        String providerFqcn = type.getName();
        if (!isRegistered(providerFqcn)) {
            ServiceProvider instance = (ServiceProvider) SPLUtils.instantiate(type);
            registerProvider(instance, null);
            if (isBooting) {
                rootLayer.bootProviders.add(providerFqcn);
            }
        }
    }

    private void verifyModuleVersionCompatibility(String moduleName, SemVer moduleBuildCascaraVersion, SemVer moduleMinCascaraVersion) {
        SemVer activeCascaraVersion = Cascara.getVersion();

        boolean hasMinVersion = !"0.0.0".equals(moduleMinCascaraVersion.toString());
        boolean hasBuildVersion = !"0.0.0".equals(moduleBuildCascaraVersion.toString());

        if (
            (hasMinVersion && (
                activeCascaraVersion.isLowerThan(moduleMinCascaraVersion) ||
                activeCascaraVersion.getMajor() != moduleMinCascaraVersion.getMajor()
            )) ||
            (hasBuildVersion && activeCascaraVersion.getMajor() != moduleBuildCascaraVersion.getMajor())
        ) {
            System.out.println("activeCascaraVersion: " + activeCascaraVersion);
            System.out.println("moduleBuildCascaraVersion: " + moduleBuildCascaraVersion);
            System.out.println("moduleMinCascaraVersion: " + moduleMinCascaraVersion);
            throw new ServiceException(
                ServiceDiagnosticCode.INCOMPATIBLE_MODULE_VERSION,
                moduleName, moduleMinCascaraVersion, activeCascaraVersion
            );
        }
    }

    /// Adds a module to the root layer's `moduleToLayers` map and updates
    // the `modules` `TrackableArray`
    private void addModuleToMap(String moduleName) {
        Set<ServiceProviderLayer> layers = rootLayer.moduleToLayers.get(moduleName);
        if (layers == null) {
            layers = new HashSet<>();
            rootLayer.moduleToLayers.put(moduleName, layers);
        }
        layers.add(this);
        if (!modules.contains(moduleName)) {
            modules.add(moduleName);
        }
    }

    /// Removes a module from the root layer's `moduleToLayers` map and updates
    // the `modules` `TrackableArray`
    private void removeModuleFromMap(String moduleName) {
        Set<ServiceProviderLayer> layers = rootLayer.moduleToLayers.get(moduleName);
        if (layers != null) {
            layers.remove(this);
            if (layers.isEmpty()) {
                rootLayer.moduleToLayers.remove(layers);
            }
        }
        modules.remove(moduleName);
    }

    /// Returns the Reporter of this layer or the nearest ancetor that has one.
    private Reporter getReporter() {
        if (ownsReporter || parent == null) { return reporter; }
        return parent.getReporter();
    }

    /// Use SPI to find the service implementations inside this layer
    private void enumerateProviders() {
        var loader = ServiceLoader.load(moduleLayer, ServiceProvider.class);
        loader.forEach(provider -> {
            if (!isRegistered(provider.getClass().getName())) {
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

    private boolean isRegistered(String providerFqcn) {
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

                ServiceMetadata provider = new ServiceMetadata(this, providerClass, getProviderProperties(instance, jarPath), contentType, isSingleton);

                orderedProviders.add(provider);
                providersByFqcn.put(providerClass.getName(), provider);

                for (Class<ServiceProvider> serviceInterface : interfaceHierarchy) {

                    ServiceMetadata service = new ServiceMetadata(this, serviceInterface, getServiceProperties(serviceInterface), null, false);
                    servicesByFqcn.put(serviceInterface.getName(), service);

                    Set<ServiceMetadata> providers = providersByServiceType.get(serviceInterface);
                    if (providers == null) {
                        providers = new HashSet<>();
                        providersByServiceType.put(serviceInterface, providers);



                        // providersByServiceFqcn.put(serviceInterface.getName(), providers);



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

                declaredProviders.add(provider);
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

    //
    // Hierarchy Traversal Code
    //

    private List<ServiceMetadata> internalFindAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate, SPLBranch previous) {
        String startLayer = (name == null ? "unnamed layer" : "layer " + name);
        getReporter().debug("Searching for " + serviceType.getSimpleName() + " starting at " + startLayer);
        List<ServiceMetadata> found = new ArrayList<>();

        addAllToList(findProvidersInBranches(serviceType, capabilityPredicate, 0), found);

        if (parent != null && parent != previous) {
            // Go towards root
            getReporter().trace("⬆ " + parent.name);
            addAllToList(parent.internalFindAllProviders(serviceType, capabilityPredicate, this), found);
        }

        return found;
    }

    private List<ServiceMetadata> findProvidersInBranches(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate, int depth) {
        List<ServiceMetadata> found = new ArrayList<>();
        getReporter().trace("" + "  ".repeat(depth) + "⬇ " + name);

        Set<ServiceMetadata> byType = providersByServiceType.get(serviceType);
        // Set<ServiceMetadata> byFqcn = providersByServiceFqcn.get(serviceType.getName());

        if (byType != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    if (!found.contains(provider)) {
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
        }

        for (SPLBranch layer : children) {
            if (layer.isPublic) {
                addAllToList(layer.findProvidersInBranches(serviceType, capabilityPredicate, depth + 1), found);
            }
        }

        return found;
    }

    private void addAllToList(List<ServiceMetadata> add, List<ServiceMetadata> list) {
        for (ServiceMetadata item : add) {
            if (!list.contains(item)) {
                list.add(item);
            }
        }
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
        getReporter().debug("[provider] " + "layer=\"" + name + "\", class=\"" + item.getType().getName() + "\"" +
            (item.getJarPath() == null ? "" : ", jar=\"" + item.getJarPath() + "\""));
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
