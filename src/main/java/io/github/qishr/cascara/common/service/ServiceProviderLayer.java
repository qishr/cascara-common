package io.github.qishr.cascara.common.service;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.ModulePath;
import io.github.qishr.cascara.common.util.Properties;

public class ServiceProviderLayer {
    private static ServiceProviderLayer rootLayer;

    private Reporter reporter;

    private boolean ownsReporter = false;

    private String name;
    private boolean isPublic;
    private ModulePath modulePath;
    private ModuleLayer moduleLayer;
    private ServiceProviderLayer parent;

    private List<Path> jarPaths = new ArrayList<>();
    private List<ServiceProviderLayer> children = new ArrayList<>();
    private Map<String,ServiceProviderLayer> namedChildren = new HashMap<>();

    private List<ServiceMetadata> orderedProviders = new ArrayList<>();

    private Map<String,ServiceMetadata> providersByFqcn = new HashMap<>();
    private Map<String,ServiceMetadata> servicesByFqcn = new HashMap<>();
    private Map<Class<ServiceProvider>, Set<ServiceMetadata>> providersByServiceType = new HashMap<>();

    private ServiceProviderLayer() { }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured.
    public static ServiceProviderLayer getRootLayer() {
        return getRootLayer(null);
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured with a specified Reporter.
    /// This reporter is used for non-fatal error and warning reporting.
    public static ServiceProviderLayer getRootLayer(Reporter reporter) {
        if (reporter == null) {
            reporter = new NoOpReporter();
        }
        if (rootLayer == null) {
            rootLayer = new ServiceProviderLayer();
            rootLayer.name = "root";
            rootLayer.setReporter(reporter);
            ModuleLayer boot = ModuleLayer.boot();
            boot.modules().forEach((module) -> {
                rootLayer.registerModule(module);
            });
        }
        return rootLayer;
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadProvider(Class<T> providerClass) {
        if (!ServiceProvider.class.isAssignableFrom(providerClass)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, providerClass);
        }
        try {
            Constructor<?> constructor = providerClass.getDeclaredConstructor();
            if (constructor == null) {
                throw new ServiceException(ServiceDiagnosticCode.NOARGS_CONSTRUCTOR_REQUIRED, providerClass.getName());
            } else {
                ServiceProvider instance = (ServiceProvider) constructor.newInstance();
                return (T)instance;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new ServiceException(e, ServiceDiagnosticCode.FAILED_TO_INSTANTIATE_CLASS, providerClass.getName(), e.getMessage());
        }
    }

    public static <T> T loadProvider(Class<T> serviceType, ServiceMetadata metadata) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }
        Class<? extends ServiceProvider> clazz = metadata.getType();
        return serviceType.cast(loadProvider(clazz));
    }

    // TODO: Don't just pick the first one, pick one that's declared in a Cascara module
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T loadDefault(Class<T> serviceType) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException(ServiceDiagnosticCode.NOT_A_SERVICE_PROVIDER, serviceType);
        }
        List<ServiceMetadata> providers = getRootLayer().findAllProviders((Class)serviceType);
        if (providers.isEmpty()) {
            throw new ServiceException(ServiceDiagnosticCode.NO_PROVIDER_REGISTERED, serviceType.getSimpleName());
        }
        ServiceMetadata meta = providers.getFirst();
        Class<T> clazz = (Class<T> )meta.getType();
        return loadProvider(clazz);
    }

    //
    // All Layers - TODO: These should be cached
    //

    /// Returns a list of all known service types.
    public Set<Class<ServiceProvider>> findServiceTypes() {
        Set<Class<ServiceProvider>> found = new HashSet<>();
        found.addAll(getServiceTypes());
        for (ServiceProviderLayer layer : children) {
            found.addAll(layer.findServiceTypes());
        }
        return found;
    }

    public Set<ServiceMetadata> findServices() {
        Set<ServiceMetadata> found = new HashSet<>();
        found.addAll(getServices());
        for (ServiceProviderLayer layer : children) {
            found.addAll(layer.findServices());
        }
        return found;
    }

    /// Retrieves metadata of the nearest known provider of the specified service type.
    public ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType) {
        List<ServiceMetadata> all = internalFindAllProviders(serviceType, null, null);
        return all.isEmpty() ? null : all.getFirst();
    }

    /// Retrieves metadata of the nearest known provider whose capabilities satisfy the given predicate.
    public ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<Properties> capabilityPredicate) {
        List<ServiceMetadata> all = internalFindAllProviders(serviceType, capabilityPredicate, null);
        return all.isEmpty() ? null : all.getFirst();
    }

    /// Retrieves metadata of all known providers of the specified service type.
    public List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType) {
        return internalFindAllProviders(serviceType, null, null);
    }

    /// Retrieves metadata of all known providers whose capabilities satisfy the given predicate.
    public List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<Properties> capabilityPredicate) {
        return internalFindAllProviders(serviceType, capabilityPredicate, null);
    }

    //
    // This Layer
    //

    public String getName() { return name; }

    public ServiceProviderLayer getParent() { return parent; }

    public Collection<ServiceProviderLayer> getChildren() { return children; }

    public ServiceProviderLayer getChild(String name) { return namedChildren.get(name); }

    public boolean hasChild(String name) { return namedChildren.containsKey(name); }

    public boolean hasProvider(String name) { return providersByFqcn.containsKey(name); }

    public Collection<ServiceMetadata> getProvidersByFqcn() { return providersByFqcn.values(); }

    public Path getModulePath(String name) { return modulePath.getPathForModule(name); }

    public boolean isPublic() { return isPublic; }

    public void setPublic(boolean v) { isPublic = v; }

    /// Sets the reporter for communicating mapping warnings or errors in this layer.
    public ServiceProviderLayer setReporter(Reporter reporter) {
        if (reporter == null) {
            reporter = new NoOpReporter();
        } else {
            this.reporter = reporter;
            this.ownsReporter = true;
        }
        return this;
    }

    /// Retrieves metadata of the specified provider if it exists in this layer.
    public ServiceMetadata getProvider(String providerName) {
        return providersByFqcn.get(providerName);
    }

    /// Retrieves metadata of providers of the specified service type in this layer.
    public Collection<ServiceMetadata> getProviders() {
        return providersByFqcn.values();
    }

    /// Retrieves metadata of providers of the specified service type in this layer.
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
    public List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<Properties> capabilityPredicate) {
        List<ServiceMetadata> found = new ArrayList<>();
        if (providersByServiceType.get(serviceType) != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    if (capabilityPredicate.test(provider.getProperties())) {
                        found.add(provider);
                        reportFinding(provider, 0);
                    }
                }
            }
        }
        return found;
    }

    //
    // Registration
    //

    public ServiceProviderLayer create() {
        return create(null);
    }

    public ServiceProviderLayer create(String name) {
        ServiceProviderLayer layer = new ServiceProviderLayer();
        layer.parent = this;
        children.add(layer);
        if (name != null) {
            layer.name = name;
            namedChildren.put(name, layer);
        }
        return layer;
    }

    public void remove(String layerName) {
        for (ServiceProviderLayer layer : children) {
            if (layer.getName().equals(layerName)) {
                children.remove(layer);
                namedChildren.remove(layerName);
                return;
            }
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public void registerModule(Module module) {
        String moduleName = module.getName();
        if (moduleName.startsWith("java.") ||
            moduleName.startsWith("javax.") ||
            moduleName.startsWith("jdk.")) {
            // These modules will never contain a Cascara ServiceProvider
            return;
        }

        ClassLoader classLoader = module.getClassLoader();

        ModuleDescriptor desc = module.getDescriptor();
        for (Provides provides : desc.provides()) {
            for (String providerClassName : provides.providers()) {
                try {
                    Class<?> type = classLoader.loadClass(providerClassName);
                    registerClass((Class)type);
                } catch (ClassNotFoundException | ServiceException e) {
                    getReporter().warn(ServiceDiagnosticCode.FAILED_TO_LOAD_CLASS, providerClassName, e.getMessage());
                }
            }
        }
    }

    public void registerClass(Class<?> type) {
        if (type == null || !ServiceProvider.class.isAssignableFrom(type)) {
            return;
        }
        ServiceProvider instance = (ServiceProvider) loadProvider(type);
        registerProvider(instance, null);
    }

    public void registerJar(Path jarPath) {
        String moduleName;

        try {
            JarFile jar = JarFile.load(jarPath);
            moduleName = jar.getModuleName();
        } catch (LocalizableIOException e) {
            throw new ServiceException(e, ServiceDiagnosticCode.FAILED_TO_READ_JAR, jarPath, e.getMessage());
        }

        if (moduleName == null || moduleName.isEmpty()) {
            throw new ServiceException(ServiceDiagnosticCode.NON_MODULAR_JAR, jarPath);
        }

        getReporter().debug("Discovering providers in \"%s\"", jarPath);

        jarPaths.add(jarPath);
        String paths = String.join(":", getJarStrings());
        modulePath = new ModulePath(paths);

        // 1. Create a finder for modules in this layer
        ModuleFinder finder = ModuleFinder.of(getJarPaths());

        // 2. Resolve the module(s) found against the current boot layer
        Set<String> roots = finder.findAll().stream()
                .map(m -> m.descriptor().name())
                .collect(Collectors.toSet());

        ModuleLayer parent = ModuleLayer.boot();
        Configuration cf = parent.configuration().resolve(finder, ModuleFinder.of(), roots);

        // 3. (re-)create the layer.
        moduleLayer = parent.defineModulesWithManyLoaders(cf, ClassLoader.getSystemClassLoader());

        enumerateProviders();
    }

    //
    // Private Methods
    //

    /// Returns the Reporter of this layer or the nearest ancetor that has one.
    private Reporter getReporter() {
        if (ownsReporter || parent == null) { return reporter; }
        return parent.getReporter();
    }

    /// Use SPI to find the service implementations inside this layer
    private void enumerateProviders() {
        providersByFqcn.clear();
        providersByServiceType.clear();
        var loader = ServiceLoader.load(moduleLayer, ServiceProvider.class);
        loader.forEach(provider -> {
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
        });
    }

    private void registerProvider(ServiceProvider instance, Path jarPath) {
        getReporter().debug("Registering \"%s\"", instance.getClass().getName());
        try {
            Class<? extends ServiceProvider> providerClass = instance.getClass();

            List<Class<ServiceProvider>> interfaceHierarchy = new ArrayList<>();

            if (collectCascaraModuleInterfaces(providerClass, interfaceHierarchy)) {
                ServiceMetadata provider = new ServiceMetadata(providerClass, getProviderProperties(instance, jarPath));

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

    private String[] getJarStrings() {
        String[] strings = new String[jarPaths.size()];
        for (int i = 0; i < jarPaths.size(); i++) {
            strings[i] = jarPaths.get(i).toString();
        }
        return strings;
    }

    private List<ServiceMetadata> internalFindAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<Properties> capabilityPredicate, ServiceProviderLayer previous) {
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
                        if (capabilityPredicate.test(provider.getProperties())) {
                            found.add(provider);
                            reportFinding(provider, 0);
                        }
                    }
                }
            }
        }

        if (parent == null) {
            // Branch out from root. `previous` is used to avoid going down the branch we just came from
            for (ServiceProviderLayer layer : children) {
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

    private List<ServiceMetadata> findProvidersInBranches(Class<? extends ServiceProvider> serviceType, Predicate<Properties> capabilityPredicate, int depth) {
        List<ServiceMetadata> found = new ArrayList<>();
        getReporter().trace("" + "  ".repeat(depth) + "⬇ " + name);

        if (providersByServiceType.get(serviceType) != null) {
            for (ServiceMetadata provider : orderedProviders) {
                if (serviceType.isAssignableFrom(provider.getType())) {
                    if (capabilityPredicate == null) {
                        found.add(provider);
                        reportFinding(provider, depth);
                    } else {
                        if (capabilityPredicate.test(provider.getProperties())) {
                            found.add(provider);
                            reportFinding(provider, depth);
                        }
                    }
                }
            }
        }

        for (ServiceProviderLayer layer : children) {
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

    private void reportFinding(ServiceMetadata item, int depth) {
        getReporter().debug("[" + name + "] " + "  ".repeat(depth) + item.getType().getName() +
                (item.getJarPath() == null ? "" : " from " + item.getJarPath()));
    }
}
