package cn.lambdalib2.registry;

import cn.lambdalib2.registry.AnnotationScanner.Entry;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Dispatches {@link StateEventCallback}s. Common callbacks are invoked by the main entrypoint, client-only ones by
 *  the client entrypoint.
 */
public enum RegistryManager {
    INSTANCE;

    private static final class Callback {
        final Entry entry;
        final int priority;
        Method method;

        Callback(Entry entry) {
            this.entry = entry;
            Object p = entry.values.get("priority");
            this.priority = p == null ? 0 : (Integer) p;
        }

        Method method() {
            if (method == null) {
                method = ReflectionUtils.toMethod(entry);
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("StateEventCallback method " + method + " must be static.");
                }
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("StateEventCallback method " + method + " requires exactly 1 argument.");
                }
            }
            return method;
        }

        String eventTypeName() {
            return org.objectweb.asm.Type.getArgumentTypes(entry.desc)[0].getClassName();
        }
    }

    private List<Callback> common, client;

    private void checkInit() {
        if (common != null) return;
        common = new ArrayList<>();
        client = new ArrayList<>();
        for (Entry e : ReflectionUtils.getRawObjects(StateEventCallback.class)) {
            Callback cb = new Callback(e);
            if (AnnotationScanner.isClientOnlyEntry(e)) {
                client.add(cb);
            } else {
                common.add(cb);
            }
        }
        Comparator<Callback> cmp = (a, b) -> {
            if (a.priority != b.priority) return b.priority - a.priority;
            return a.entry.toString().compareTo(b.entry.toString());
        };
        common.sort(cmp);
        client.sort(cmp);
    }

    public void fireCommon(StateEvents.StateEvent event) {
        checkInit();
        fire(common, event);
    }

    private boolean registriesReadyFired = false;

    /**
     * Fires {@link StateEvents.RegistriesReady} if it hasn't been fired yet.
     */
    public synchronized void fireRegistriesReady() {
        if (registriesReadyFired) return;
        registriesReadyFired = true;
        fireCommon(new StateEvents.RegistriesReady());
    }

    public void fireClient(StateEvents.StateEvent event) {
        checkInit();
        fire(client, event);
    }

    private void fire(List<Callback> list, StateEvents.StateEvent event) {
        String typeName = event.getClass().getName();
        for (Callback cb : list) {
            if (!cb.eventTypeName().equals(typeName)) continue;
            Method m = cb.method();
            try {
                m.invoke(null, event);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException("Error when calling StateEventCallback " + m, ex.getCause());
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
