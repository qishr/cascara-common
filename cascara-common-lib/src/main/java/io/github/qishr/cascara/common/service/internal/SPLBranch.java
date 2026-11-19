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
import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import io.github.qishr.cascara.common.annotation.NoAutoRegistration;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.annotation.Priority;
import io.github.qishr.cascara.common.annotation.SingletonInitializer;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exception.UnexpectedNullValueException;
import io.github.qishr.cascara.common.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.security.policy.SignaturePolicy;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.security.policy.SignatureException;
import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.service.ContentTypeProvider;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.SPLConfig;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.ServiceMessage;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.common.trackable.TrackableArray;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ClassHierarchy;
import io.github.qishr.cascara.common.util.Classes;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.ModulePath;
import io.github.qishr.cascara.common.util.ReflectionUtils;
import io.github.qishr.cascara.common.util.ModulePath.DiscoveredModule;

public class SPLBranch implements SPL {
    protected static final Reporter REPORTER = Reporter.forClass(SPL.class);

    protected static SPLRoot rootLayer;

    protected static SPLConfig config = SPLConfig.get();

    // TODO: Remove this
    protected boolean ownsReporter = false;

    protected String name;
    protected boolean isPublic;
    protected ModulePath modulePath;
    protected ModuleLayer moduleLayer;
    protected SPLBranch parent;

    protected List<Path> jarPaths = new ArrayList<>();
    protected Map<String,SPLBranch> namedChildren = new HashMap<>();

    protected List<ServiceMetadata> orderedProviders = new ArrayList<>();

    protected Map<String,ServiceMetadata> providersByFqcn = new HashMap<>();
    protected Map<String,ServiceMetadata> servicesByFqcn = new HashMap<>();
    protected Map<Class<ServiceProvider>, Set<ServiceMetadata>> providersByServiceType = new HashMap<>();

    protected final Map<String, Object> singletonCache = new ConcurrentHashMap<>();

    Set<SPLBranch> visibleLayers = new HashSet<>();
    final TrackableArray<ServiceMetadata> visibleProviders = new TrackableArray<>();
    private final TrackableArray<ServiceMetadata> declaredProviders = new TrackableArray<>();

    protected TrackableArray<String> modules = new TrackableArray<>();

    protected SPLBranch() { }

    //
    // Layer metadata, hierarchy, creation and deletion
    //

    @Override
    public String getName() { return name; }

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
    public SPL getParent() { return parent; }

    @Override
    public List<SPL> getChildren() {
        return namedChildren.values().stream().map(layer -> {
            return (SPL)layer;
        }).toList();
    }

    @Override
    public SPL getChild(String name) { return namedChildren.get(name); }

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
            for (SPLBranch sibling : parent.namedChildren.values()) {
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
        for (SPLBranch layer : namedChildren.values()) {
            found.addAll(layer.findServiceTypes());
        }
        return found;
    }

    @Override
    public Set<ServiceMetadata> findServices() {
        Set<ServiceMetadata> found = new HashSet<>();
        found.addAll(getServices());
        for (SPLBranch layer : namedChildren.values()) {
            found.addAll(layer.findServices());
        }
        return found;
    }

    /// Retrieves metadata of the nearest known provider of the specified service type.
    @Override
    public ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType) {
        List<ServiceMetadata> all = internalFindAllProviders(serviceType, null, null);
        ServiceMetadata highestPriorityProvider = getHighestPriority(all);
        return highestPriorityProvider;
    }

    /// Retrieves metadata of the nearest known provider whose capabilities satisfy the given predicate.
    @Override
    public ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate) {
        List<ServiceMetadata> all = internalFindAllProviders(serviceType, capabilityPredicate, null);
        ServiceMetadata highestPriorityProvider = getHighestPriority(all);
        return highestPriorityProvider;
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

    /// Retrieves metadata of the specified provider if it exists in this layer, otherwise `null` is returne.
    @Nullable
    @Override
    public ServiceMetadata getProvider(String providerName) {
        return providersByFqcn.get(providerName);
    }

    /// Retrieves metadata of providers in this layer.
    @Override
    public List<ServiceMetadata> getProviders() {
        return providersByFqcn.values().stream().toList();
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

    @SuppressWarnings({ "unchecked", "null" })
    @NonNull
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

            // Order matters here. Singleton should not be in the cache unless it's initialized.
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
                        ServiceMessage.INVALID_SINGLETON_INITIALIZER,
                        clazz.getName() + "." + method.getName(), "Method must take zero arguments"
                    );
                }
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Throwable t) {
                    DiagnosticMessage message = DiagnosticMessage.forException(t);
                    if (message == null) {
                        throw new ServiceException(
                            t,
                            GenericMessage.ERROR,
                            t.getMessage() + ": " + clazz.getSimpleName() + "." + method.getName()
                        );
                    } else {
                        throw new ServiceException(
                            t,
                            message,
                            clazz.getSimpleName() + "." + method.getName()
                        );
                    }
                }
                break;
            }
        }
    }

    public void removeSingleton(ServiceMetadata meta) {
        singletonCache.remove(meta.getTypeName());
    }

    //
    // Hierarchy
    //

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

    @Override
    public void registerClass(Class<?> type) {
        if (type == null)
        {
            throw new UnexpectedNullValueException("type");
        }
        if (!ServiceProvider.class.isAssignableFrom(type)) {
            throw new ServiceException(ServiceMessage.NOT_A_SERVICE_PROVIDER, type.getName());
        }

        boolean verify = config.isVerifyEnabled();

        if (verify) {
            verifySignature(type);
        }

        registerClassInternal(type, false);
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
    }

    @Override
    public void registerJar(Path jarPath) {
        String moduleName;
        JarManifest manifest;
        Set<String> classes;

        boolean verify = config.isVerifyEnabled();

        try (JarFile jar = JarFile.open(jarPath, verify)) {
            moduleName = jar.getModuleName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new ServiceException(ServiceMessage.NON_MODULAR_JAR, jarPath);
            }
            manifest = jar.getManifest();
            classes = jar.getClassNames();

            if (verify) {
                verifySignature(jar);
            }

            SemVer moduleBuildCascaraVersion = new SemVer(manifest.getString("Cascara-Version", "0.0.0"));
            SemVer moduleMinCascaraVersion = new SemVer(manifest.getString("Min-Cascara-Version", moduleBuildCascaraVersion.toString()));
            verifyModuleVersionCompatibility(moduleName, moduleBuildCascaraVersion, moduleMinCascaraVersion);

            REPORTER.debug("Discovering providers in \"%s\"", jarPath);

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
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            moduleLayer = parent.defineModulesWithManyLoaders(cf, loader);

            DiscoveredModule dm = new DiscoveredModule(moduleName, classes, jarPath);
            registerProviders(dm, verify);

            addModuleToMap(moduleName);
            SPLUtils.recomputeAllVisibleProviders(rootLayer);

            // TODO: These should use the same source
            Classes.invalidate();
            ClassHierarchy.invalidate();

        } catch(IOException | SecurityException e) {
            throw new ServiceException(e, ServiceMessage.FAILED_TO_READ_JAR, jarPath, e.getMessage());
        }
    }

    //
    // Layer Hierarchy
    //

    private SPLBranch createInternal(String name, boolean isPublic) {
        if (name == null) {
            throw new UnexpectedNullValueException("name");
        }
        SPLBranch layer = new SPLBranch();
        layer.parent = this;
        layer.isPublic = isPublic;
        layer.name = name;
        namedChildren.put(name, layer);
        SPLUtils.recomputeAllVisibleProviders(rootLayer);
        return layer;
    }

    /// Recursively removes a child layer and all descendant layers.
    private void removeInternal(SPLBranch layerToRemove) {
        for (SPLBranch childLayer : layerToRemove.namedChildren.values()) {
            removeInternal(childLayer);
        }
        layerToRemove.delete();
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
        for (SPLBranch descendant : layer.namedChildren.values()) {
            if (descendant.isPublic) {
                collected.add(descendant);
                collectPublicDescendants(descendant, collected);
            }
        }
    }

    //
    // Discovery
    //

    protected void registerProviders(DiscoveredModule dm, boolean isAutoRegistration) {
        Module module = dm.getName() == null
            ? null
            : moduleLayer.findModule(dm.getName()).orElse(null);

        Path jarPath = dm.getJarPath();
        for (String className : dm.getClasses()) {
            if (!isRegistered(className)) {
                try {
                    Class<?> clazz = module == null
                        ? Class.forName(className, false, ClassLoader.getSystemClassLoader())
                        : Class.forName(module, className);

                    // MUST verify class is a concrete ServiceProvider implementation
                    if (clazz != null
                            && ServiceProvider.class.isAssignableFrom(clazz)
                            && !clazz.isInterface()
                            && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {

                        @SuppressWarnings("unchecked")
                        Class<? extends ServiceProvider> providerClass = (Class<? extends ServiceProvider>) clazz;

                        ServiceProvider providerInstance = SPLUtils.instantiate(providerClass);
                        registerProvider(providerInstance, jarPath, true);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    if (isAutoRegistration) {
                        REPORTER.debug("Class not found: " + className);
                    } else {
                        registrationFailure("Class not found: " + className, jarPath, e, isAutoRegistration);
                    }
                } catch (ServiceException e) {
                    if (isAutoRegistration) {
                        REPORTER.debug("ServiceException while loading class " + className + ": " + e.getMessage());
                    } else {
                        registrationFailure("ServiceException while loading class " + className, jarPath, e, isAutoRegistration);
                    }
                // } catch (InstantiationException e) {
                } catch (AbstractMethodError | ServiceConfigurationError e) {
                    registrationFailure("Incompatible module.", jarPath, e, isAutoRegistration);
                } catch (Exception e) {
                    REPORTER.debug(e.getClass().getSimpleName() + " while loading " + className + ": " + e.getMessage());
                    registrationFailure("Failed to instantiate candidate provider " + className + ".", jarPath, e, true);
                }
            }
        }
    }

    public class VfsJarClassLoader extends ClassLoader {
        private final Path jarPath;
        public VfsJarClassLoader(Path jarPath, ClassLoader parent) {
            super(parent);
            this.jarPath = jarPath;
        }
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // Convert FQCN to JAR entry path: com.example.Foo -> com/example/Foo.class
            String entryPath = name.replace('.', '/') + ".class";
            try (JarFile jar = JarFile.open(jarPath)) {
                JarEntry entry = jar.getJarEntry(entryPath);
                if (entry != null) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        byte[] classBytes = in.readAllBytes();
                        return defineClass(name, classBytes, 0, classBytes.length);
                    }
                }
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to read class " + name + " from " + jarPath, e);
            }
            throw new ClassNotFoundException(name);
        }
    }

    protected static void verifySignature(Class<?> cls) {
        SignaturePolicy policy = SPLRoot.getSignaturePolicy();

        if (policy == null) {
            return;
        }

        SignatureStatus status = policy.evaluate(cls);

        if (status != SignatureStatus.VALID) {
            throw new SignatureException(
                status,
                ServiceMessage.VERIFICATION_FAILED,
                status,
                cls
            );
        }
    }

    protected static void verifySignature(JarFile jar) {

        List<ZipEntry> entries = jar.getEntries();
        for (ZipEntry entry : entries) {
            try (InputStream in = jar.getInputStream(entry)) {
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        SignaturePolicy policy = SPLRoot.getSignaturePolicy();

        if (policy == null) {
            return;
        }

        SignatureStatus status = policy.evaluate(jar);

        if (status != SignatureStatus.VALID) {
            throw new SignatureException(
                status,
                ServiceMessage.VERIFICATION_FAILED,
                status,
                jar.getPath()
            );
        }
    }

    private void verifyModuleVersionCompatibility(String moduleName, SemVer moduleBuildCascaraVersion, SemVer moduleMinCascaraVersion) {
        SemVer activeCascaraVersion = Cascara.instance().getVersion();

        boolean hasMinVersion = !"0.0.0".equals(moduleMinCascaraVersion.toString());
        boolean hasBuildVersion = !"0.0.0".equals(moduleBuildCascaraVersion.toString());

        if (
            (hasMinVersion && (
                activeCascaraVersion.isLowerThan(moduleMinCascaraVersion) ||
                activeCascaraVersion.getMajor() != moduleMinCascaraVersion.getMajor()
            )) ||
            (hasBuildVersion && activeCascaraVersion.getMajor() != moduleBuildCascaraVersion.getMajor())
        ) {
            throw new ServiceException(
                ServiceMessage.INCOMPATIBLE_MODULE_VERSION,
                moduleName, moduleMinCascaraVersion, activeCascaraVersion
            );
        }
    }

    /// Adds a module to the root layer's `moduleToLayers` map and updates
    /// the `modules` `TrackableArray`
    private void addModuleToMap(String moduleName) {
        Set<SPL> layers = rootLayer.moduleToLayers.get(moduleName);
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
    /// the `modules` `TrackableArray`
    private void removeModuleFromMap(String moduleName) {
        Set<SPL> layers = rootLayer.moduleToLayers.get(moduleName);
        if (layers != null) {
            layers.remove(this);
            if (layers.isEmpty()) {
                rootLayer.moduleToLayers.remove(moduleName);
            }
        }
        modules.remove(moduleName);
    }

    //
    // Registration
    //

    private boolean isRegistered(String providerFqcn) {
        return rootLayer.bootProviders.contains(providerFqcn) || providersByFqcn.containsKey(providerFqcn);
    }

    private boolean noAutoRegistration(Class<?> clazz) {
        return clazz.isAnnotationPresent(NoAutoRegistration.class);
    }

    private void registerClassInternal(Class<?> providerClass, boolean isAutoRegistration) {
        String providerFqcn = providerClass.getName();
        if (!isRegistered(providerFqcn)) {
            ServiceProvider instance = (ServiceProvider) SPLUtils.instantiate(providerClass);
            Path jarPath = modulePath != null ? modulePath.getPathForModule(providerClass.getModule().getName()) : null;
            registerProvider(instance, jarPath, isAutoRegistration);
            if (SPLRoot.isBooting) {
                rootLayer.bootProviders.add(providerFqcn);
            }
        }
    }

    private void registerProvider(ServiceProvider instance, Path jarPath, boolean isAutoRegistration) {
        try {
            Class<? extends ServiceProvider> providerClass = instance.getClass();
            if (isAutoRegistration && noAutoRegistration(providerClass)) {
                REPORTER.trace("  Not auto-registering %s", instance.getClass().getName());
                return;
            }

            List<Class<ServiceProvider>> interfaceHierarchy = new ArrayList<>();
            if (collectCascaraModuleInterfaces(providerClass, interfaceHierarchy)) {

                for (Class<ServiceProvider> serviceInterface : interfaceHierarchy) {
                    if (noAutoRegistration(serviceInterface.getClass())) {
                        REPORTER.trace("  Not auto-registering %s", instance.getClass().getName());
                        return;
                    }
                }
                REPORTER.trace("  Registering %s", instance.getClass().getName());

                // Experimental:
                // Store the rich ContentTypes that services support
                // We probably want services to spply an array/list of content types
                ContentType contentType = null;
                if (instance instanceof ContentTypeProvider ctp) {
                    try {
                        // TODO: Any methods called of the provider should have try/catch
                        contentType = ctp.getContentType();
                        if (contentType != null) {
                            rootLayer.storeContentType(contentType);
                        }
                    } catch(Exception e){}
                }

                boolean isSingleton = false;
                List<Method> methods = ReflectionUtils.getAllMethods(providerClass);
                for (Method method : methods) {
                    if (method.isAnnotationPresent(SingletonInitializer.class)) {
                        isSingleton = true;
                    }
                }

                Priority annotation = providerClass.getAnnotation(Priority.class);
                int priority =  annotation != null ? annotation.value() : 0; // Default priority is 0

                Properties providerProperties = getProviderProperties(instance, jarPath);
                ServiceMetadata provider = new ServiceMetadata(this, providerClass, providerProperties, contentType, isSingleton, priority);

                orderedProviders.add(provider);
                providersByFqcn.put(providerClass.getName(), provider);

                for (Class<ServiceProvider> serviceInterface : interfaceHierarchy) {

                    ServiceMetadata service = new ServiceMetadata(this, serviceInterface, getServiceProperties(serviceInterface), null, false, 0);
                    servicesByFqcn.put(serviceInterface.getName(), service);

                    Set<ServiceMetadata> providers = providersByServiceType.get(serviceInterface);
                    if (providers == null) {
                        providers = new HashSet<>();
                        providersByServiceType.put(serviceInterface, providers);
                    }
                    providers.add(provider);
                }

                if (contentType == null) {
                    REPORTER.debug("  Registered " + providerClass.getName());
                } else {
                    // if (contentType.getMimeTypes().contains(GlobalReporter.SERIALIZATION_FORMAT)) {
                    //     if (instance instanceof Serializer<?> serializer) {
                    //         GlobalReporter.initSerializer(serializer);
                    //     }
                    // }
                    REPORTER.debug("  Registered " + providerClass.getName() + " with content types:");
                    for (String type : contentType.getMimeTypes()) {
                        REPORTER.debug("    " + type);
                    }
                }

                declaredProviders.add(provider);
            }
        } catch(AbstractMethodError | ServiceConfigurationError e) {
            registrationFailure("Incompatible module: " + instance.getClass().getSimpleName() + ".", jarPath, e, isAutoRegistration);
        }
    }

    private Properties getServiceProperties(Class<ServiceProvider> serviceInterface) {
        Properties properties = new Properties();
        setModuleProperties(properties, serviceInterface);
        properties.set(SPL.SP_SERVICE_NAME, serviceInterface.getSimpleName());
        return properties;
    }

    private Properties getProviderProperties(ServiceProvider provider, Path jarPath) {
        Properties properties = new Properties();
        setModuleProperties(properties, provider.getClass());
        if (jarPath != null) {
            properties.set(SPL.SP_JAR_PATH, jarPath.toString());
        }
        properties.set(SPL.SP_PROVIDER_NAME, provider.getClass().getSimpleName());
        Properties declaredCapabilities = null;
        try {
            declaredCapabilities = provider.getServiceProperties();
        } catch (Throwable t) {

            // TODO: Remove this
            System.err.println(t.getMessage());

        }
        if (declaredCapabilities != null) {
            properties.addAll(declaredCapabilities);
        }
        return properties;
    }

    private void setModuleProperties(Properties properties, Class<?> type) {
        Module module = type.getModule();
        properties.set(SPL.SP_MODULE_NAME, module.getName());
        ModuleDescriptor descriptor = module.getDescriptor();
        if (descriptor != null) {
            descriptor.rawVersion().ifPresent(moduleVersion -> {
                properties.set(SPL.SP_MODULE_VERSION, moduleVersion);
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

    private void registrationFailure(String message, Path location, Throwable t, boolean isAutoRegistration) {
        String logMessage = message;
        if (location != null) {
            logMessage = logMessage + " " + location;
        }

        // if (message.contains("GlobalReporterTestTask")) {
        //     // Thread.dumpStack();
        //     // if (t instanceof Exception e) {
        //     //     System.err.println(e.getClass());
        //     //     System.err.println(e.getMessage());
        //     //     e.printStackTrace();
        //     //     if (e.getCause() instanceof Exception e2) {
        //     //         e2.printStackTrace();
        //     //     }
        //     // }
        //     REPORTER.error(t, GenericMessage.ERROR, logMessage);
        // }

        // TODO: Better reporting
        // if (t != null) {
        //     logMessage = logMessage + " " + t.getMessage();
        // }
        if (isAutoRegistration) {
            REPORTER.warn(GenericMessage.ERROR, logMessage);
            // REPORTER.error(t, GenericMessage.ERROR, logMessage);
        } else {
            REPORTER.error(t, GenericMessage.ERROR, logMessage);
        }
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
    // Finding
    //

    private ServiceMetadata getHighestPriority(List<ServiceMetadata> all) {
        ServiceMetadata highestPriorityProvider = null;
        int highestPriority = -3;
        for (ServiceMetadata provider : all) {
            int priority = provider.getPriority();
            if (priority > highestPriority) {
                highestPriority = priority;
                highestPriorityProvider = provider;
            }
        }
        return highestPriorityProvider;
    }

    private List<ServiceMetadata> internalFindAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate, SPLBranch previous) {
        REPORTER.debug("[finding] layer=\"" + name + "\", service=\"" + serviceType.getSimpleName() + "\"");
        List<ServiceMetadata> found = new ArrayList<>();

        addAllToList(findProvidersInBranches(serviceType, capabilityPredicate, 0), found);

        if (parent != null && parent != previous) {
            // Go towards root
            addAllToList(parent.internalFindAllProviders(serviceType, capabilityPredicate, this), found);
        }

        return found;
    }

    private List<ServiceMetadata> findProvidersInBranches(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate, int depth) {
        List<ServiceMetadata> found = new ArrayList<>();

        Set<ServiceMetadata> byType = providersByServiceType.get(serviceType);

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

        for (SPLBranch layer : namedChildren.values()) {
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
        REPORTER.debug("[found  ] " + "layer=\"" + name + "\", provider=\"" + item.getType().getName() + "\"" +
            (item.getJarPath() == null ? "" : ", jar=\"" + item.getJarPath() + "\""));
    }

    protected static void bootError(Throwable e, ServiceMessage message, Object... details) {
        REPORTER.error(e, message, details);
    }

    protected static void bootWarning(ServiceMessage message, Object... details) {
        REPORTER.warn(message, details);
    }

    public void setParent(SPL parent) {
        throw new UnimplementedMethodException();
    }
}
