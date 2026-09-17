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

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;

public class ReflectionUtils {

    public static boolean isInstance(Object thisInstance, Type thatType) {
        if (thisInstance == null || thatType == null) {
            return false;
        }

        // Case 1: Simple class
        if (thatType instanceof Class<?> cls) {
            return cls.isInstance(thisInstance);
        }

        // Case 2: Parameterized type → check raw type
        if (thatType instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass) {
                return rawClass.isInstance(thisInstance);
            }
            return false;
        }

        // Case 3: Type variable → check upper bound
        if (thatType instanceof TypeVariable<?> tv) {
            for (Type bound : tv.getBounds()) {
                if (isInstance(thisInstance, bound)) {
                    return true;
                }
            }
            return false;
        }

        // Case 4: Wildcard → check upper bounds
        if (thatType instanceof WildcardType wt) {
            for (Type bound : wt.getUpperBounds()) {
                if (isInstance(thisInstance, bound)) {
                    return true;
                }
            }
            return false;
        }

        // Case 5: Generic array → check component type
        if (thatType instanceof GenericArrayType ga) {
            Type comp = ga.getGenericComponentType();
            if (thisInstance.getClass().isArray()) {
                Class<?> compClass = thisInstance.getClass().getComponentType();
                return canAssign(compClass, getRawClass(comp));
            }
            return false;
        }

        return false;
    }


    public static boolean canAssign(Type fromThis, Class<?> toThat) {
        if (fromThis == null || toThat == null) {
            return false;
        }

        // Case 1: Simple class
        if (fromThis instanceof Class<?> cls) {
            return toThat.isAssignableFrom(cls);
        }

        // Case 2: Parameterized type → check raw type
        if (fromThis instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass) {
                return toThat.isAssignableFrom(rawClass);
            }
            return false;
        }

        // Case 3: Type variable → check upper bounds
        if (fromThis instanceof TypeVariable<?> tv) {
            for (Type bound : tv.getBounds()) {
                if (canAssign(bound, toThat)) {
                    return true;
                }
            }
            return false;
        }

        // Case 4: Wildcard → check upper bounds
        if (fromThis instanceof WildcardType wt) {
            for (Type bound : wt.getUpperBounds()) {
                if (canAssign(bound, toThat)) {
                    return true;
                }
            }
            return false;
        }

        // Case 5: Generic array → check component type
        if (fromThis instanceof GenericArrayType ga) {
            Type comp = ga.getGenericComponentType();
            Class<?> rawComp = getRawClass(comp);
            Class<?> arrayClass = java.lang.reflect.Array.newInstance(rawComp, 0).getClass();
            return toThat.isAssignableFrom(arrayClass);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static <C> C getRawClass(Type jvmType) {
        if (jvmType instanceof Class<?> cls) {
            return (C)cls;
        }
        if (jvmType instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass) {
                return (C)rawClass;
            }
        }
        if (jvmType instanceof TypeVariable<?> tv) {
            Type[] bounds = tv.getBounds();
            if (bounds.length > 0) {
                return getRawClass(bounds[0]);
            }
        }
        if (jvmType instanceof WildcardType wt) {
            Type[] bounds = wt.getUpperBounds();
            if (bounds.length > 0) {
                return getRawClass(bounds[0]);
            }
        }
        if (jvmType instanceof GenericArrayType ga) {
            Class<?> comp = getRawClass(ga.getGenericComponentType());
            return (C) java.lang.reflect.Array.newInstance(comp, 0).getClass();
        }

        throw new SerializerException(GenericDiagnosticCode.ERROR, "Failed to classify type: " + jvmType);
    }

    public static String getTypeName(Type jvmType) {
        Class<?> jvmClass = getRawClass(jvmType);
        return jvmClass.getName();
    }

    @Nullable
    public static String getTestName() {
        Method testMethod = getTestMethod();
        if (testMethod == null) {
            return null;
        }
        return testMethod.getName();
    }

    @Nullable
    public static Method getTestMethod() {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : callStack) {
            String className = frame.getClassName();
            String methodName = frame.getMethodName();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
                List<Method> methods = getMethodsByName(clazz, methodName);
                for (Method method : methods) {
                    if (hasTestAnnotation(method)) {
                        return method;
                    }
                }
            } catch (ClassNotFoundException e) {
                break;
            }
        }
        return null;
    }

    private static boolean hasTestAnnotation(Method method) {
        Annotation[] annotations = method.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            String name = type.getName();
            if (name.startsWith("org.junit.jupiter.api.Test")) {
                return true;
            }
        }
        return false;
    }

    private static List<Method> getMethodsByName(Class<?> clazz, String name) {
        List<Method> methods = new ArrayList<>();
        for (Method method : clazz.getMethods()){
            if(method.getName().equals(name)){
                // System.out.println("Possible match : " + method);
                methods.add(method);
            }
        }
        for (Method method : clazz.getDeclaredMethods()){
            if(method.getName().equals(name)){
                // System.out.println("Possible match : " + method);
                methods.add(method);
            }
        }
        return methods;
    }

    @Nullable
    public static Pair<Class<?>,String> getCaller() {
        return getCaller(false);
    }

    @Nullable
    public static Pair<Class<?>,String> getCaller(boolean ignoreQueryingClass) {
        String thisClass = ReflectionUtils.class.getName();
        String queryingClass = null;
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : callStack) {
            String className = frame.getClassName();
            String methodName = frame.getMethodName();
            if (!className.equals("java.lang.Thread") && !className.equals(thisClass)) {
                if (queryingClass == null) {
                    queryingClass = className;
                } else if (!ignoreQueryingClass || !className.equals(queryingClass)) {
                    try {
                        Class<?> callingClass = Class.forName(className);
                        return new Pair<>(callingClass, methodName);
                    } catch (ClassNotFoundException e) {
                        break;
                    }
                }
            }
        }
        return null;
    }
}

