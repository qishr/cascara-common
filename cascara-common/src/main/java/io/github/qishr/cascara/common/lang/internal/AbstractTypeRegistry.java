package io.github.qishr.cascara.common.lang.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/// Centralized registry mapping abstract types and interfaces to concrete implementations,
/// supporting programmatic overrides, system properties (-D), and property file loading.
public final class AbstractTypeRegistry {

    private static final String DEFAULT_PROPERTIES_FILE = "cascara-type-mappings.properties";
    private static final Map<Class<?>, Class<?>> registry = new ConcurrentHashMap<>();
    private static final Properties fileProperties = new Properties();

    static {
        loadPropertiesFile();
        loadDefaultMappings();
    }

    private AbstractTypeRegistry() {}

    private static void loadPropertiesFile() {
        try (InputStream input = AbstractTypeRegistry.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (input != null) {
                fileProperties.load(input);
            }
        } catch (IOException ignored) {
            // Fallback gracefully if properties file is absent
        }
    }

    private static void loadDefaultMappings() {
        // Core Java Collections & Interfaces Defaults
        registry.put(java.util.List.class, java.util.ArrayList.class);
        registry.put(java.util.Set.class, java.util.HashSet.class);
        registry.put(java.util.Map.class, java.util.LinkedHashMap.class);
        registry.put(java.util.Queue.class, java.util.ArrayDeque.class);
        registry.put(java.util.Deque.class, java.util.ArrayDeque.class);
        registry.put(java.util.AbstractQueue.class, java.util.PriorityQueue.class);
        registry.put(java.util.SortedSet.class, java.util.TreeSet.class);
        registry.put(java.util.NavigableSet.class, java.util.TreeSet.class);
        registry.put(java.util.SortedMap.class, java.util.TreeMap.class);
        registry.put(java.util.NavigableMap.class, java.util.TreeMap.class);
        registry.put(java.util.Calendar.class, java.util.GregorianCalendar.class);
        registry.put(java.util.Dictionary.class, java.util.Hashtable.class);
        registry.put(java.util.SequencedCollection.class, ArrayList.class);
    }

    /// Programmatically register or override an abstract-to-concrete mapping.
    public static <T> void register(Class<T> abstractType, Class<? extends T> concreteType) {
        registry.put(abstractType, concreteType);
    }

    /// Resolves a concrete implementation class for a given abstract type or interface.
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> resolve(Class<T> abstractType) {
        if (abstractType == null) return null;

        // 1. Check System Properties (-Dcascara.type.mapping.<FQCN>=<ConcreteFQCN>)
        String sysPropKey = "cascara.type.mapping." + abstractType.getName();
        String sysPropVal = System.getProperty(sysPropKey);
        if (sysPropVal != null && !sysPropVal.isBlank()) {
            try {
                Class<?> loaded = Class.forName(sysPropVal.trim());
                if (abstractType.isAssignableFrom(loaded)) {
                    return (Class<? extends T>) loaded;
                }
            } catch (ClassNotFoundException ignored) {}
        }

        // 2. Check Property File configuration
        String filePropVal = fileProperties.getProperty(abstractType.getName());
        if (filePropVal != null && !filePropVal.isBlank()) {
            try {
                Class<?> loaded = Class.forName(filePropVal.trim());
                if (abstractType.isAssignableFrom(loaded)) {
                    return (Class<? extends T>) loaded;
                }
            } catch (ClassNotFoundException ignored) {}
        }

        // 3. Check programmatic registry / defaults map
        Class<?> mapped = registry.get(abstractType);
        if (mapped != null) {
            return (Class<? extends T>) mapped;
        }

        // 4. Return the type itself if it's already concrete
        return abstractType;
    }
}
