package cn.lambdalib2.util;

import cn.lambdalib2.registry.AnnotationScanner;
import cn.lambdalib2.registry.AnnotationScanner.Entry;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionUtils {

    private static final ClassLoader LOADER = ReflectionUtils.class.getClassLoader();

    /**
     * Get all the methods for a class, including those that are private or protected in parent class.
     * All the methods are made accessible.
     */
    public static List<Method> getAccessibleMethods(Class cls) {
        List<Method> ret = new ArrayList<>();

        while (cls != null) {
            for (Method m : cls.getDeclaredMethods()) {
                try {
                    m.setAccessible(true);
                } catch (RuntimeException ignored) {
                    continue;
                }
                ret.add(m);
            }
            cls = cls.getSuperclass();
        }

        return ret;
    }

    public static Method getMethod(Class<?> cl, String methodName, Class... parameterTypes) {
        try {
            Method m = cl.getDeclaredMethod(methodName, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getField(Class<?> cl, String name) {
        try {
            Field f = cl.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> forName(String name) {
        switch (name) {
            case "boolean": return boolean.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "short": return short.class;
            case "int": return int.class;
            case "long": return long.class;
            case "float": return float.class;
            case "double": return double.class;
            case "void": return void.class;
        }
        try {
            return Class.forName(name, true, LOADER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> forType(Type t) {
        if (t.getSort() == Type.ARRAY) {
            Class<?> comp = forType(t.getElementType());
            for (int i = 0; i < t.getDimensions(); ++i) {
                comp = java.lang.reflect.Array.newInstance(comp, 0).getClass();
            }
            return comp;
        }
        return forName(t.getClassName());
    }

    public static Method toMethod(Entry data) {
        try {
            Class<?> type = forName(data.className);
            Type[] rawArgs = Type.getArgumentTypes(data.desc);
            Class[] args = new Class[rawArgs.length];
            for (int i = 0; i < rawArgs.length; ++i) {
                args[i] = forType(rawArgs[i]);
            }
            Method method = type.getDeclaredMethod(data.memberName, args);
            method.setAccessible(true);
            return method;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field toField(Entry data) {
        try {
            Field f = forName(data.className).getDeclaredField(data.memberName);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all methods in all classes with given annotation.
     */
    public static List<Method> getMethods(Class<? extends Annotation> annoClass) {
        return getRawObjects(annoClass).stream()
            .filter(e -> e.kind == AnnotationScanner.Kind.METHOD)
            .map(ReflectionUtils::toMethod)
            .collect(Collectors.toList());
    }

    /**
     * Get all classes with given annotation.
     */
    public static List<Class<?>> getClasses(Class<? extends Annotation> annoClass) {
        return getRawObjects(annoClass).stream()
            .filter(e -> e.kind == AnnotationScanner.Kind.CLASS)
            .map(e -> e.className)
            .distinct()
            .map(ReflectionUtils::forName)
            .collect(Collectors.toList());
    }

    /**
     * Get all fields with given annotation.
     */
    public static List<Field> getFields(Class<? extends Annotation> annoClass) {
        return getRawObjects(annoClass).stream()
            .filter(e -> e.kind == AnnotationScanner.Kind.FIELD)
            .map(ReflectionUtils::toField)
            .collect(Collectors.toList());
    }

    public static List<Entry> getRawObjects(Class<? extends Annotation> annoClass) {
        return AnnotationScanner.getAll(annoClass.getName());
    }

}
