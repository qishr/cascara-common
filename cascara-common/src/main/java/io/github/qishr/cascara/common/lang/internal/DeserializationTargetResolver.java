package io.github.qishr.cascara.common.lang.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.PolymorphicType;
import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.annotation.SubType;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.util.ClassHierarchy;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public class DeserializationTargetResolver {
    private Map<Class<?>,Set<Class<?>>> permittedSubclassesCache = new HashMap<>();
    private Map<Class<?>,Set<Class<?>>> allSubclassesCache = new HashMap<>();
    private Map<String,Class<?>> specificSubclassCache = new HashMap<>();

    public DeserializationTargetResolver() {}

    @SuppressWarnings("unchecked")
    @NonNull
    public <C> Class<? extends C> resolveTarget(MapAstNode<?,?,?> node, Type jvmType, boolean useHeuristicMatch) {
        Class<C> rawClass = ReflectionUtils.getRawClass(jvmType);
        Class<? extends C> targetClass = null;
        Set<Class<?>> candidates = null;
        String specifiedSubType = null;

        // 0. Overrides specified by AbstractTypeRegistry
        if (rawClass.isInterface() || Modifier.isAbstract(rawClass.getModifiers())) {
            Class<?> concreteClass = AbstractTypeRegistry.resolve(rawClass);
            if (concreteClass != null && !Modifier.isAbstract(concreteClass.getModifiers())) {
                targetClass = (Class<C>) concreteClass;
            }
        }

        // 1. Check if the base class has an @AbstractType annotation
        if (targetClass == null && node != null) {
            PolymorphicType abstractTypeAnno = rawClass.getAnnotation(PolymorphicType.class);
            if (abstractTypeAnno != null) {
                String typeFieldName = abstractTypeAnno.property();
                specifiedSubType = node.getString(typeFieldName);
            }
        }

        // 2. Annotations on concrete subclasses of sealed class.
        if (targetClass == null && specifiedSubType != null) {
            if (rawClass.isSealed()) {
                candidates = getPermittedSubclasses(rawClass);
                targetClass = getSubclass(rawClass, specifiedSubType, candidates);
            }
        }

        if (targetClass == null) {
            candidates = getAllSubclasses(rawClass);

            // 3. Annotations on concrete subclasses of non-sealed subclasses.
            if (specifiedSubType != null) {
                targetClass = getSubclass(rawClass, specifiedSubType, candidates);
            }

            // 4. Polymorphic inference / discrimination / heuristics
            if (targetClass == null && useHeuristicMatch) {
                targetClass = resolvePolymorphicTarget(node, rawClass, candidates);
            }
        }

        // 5. If no subclass was found, return the base class
        if (targetClass == null) {
            targetClass = rawClass;
        }

        if (Modifier.isAbstract(targetClass.getModifiers())) {
            throw new SerializerException(LangMessage.AMBIGUOUS_POLYMORPHIC, rawClass);
        }

        return targetClass;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private <C> Class<? extends C> resolvePolymorphicTarget(MapAstNode<?,?,?> map, Class<C> baseClass, Set<Class<?>> candidates) {
        if (map == null) {
            return null;
        }

        Set<String> keys = map.keyStringSet();
        Class<? extends C> best = baseClass;
        int bestScore = -1;
        boolean hasTie = false;

        for (Class<?> candidate : candidates) {
            try {
                if (baseClass.isAssignableFrom(candidate)) {
                    Set<String> candidateFields = getSerializableFieldNames(candidate);
                    int score = intersectionSize(keys, candidateFields);

                    if (score > bestScore) {
                        bestScore = score;
                        best = (Class<C>) candidate;
                        hasTie = false; // New leader found, reset tie flag
                    } else if (score == bestScore && score > 0) {
                        hasTie = true; // Duplicate highest score encountered
                    }
                }
            } catch (Exception e) {
                // Ignore and continue
            }
        }

        if (hasTie) {
            throw new SerializerException(LangMessage.AMBIGUOUS_POLYMORPHIC, baseClass);
        }

        return best;
    }

    private Set<Class<?>> getPermittedSubclasses(Class<?> baseClass) {
        synchronized (permittedSubclassesCache) {
            return permittedSubclassesCache.computeIfAbsent(baseClass, c -> {
                Class<?>[] permitted = baseClass.getPermittedSubclasses();
                Set<Class<?>> subclasses = new HashSet<>();
                for (Class<?> clazz : permitted) {
                    if (baseClass.isAssignableFrom(clazz)) {
                        subclasses.add((Class<?>)clazz);
                    }
                }
                return subclasses;
            });
        }
    }

    private Set<Class<?>> getAllSubclasses(Class<?> baseClass) {
        synchronized (allSubclassesCache) {
            return allSubclassesCache.computeIfAbsent(baseClass, c -> {
                List<String> subclassNames = ClassHierarchy.getSubclasses(baseClass.getName());
                Set<Class<?>> subclasses = new HashSet<>();
                for (String fqcn : subclassNames) {
                    Class<?> candidate;
                    try {
                        candidate = Class.forName(fqcn);
                        subclasses.add(candidate);
                    } catch (ClassNotFoundException ignored) {}
                }
                return subclasses;
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <C> Class<? extends C> getSubclass(Class<C> baseClass, String specifiedSubType, Set<Class<?>> candidates) {
        synchronized(specificSubclassCache) {
            String key = baseClass.getName() + "#" + specifiedSubType;
            return (Class<C>) specificSubclassCache.computeIfAbsent(key, k -> {
                for (Class<?> candidate : candidates) {
                    SubType subTypeAnno = candidate.getAnnotation(SubType.class);
                    if (subTypeAnno != null) {
                        String name = subTypeAnno.name();
                        if (name.equals(specifiedSubType)) {
                            if (baseClass.isAssignableFrom(candidate)) {
                                return (Class<C>) candidate;
                            }
                        }
                    }
                }
                return null;
            });
        }
    }

    @NonNull
    private static Set<String> getSerializableFieldNames(Class<?> jvmClass) {
        List<Field> fields = getAllSerializableFields(jvmClass);
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        return names;
    }

    /// Retrieves all declared fields for a class and all its superclasses (excluding Object).
    @NonNull
    private static List<Field> getAllSerializableFields(Class<?> jvmType) {
        return ReflectionUtils.getAllFields(jvmType, f -> f.isAnnotationPresent(DataIgnore.class));
    }

    private static int intersectionSize(Set<String> a, Set<String> b) {
        int count = 0;
        for (String k : a) {
            if (b.contains(k)) {
                count++;
            }
        }
        return count;
    }
}
