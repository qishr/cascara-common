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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Nullable;

public class ReflectionUtils {

    @Nullable
    public static Class<?> getGenericTypeOfListField(Field field) {
        // Check if the field is a List type
        if (List.class.isAssignableFrom(field.getType())) {
            // Get the generic type of the field
            Type genericFieldType = field.getGenericType();
            // Check if it is a ParameterizedType
            if (genericFieldType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericFieldType;
                // Get the actual type arguments
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 0) {
                    // Return the raw class of the first actual type argument
                    return (Class<?>) actualTypeArguments[0];
                }
            }
        }
        // Return null if it's not a List or doesn't have a generic type
        return null;
    }

    public static Class<?> getGenericTypeOfMapKey(Field field) {
        return getGenericType(field, 0);
    }

    public static Class<?> getGenericTypeOfMapValue(Field field) {
        return getGenericType(field, 1);
    }

    private static Class<?> getGenericType(Field field, int index) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            if (actualTypeArguments.length > index) {
                Type typeArg = actualTypeArguments[index];
                if (typeArg instanceof Class<?>) {
                    return (Class<?>) typeArg;
                }
            }
        }

        // TODO: This looks very tied in to serializers...
        return String.class; // Fallback to String if type cannot be determined
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

