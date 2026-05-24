package io.github.qishr.cascara.common.service;

import java.io.IOException;
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
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.ModulePath;
import io.github.qishr.cascara.common.util.Properties;

public class ServiceProviderLayer {
    private static ServiceProviderLayer rootLayer;

    private Reporter reporter = new NoOpReporter();

    private boolean ownsReporter = false;

    private String name;
    private boolean isPublic;
    private ModulePath modulePath;
    private ModuleLayer moduleLayer;
    private ServiceProviderLayer parent;

    private List<Path> jarPaths = new ArrayList<>();
    private List<ServiceProviderLayer> allLayers = new ArrayList<>();
    private Map<String,ServiceProviderLayer> namedLayers = new HashMap<>();
    private Map<String,ServiceProviderMetadata> providers = new HashMap<>();
    private Map<Class<ServiceProvider>, Set<ServiceProviderMetadata>> metadataForServiceType = new HashMap<>();
    private Map<String, Properties> capabilitiesForProvider = new HashMap<>();

    private ServiceProviderLayer() { }

    public String getName() { return name; }

    public ServiceProviderLayer getParent() { return parent; }

    public Collection<ServiceProviderLayer> getLayers() { return allLayers; }

    public ServiceProviderLayer getLayer(String name) { return namedLayers.get(name); }

    public boolean containsLayer(String name) { return namedLayers.containsKey(name); }

    public boolean containsProvider(String name) { return providers.containsKey(name); }

    public Collection<ServiceProviderMetadata> getProviders() { return providers.values(); }

    public Path getPath(String name) { return modulePath.getPathForModule(name); }

    public boolean isPublic() { return isPublic; }

    public void setPublic(boolean v) { isPublic = v; }

    public static ServiceProviderLayer getRootLayer() {
        return getRootLayer(null);
    }

    public static ServiceProviderLayer getRootLayer(Reporter reporter) {
        if (rootLayer == null) {
            rootLayer = new ServiceProviderLayer();
            rootLayer.name = "root";
            if (rootLayer != null) {
                rootLayer.setReporter(reporter);
            }
            ModuleLayer boot = ModuleLayer.boot();
            boot.modules().forEach((module) -> {
                rootLayer.registerModule(module);
            });
        }
        return rootLayer;
    }

    public Reporter getReporter() {
        if (ownsReporter || parent == null) { return reporter; }
        return parent.getReporter();
    }

    /// Sets the reporter for communicating mapping warnings or errors.
    public ServiceProviderLayer setReporter(Reporter repoter) {
        if (reporter == null) {
            reporter = new NoOpReporter();
        } else {
            this.reporter = repoter;
            this.ownsReporter = true;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadProvider(Class<T> providerClass) {
        if (!ServiceProvider.class.isAssignableFrom(providerClass)) {
            throw new ServiceException("\"" + providerClass + "\" is not a ServiceProvider.");
        }
        try {
            Constructor<?> constructor = providerClass.getDeclaredConstructor();
            if (constructor == null) {
                getRootLayer().reporter.debug("No declared constructor for " + providerClass.getName());
                throw new ServiceException("Class " + providerClass.getName() + " has no no-args constructor.");
            } else {
                ServiceProvider instance = (ServiceProvider) constructor.newInstance();
                return (T)instance;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new ServiceException("Failed to instantiate class " + providerClass.getName() + ". Cause: " + e.getMessage(), e);
        }
    }

    // TODO: Don't just pick the first one, pick one that's declared in a Cascara module
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T loadDefault(Class<T> serviceType) {
        if (!ServiceProvider.class.isAssignableFrom(serviceType)) {
            throw new ServiceException("\"" + serviceType + "\" is not a ServiceProvider.");
        }
        List<ServiceProviderMetadata> providers = getRootLayer().findAllProviders((Class)serviceType);
        if (providers.isEmpty()) {
            throw new ServiceException("No providers registered for " + serviceType.getSimpleName());
        }
        ServiceProviderMetadata meta = providers.getFirst();
        Class<ServiceProvider> clazz = meta.getType();
        return (T)loadProvider(clazz);
    }

    public Set<Class<ServiceProvider>> getServiceTypes() {
        return metadataForServiceType.keySet();
    }

    public Set<Class<ServiceProvider>> findServiceTypes() {
        Set<Class<ServiceProvider>> found = new HashSet<>();
        found.addAll(getServiceTypes());
        for (ServiceProviderLayer layer : allLayers) {
            found.addAll(layer.findServiceTypes());
        }
        return found;
    }

    public ServiceProviderMetadata getProviderMetadata(String providerName) {
        return providers.get(providerName);
    }

    public List<ServiceProviderMetadata> getProviders(Class<? extends ServiceProvider> serviceType) {
        List<ServiceProviderMetadata> found = new ArrayList<>();
        if (metadataForServiceType.get(serviceType) != null) {
            Set<ServiceProviderMetadata> set = metadataForServiceType.get(serviceType);
            for (ServiceProviderMetadata item : set) {
                reportFinding(item, 0);
            }
            found.addAll(set);
        }
        return found;
    }

    public List<ServiceProviderMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType) {
        String startLayer = (name == null ? "unnamed layer" : "layer " + name);
        getReporter().debug("Searching for " + serviceType.getSimpleName() + " starting at " + startLayer);
        return findAllProviders(serviceType, null);
    }

    private List<ServiceProviderMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, ServiceProviderLayer previous) {
        List<ServiceProviderMetadata> found = new ArrayList<>();

        if (metadataForServiceType.get(serviceType) != null) {
            Set<ServiceProviderMetadata> set = metadataForServiceType.get(serviceType);
            for (ServiceProviderMetadata item : set) {
                reportFinding(item, 0);
            }
            found.addAll(set);
        }

        if (parent == null) {
            // Branch out from root. `previous` is used to avoid going down the branch we just came from
            for (ServiceProviderLayer layer : allLayers) {
                if (layer != previous && layer.isPublic) {
                    found.addAll(layer.findProvidersInBranches(serviceType, 0));
                }
            }
        } else {
            // Go towards root
            getReporter().trace("⬆ " + parent.name);
            found.addAll(parent.findAllProviders(serviceType, this));
        }

        return found;
    }

    private List<ServiceProviderMetadata> findProvidersInBranches(Class<? extends ServiceProvider> serviceType, int depth) {
        List<ServiceProviderMetadata> found = new ArrayList<>();
        getReporter().trace("" + "  ".repeat(depth) + "⬇ " + name);

        if (metadataForServiceType.get(serviceType) != null) {
            Set<ServiceProviderMetadata> set = metadataForServiceType.get(serviceType);
            for (ServiceProviderMetadata item : set) {
                reportFinding(item, depth);
            }
            found.addAll(set);
        }

        for (ServiceProviderLayer layer : allLayers) {
            if (layer.isPublic) {
                found.addAll(layer.findProvidersInBranches(serviceType, depth + 1));
            }
        }

        return found;
    }

    private void reportFinding(ServiceProviderMetadata item, int depth) {
        getReporter().debug("[" + name + "] " + "  ".repeat(depth) + item.getType().getName() +
                (item.getLocation() == null ? "" : " from " + item.getLocation()));
    }

    public void remove(String layerName) {
        for (ServiceProviderLayer layer : allLayers) {
            if (layer.getName().equals(layerName)) {
                allLayers.remove(layer);
                namedLayers.remove(layerName);
                return;
            }
        }
    }

    public ServiceProviderLayer create() {
        return create(null);
    }

    public ServiceProviderLayer create(String name) {
        ServiceProviderLayer layer = new ServiceProviderLayer();
        layer.parent = this;
        allLayers.add(layer);
        if (name != null) {
            layer.name = name;
            namedLayers.put(name, layer);
        }
        return layer;
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
        if (classLoader == null) {
            getReporter().error("Module \"%s\" has no ClassLoader.", moduleName);
            return;
        }

        ModuleDescriptor desc = module.getDescriptor();
        for (Provides provides : desc.provides()) {
            for (String providerClassName : provides.providers()) {
                try {
                    Class<?> type = classLoader.loadClass(providerClassName);
                    registerClass((Class)type);
                } catch (ClassNotFoundException | ServiceException e) {
                    getReporter().warn("Failed to load class " + providerClassName +". Cause: " + e.getMessage(), e);
                }
            }
        }

        //
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
        } catch (IOException e) {
            String message = String.format("Failed to read Jar \"%s\". Cause: " + e.getMessage(), jarPath);
            throw new ServiceException(message, e);
        }

        if (moduleName == null || moduleName.isEmpty()) {
            String message = String.format("Jar \"%s\" does not contain a module.", jarPath);
            throw new ServiceException(message);
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

        enumerateServices();
    }

    /// Use SPI to find the service implementations inside this layer
    private <P> void enumerateServices() {
        providers.clear();
        metadataForServiceType.clear();
        var loader = ServiceLoader.load(moduleLayer, ServiceProvider.class);
        loader.forEach(provider -> {
            Path jarPath = modulePath.getPathForModule(provider.getClass().getModule().getName());
            try {
                registerProvider(provider, jarPath.toString());
            } catch (Exception e) {
                registrationError("Failed to query module.", null, e);
            } catch(AbstractMethodError e) {
                registrationError("Incompatible module.", jarPath.toString(), e);
            } catch (NoClassDefFoundError e) {
                registrationError("Incompatible module.", jarPath.toString(), e);
            } catch (ServiceConfigurationError e) {
                registrationError("Incompatible module.", jarPath.toString(), e);
            }
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void registerProvider(ServiceProvider provider, String location) {
        getReporter().debug("Registering \"%s\"", provider.getClass().getName());
        try {
            Class<?> providerClass = provider.getClass();

            List<Class<ServiceProvider>> interfaceHierarchy = new ArrayList<>();

            if (collectCascaraModuleInterfaces(providerClass, interfaceHierarchy)) {

                String providerClassName = providerClass.getName();
                Properties capabilities = provider.getCapabilities();

                capabilitiesForProvider.put(providerClassName, provider.getCapabilities());

                ServiceProviderMetadata meta = new ServiceProviderMetadata();

                meta.setType((Class)provider.getClass());
                meta.setCapabilities(capabilities);
                meta.setLocation(location);
                providers.put(providerClassName, meta);

                for (Class<?> interfaceType : interfaceHierarchy) {

                    Set<ServiceProviderMetadata> metas = metadataForServiceType.get(interfaceType);
                    if (metas == null) {
                        metas = new HashSet<>();
                        metadataForServiceType.put((Class)interfaceType, metas);
                    }
                    metas.add(meta);
                }
            }
        } catch(AbstractMethodError e) {
            registrationError("Incompatible module: " + provider.getClass().getName() + ".", location, e);
        } catch (NoClassDefFoundError e) {
            registrationError("Incompatible module: " + provider.getClass().getName() + ".", location, e);
        } catch (ServiceConfigurationError e) {
            registrationError("Incompatible module: " + provider.getClass().getName() + ".", location, e);
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

    private void registrationError(String message, String location, Throwable t) {
        String logMessage = message;
        if (location != null) {
            logMessage = logMessage + " " + location;
        }
        if (t != null) {
            logMessage = logMessage + " " + t.getMessage();
        }
        getReporter().error(logMessage);
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
}
